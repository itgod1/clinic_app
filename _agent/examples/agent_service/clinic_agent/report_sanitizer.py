# -*- coding: utf-8 -*-
"""Worker 汇报硬校验（能力黑名单）——提示词是嘱咐，这里是安检。

背景（2026-09-24 实测）：worker 在 TeamSay 汇报里编造"口腔科签到/小程序
签到"等系统里根本不存在的流程（知识库从未喂过，属模型自由发挥）。提示词
软约束只能降频、不能杜绝，故在 worker 的 TeamSay 真正执行前加一道确定性
关卡：按句切分汇报内容，命中 capabilities.json 黑名单词的句子整句替换为
兜底话术，并记 WARNING 日志（原句进日志，便于持续调黑名单）。

挂载点：Agent 的 on_acting 中间件钩子，包住 toolkit.call_tool——权限校验
已过、执行尚未发生，改写 tool_call.input 只影响本次投递，不动会话历史里
的原块。leader 对患者的最终转述是流式文本，无法做非流式关卡——那层靠
prompts.py 接地规则（软）+ 二期事实卡片化。

只挂 clinic worker：patient:* 且 agent 记录 source=="team"（与 factory.py
工具门控同一判据）。leader 的 TeamSay 是派单指令，可能原样引用患者的话
（"患者问怎么签到"），不做替换，避免改写任务本身。
"""
import json
import logging
import re
from pathlib import Path
from typing import Any

from agentscope.middleware import MiddlewareBase

from .factory import _is_business_agent

# agentscope 的日志走 "as" logger（_logging.py，INFO 级自带 handler）；
# 普通模块 logger 的 INFO 会被 root 的 WARNING 默认级吞掉。替换属异常
# 事件，用 WARNING 保证默认配置下也可见。
logger = logging.getLogger("as")

_CAPABILITIES_PATH = Path(__file__).parent / "capabilities.json"

# 句末分隔符：中英文句/问/叹号、分号、换行。worker 汇报是分条要点，
# 换行即句界；兜底保持整句粒度，避免把半句话替换得读不通。
_SENTENCE_SPLIT = re.compile(
    r"[^。！？!?\n；;]*[。！？!?\n；;]|[^。！？!?\n；;]+$",
)

# 模块级缓存：黑名单每次读盘太浪费；:8000 是常驻进程，改配置重启生效
# （capabilities.json _comment 里已写明）。
_capabilities_cache: dict | None = None


def load_capabilities(path: Path = _CAPABILITIES_PATH) -> dict:
    """读黑名单配置，带模块级缓存。坏文件直接抛——安检失配就让它响。"""
    global _capabilities_cache
    if _capabilities_cache is None:
        with open(path, encoding="utf-8") as f:
            cfg = json.load(f)
        if not isinstance(cfg.get("banned_terms"), list) or not isinstance(
            cfg.get("fallback"), str,
        ):
            raise ValueError(f"capabilities 配置缺字段: {path}")
        _capabilities_cache = cfg
    return _capabilities_cache


def sanitize_report(
    text: str,
    *,
    config: dict | None = None,
) -> tuple[str, list[str]]:
    """净化汇报文本。纯函数，无 I/O。

    Returns:
        (净化后文本, 被替换的原句列表)。命中黑名单的句子整句删除，在第一处
        命中位置插入一次兜底话术（多处命中不重复兜底，读起来不像复读机）。
        已知取舍：一句话里真实数据和黑名单词混排（"排队1号，请先签到"）时
        整句陪葬——靠 WORKER_PROMPT 的"数据与建议分条"规则降低概率，替换
        原句已进日志可人工复核。
    """
    cfg = config or load_capabilities()
    banned = cfg["banned_terms"]
    fallback = cfg["fallback"]
    replaced: list[str] = []
    out: list[str] = []
    fallback_used = False
    for m in _SENTENCE_SPLIT.finditer(text):
        seg = m.group(0)
        if seg and any(t in seg for t in banned):
            replaced.append(seg)
            if not fallback_used:
                out.append(fallback)
                fallback_used = True
        else:
            out.append(seg)
    return "".join(out), replaced


class ReportSanitizerMiddleware(MiddlewareBase):
    """on_acting 钩子：拦截本 worker 的 TeamSay，改写 content 后再投递。"""

    def __init__(self, user_id: str, agent_id: str, session_id: str) -> None:
        self.user_id = user_id
        self.agent_id = agent_id
        self.session_id = session_id

    async def on_acting(
        self,
        agent: Any,
        input_kwargs: dict,
        next_handler,
    ):
        tc = input_kwargs.get("tool_call")
        if tc is not None and getattr(tc, "name", "") == "TeamSay":
            try:
                args = json.loads(tc.input)
            except (TypeError, ValueError):
                args = None
            if isinstance(args, dict) and isinstance(
                args.get("content"), str,
            ):
                cleaned, replaced = sanitize_report(args["content"])
                if replaced:
                    logger.warning(
                        "report-sanitizer: TeamSay 汇报 %d 句命中黑名单已替"
                        "换 user=%s agent=%s session=%s 原句=%s",
                        len(replaced), self.user_id, self.agent_id,
                        self.session_id, replaced,
                    )
                    args["content"] = cleaned
                    rewritten = tc.model_copy(
                        update={
                            "input": json.dumps(args, ensure_ascii=False),
                        },
                    )
                    async for item in next_handler(tool_call=rewritten):
                        yield item
                    return
        async for item in next_handler(**input_kwargs):
            yield item


async def clinic_report_guard_factory(
    user_id: str,
    agent_id: str,
    session_id: str,
    workspace,
) -> list[MiddlewareBase]:
    """经 create_app(extra_agent_middlewares=...) 对每个 agent 装配。

    与 clinic_tool_factory 同一判据（patient:* + source=="team"），只给
    clinic worker 挂安检；查不到记录按"不是"处理（fail-closed）。
    """
    del workspace
    if not await _is_business_agent(user_id, agent_id):
        return []
    return [ReportSanitizerMiddleware(user_id, agent_id, session_id)]
