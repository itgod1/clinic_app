# -*- coding: utf-8 -*-
"""业务工具工厂：经 create_app(extra_agent_tools=...) 对每个 agent 装配时调用。

门控（合并进 :8000 主服务后收紧，防止污染用户个人 agent）：
1. user_id 必须是患者命名空间（``patient:*``）；
2. agent 记录 source=="team"（leader/用户手建的 agent 是 "user"，team 派生的 worker 是 "team"）。
两条都满足才注入 12 个业务工具，否则返回空列表。查不到记录按"不是"处理（fail-closed）。
"""
import json
import logging

import redis.asyncio as redis
from agentscope.tool import ToolBase

from .token_registry import registry
from .tools import build_tools

# agentscope 的日志走 "as" logger（_logging.py，INFO 级自带 handler）；
# 普通模块 logger 的 INFO 会被 root 的 WARNING 默认级吞掉。
logger = logging.getLogger("as")

AGENT_KEY = "agentscope:user:{user_id}:agent:{agent_id}"

_pool: redis.Redis | None = None


def _client() -> redis.Redis:
    """进程级共享连接（与主服务 Redis 同实例，只读 agent 记录）。"""
    global _pool
    if _pool is None:
        _pool = redis.Redis(host="localhost", port=6379, decode_responses=True)
    return _pool


async def _is_business_agent(user_id: str, agent_id: str) -> bool:
    if not user_id.startswith("patient:"):
        return False
    try:
        raw = await _client().get(AGENT_KEY.format(user_id=user_id, agent_id=agent_id))
    except redis.RedisError:
        logger.exception("gate: redis unreadable, deny tools for %s", agent_id)
        return False
    if not raw:
        return False
    try:
        return json.loads(raw).get("source") == "team"
    except (ValueError, AttributeError):
        logger.warning("gate: bad agent record for %s, deny", agent_id)
        return False


async def clinic_tool_factory(
    user_id: str,
    agent_id: str,
    session_id: str,
) -> list[ToolBase]:
    allowed = await _is_business_agent(user_id, agent_id)
    logger.info(
        "clinic tools for user=%s agent=%s session=%s -> %s",
        user_id, agent_id, session_id, "GRANTED" if allowed else "denied",
    )
    if allowed:
        return build_tools(registry, user_id)
    return []
