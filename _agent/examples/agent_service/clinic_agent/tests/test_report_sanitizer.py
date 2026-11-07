# -*- coding: utf-8 -*-
"""report_sanitizer 单测：黑名单整句替换、干净文本零改动、多处命中只兜底
一次、middleware 只改写 TeamSay 的 content 且保留 id/name/to、其余工具
原样透传、工厂门控复用 factory 的 source=="team" 判据。"""
import asyncio
import json

from agentscope.message import ToolCallBlock

from clinic_agent import report_sanitizer as R


def drain(async_gen):
    """同步驱动 async generator（项目无 pytest-asyncio，沿用 asyncio.run）。"""

    async def run():
        async for _ in async_gen:
            pass

    asyncio.run(run())


def make_handler():
    """next_handler 桩：记录收到的 tool_call，不产出事件。"""
    seen = []

    async def handler(**kwargs):
        seen.append(kwargs.get("tool_call"))
        return
        yield  # noqa: unreachable —— 让它成为 async generator 函数

    return seen, handler


# ---------------------------------------------------------------- 纯函数

def test_clean_report_untouched():
    text = "已取消挂号，单号 REG123。如需再约告诉我。"
    cleaned, replaced = R.sanitize_report(text)
    assert cleaned == text
    assert replaced == []


def test_banned_sentence_replaced_and_data_kept():
    fallback = R.load_capabilities()["fallback"]
    text = "已为你约到明天上午王明医生，排队1号。到院后请先在口腔科前台签到。"
    cleaned, replaced = R.sanitize_report(text)
    assert len(replaced) == 1 and "签到" in replaced[0]
    assert "排队1号" in cleaned
    assert "签到" not in cleaned
    assert fallback in cleaned
    # 兜底插在第一处命中的位置，数据句保持在前
    assert cleaned.index("排队1号") < cleaned.index(fallback)


def test_multiple_hits_single_fallback():
    fallback = R.load_capabilities()["fallback"]
    text = "先去小程序签到。记得报销要带发票。预约成功。"
    cleaned, replaced = R.sanitize_report(text)
    assert len(replaced) == 2
    assert cleaned.count(fallback) == 1
    assert "预约成功" in cleaned


def test_newline_and_bullets_are_sentence_boundaries():
    fallback = R.load_capabilities()["fallback"]
    text = "预约成功\n- 排队1号\n- 到院请打卡\n- 医生：王明"
    cleaned, replaced = R.sanitize_report(text)
    assert len(replaced) == 1 and "打卡" in replaced[0]
    assert "排队1号" in cleaned and "王明" in cleaned
    assert "打卡" not in cleaned and fallback in cleaned


def test_mixed_data_sentence_is_whole_sentence_tradeoff():
    """已知取舍：数据与黑名单词同句时整句陪葬（原句进日志可复核）。"""
    text = "排队1号，请先到自助机签到。"
    cleaned, replaced = R.sanitize_report(text)
    assert len(replaced) == 1
    assert "排队1号" not in cleaned


def test_every_banned_term_triggers():
    for term in R.load_capabilities()["banned_terms"]:
        cleaned, replaced = R.sanitize_report(f"温馨提示：涉及{term}。")
        assert replaced, f"黑名单词未生效: {term}"
        assert term not in cleaned


# ---------------------------------------------------------------- middleware

def test_middleware_rewrites_teamsay_content():
    mw = R.ReportSanitizerMiddleware("patient:35", "ag1", "ss1")
    tc = ToolCallBlock(
        id="t1",
        name="TeamSay",
        input=json.dumps(
            {
                "content": "挂号成功，排队1号。到院后请在口腔科前台签到。",
                "to": "诊所助手",
            },
            ensure_ascii=False,
        ),
    )
    seen, handler = make_handler()
    drain(mw.on_acting(None, {"tool_call": tc}, handler))
    assert len(seen) == 1
    got = json.loads(seen[0].input)
    assert "签到" not in got["content"]
    assert "排队1号" in got["content"]
    # 只改 content：id/name/to 原样，会话簿记不乱
    assert got["to"] == "诊所助手"
    assert seen[0].name == "TeamSay" and seen[0].id == "t1"


def test_middleware_passes_clean_teamsay_through():
    mw = R.ReportSanitizerMiddleware("patient:35", "ag1", "ss1")
    tc = ToolCallBlock(
        id="t2",
        name="TeamSay",
        input=json.dumps(
            {"content": "药单共 3 条，最新一张 2026-09-24。"},
            ensure_ascii=False,
        ),
    )
    seen, handler = make_handler()
    drain(mw.on_acting(None, {"tool_call": tc}, handler))
    assert seen[0] is tc  # 未改写：同一对象直达


def test_middleware_passes_other_tools_through():
    """业务工具（含 HITL 的创建/取消挂号）不经安检，原对象直达。"""
    mw = R.ReportSanitizerMiddleware("patient:35", "ag1", "ss1")
    tc = ToolCallBlock(
        id="t3",
        name="create_registration",
        input=json.dumps({"doctor_id": 2}, ensure_ascii=False),
    )
    seen, handler = make_handler()
    drain(mw.on_acting(None, {"tool_call": tc}, handler))
    assert seen[0] is tc


def test_middleware_tolerates_bad_input_json():
    mw = R.ReportSanitizerMiddleware("patient:35", "ag1", "ss1")
    tc = ToolCallBlock(id="t4", name="TeamSay", input="not-json")
    seen, handler = make_handler()
    drain(mw.on_acting(None, {"tool_call": tc}, handler))
    assert seen[0] is tc  # 解析失败放行，交给框架自身的输入校验


# ---------------------------------------------------------------- 工厂门控

def test_factory_gating(monkeypatch):
    async def fake_worker(u, a):
        return True

    async def fake_other(u, a):
        return False

    monkeypatch.setattr(R, "_is_business_agent", fake_worker)
    mws = asyncio.run(
        R.clinic_report_guard_factory("patient:35", "ag", "ss", None),
    )
    assert len(mws) == 1 and isinstance(
        mws[0], R.ReportSanitizerMiddleware,
    )

    monkeypatch.setattr(R, "_is_business_agent", fake_other)
    assert asyncio.run(
        R.clinic_report_guard_factory("patient:35", "ag", "ss", None),
    ) == []
