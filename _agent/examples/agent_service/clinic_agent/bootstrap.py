# -*- coding: utf-8 -*-
"""按患者 find-or-create：glm 凭据 → 总agent「诊所助手」→ 会话。

用法：python bootstrap.py patient:7
输出：agent_id / session_id（不含任何密钥）。

凭据策略：患者用户在 DenyAll 资源策略下看不到 local-user 的 glm 凭据，
首次引导时从 Redis 服务端原样复制一份给患者用户（api_key 不落日志）。
"""
import asyncio
import json
import sys

import httpx
import redis.asyncio as aioredis

from .prompts import LEADER_PROMPT

BASE = "http://localhost:8000"
LEADER_NAME = "诊所助手"
MODEL = "glm-5.2"
SOURCE_CRED_KEY = (
    "agentscope:user:local-user:credential:eff7dfd4459d450e873c160d33824386"
)


async def _ensure_credential(client: httpx.AsyncClient) -> str:
    r = await client.get("/credential/")
    r.raise_for_status()
    for c in r.json().get("credentials", []):
        if c.get("data", {}).get("type") == "openai_credential":
            return c["id"]  # 已有，直接用
    # 服务端复制 local-user 的 glm 凭据（不经手日志/输出）
    rds = aioredis.Redis(host="localhost", port=6379, decode_responses=True)
    try:
        raw = await rds.get(SOURCE_CRED_KEY)
    finally:
        await rds.aclose()
    if not raw:
        raise RuntimeError("Redis 中找不到 local-user 的 glm 凭据")
    data = json.loads(raw)["data"]  # 含 api_key，勿打印
    r = await client.post("/credential/", json={"data": data})
    r.raise_for_status()
    return r.json()["credential_id"]


async def _ensure_agent(client: httpx.AsyncClient) -> str:
    r = await client.get("/agent/")
    r.raise_for_status()
    for a in r.json().get("agents", []):
        if a.get("data", {}).get("name") == LEADER_NAME:
            return a["id"]
    r = await client.post(
        "/agent/",
        json={"name": LEADER_NAME, "system_prompt": LEADER_PROMPT},
    )
    r.raise_for_status()
    return r.json()["agent_id"]


async def _ensure_session(
    client: httpx.AsyncClient,
    agent_id: str,
    credential_id: str,
) -> str:
    r = await client.get("/sessions/", params={"agent_id": agent_id})
    r.raise_for_status()
    for s in r.json().get("sessions", []):
        sid = (s.get("session") or {}).get("id") or s.get("session_id")
        if sid:
            return sid
    r = await client.post(
        "/sessions/",
        json={
            "agent_id": agent_id,
            "name": "诊所对话",
            "chat_model_config": {
                "type": "openai_credential",
                "credential_id": credential_id,
                "model": MODEL,
                "parameters": {},
            },
        },
    )
    r.raise_for_status()
    return r.json()["session_id"]


async def ensure_patient_agent(user_id: str) -> tuple[str, str]:
    """返回 (agent_id, session_id)，幂等。"""
    headers = {"X-User-ID": user_id}
    async with httpx.AsyncClient(base_url=BASE, headers=headers, timeout=15) as c:
        cred = await _ensure_credential(c)
        agent_id = await _ensure_agent(c)
        session_id = await _ensure_session(c, agent_id, cred)
        return agent_id, session_id


if __name__ == "__main__":
    uid = sys.argv[1] if len(sys.argv) > 1 else "patient:7"
    a, s = asyncio.run(ensure_patient_agent(uid))
    print(f"user={uid}\nagent_id={a}\nsession_id={s}")
