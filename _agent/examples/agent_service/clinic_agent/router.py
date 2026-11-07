# -*- coding: utf-8 -*-
"""诊所专属路由：clinic-api 代理调用的聚合端点（由 main.py 挂载）。

鉴权：X-Registry-Key（CLINIC_AGENT_SECRET，clinic-api 与本服务共享）。
"""
import os

from fastapi import APIRouter, Header, HTTPException
from pydantic import BaseModel

from .bootstrap import ensure_patient_agent
from .token_registry import registry

REGISTRY_KEY = os.environ.get("CLINIC_AGENT_SECRET", "dev-clinic-secret")

clinic_router = APIRouter(prefix="/clinic")


class TokenBody(BaseModel):
    user_id: str
    jwt: str


@clinic_router.post("/session-token")
async def session_token(
    body: TokenBody,
    x_registry_key: str = Header(),
) -> None:
    """clinic-api 在每次患者对话前调用：注册/续期患者 JWT（TTL 10 分钟）。"""
    if x_registry_key != REGISTRY_KEY:
        raise HTTPException(status_code=401, detail="bad registry key")
    await registry.set(body.user_id, body.jwt)


@clinic_router.post("/bootstrap")
async def bootstrap(
    body: TokenBody,
    x_registry_key: str = Header(),
) -> dict:
    """一次调用完成：注册 JWT + find-or-create 患者 leader agent 与会话。

    clinic-api 侧按 userId 缓存 (agent_id, session_id)，缓存缺失时才调这里，
    避免 find-or-create 逻辑在 Java 里重复实现（设计修订：单一事实源）。
    """
    if x_registry_key != REGISTRY_KEY:
        raise HTTPException(status_code=401, detail="bad registry key")
    await registry.set(body.user_id, body.jwt)
    agent_id, session_id = await ensure_patient_agent(body.user_id)
    return {"agent_id": agent_id, "session_id": session_id}
