# -*- coding: utf-8 -*-
"""端到端冒烟（真实 glm-5.2）：患者提问 → leader 派生业务专员 → 诊所工具被调。

断言：任一事件流（leader 或运行期发现的 worker）中出现诊所业务工具调用。
worker 会话在 TeamDelete 后即被清理，故必须运行期间发现并订阅（SSE 自带
run 起点重放，晚加入不丢事件）。

用法：python -m clinic_agent.smoke_test [user_id] [问题]
"""
import asyncio
import json
import sys

import httpx
import redis.asyncio as aioredis

from .bootstrap import BASE, ensure_patient_agent

CLINIC_TOOLS = {
    "list_departments", "list_doctors", "list_schedules",
    "list_my_registrations", "get_queue_status", "create_registration",
    "cancel_registration", "list_prescriptions", "get_prescription",
    "list_medical_records", "get_medical_record", "list_patients",
}
REGISTRY_KEY = "dev-clinic-secret"


async def _team_worker_agents(rds: aioredis.Redis, user_id: str) -> list[str]:
    """扫 source=team 的 agent 记录 → [agent_id]（跳过 :sessions 派生键）。

    worker 的当前会话没有可靠的单数键，改由调用方走 HTTP /sessions/ 取。
    """
    out: list[str] = []
    keys = await rds.keys(f"agentscope:user:{user_id}:agent:*")
    for k in keys:
        if k.endswith(":sessions"):
            continue
        try:
            raw = await rds.get(k)
        except aioredis.RedisError:
            continue
        if not raw:
            continue
        try:
            rec = json.loads(raw)
        except ValueError:
            continue
        if rec.get("source") == "team" and rec.get("id"):
            out.append(rec["id"])
    return out


async def run(user_id: str, question: str) -> int:
    agent_id, session_id = await ensure_patient_agent(user_id)
    print(f"[smoke] user={user_id} agent={agent_id} session={session_id}")

    headers = {"X-User-ID": user_id}
    timeout = httpx.Timeout(connect=5, read=130, write=10, pool=5)
    rds = aioredis.Redis(host="localhost", port=6379, decode_responses=True)
    tool_calls: list[str] = []
    final_text: list[str] = []
    done = asyncio.Event()

    def on_event(blob: str, evt_type: str) -> None:
        for name in CLINIC_TOOLS:
            if name in blob and name not in tool_calls:
                tool_calls.append(name)
                print(f"[smoke] tool seen: {name}")
        if evt_type == "TEXT_BLOCK_DELTA":
            try:
                final_text.append(json.loads(blob).get("delta") or "")
            except ValueError:
                pass
        if evt_type == "REPLY_END":
            done.set()

    async def consume_stream(c: httpx.AsyncClient, sid: str, aid: str) -> None:
        try:
            async with c.stream(
                "GET",
                f"/sessions/{sid}/stream",
                params={"agent_id": aid},
            ) as resp:
                if resp.status_code != 200:
                    return
                print(f"[smoke] stream open: {sid[:8]} (agent {aid[:8]})")
                async for line in resp.aiter_lines():
                    if not line.startswith("data:"):
                        continue
                    raw = line[5:].strip()
                    if not raw or raw == "[DONE]":
                        continue
                    try:
                        evt = json.loads(raw)
                    except ValueError:
                        continue
                    t = evt.get("type", "") if isinstance(evt, dict) else ""
                    on_event(raw, t)
        except (httpx.HTTPError, asyncio.CancelledError):
            pass

    async def watch_workers(c: httpx.AsyncClient) -> None:
        """运行期轮询新 worker（source=team agent 的会话）并订阅（重放覆盖全 run）。"""
        subscribed: set[str] = set()
        while not done.is_set():
            try:
                aids = await _team_worker_agents(rds, user_id)
            except aioredis.RedisError:
                aids = []
            for aid in aids:
                try:
                    r = await c.get("/sessions/", params={"agent_id": aid})
                except httpx.HTTPError:
                    continue
                if r.status_code != 200:
                    continue
                for s in r.json().get("sessions", []):
                    sid = (s.get("session") or {}).get("id") or s.get("session_id")
                    if sid and sid not in subscribed:
                        subscribed.add(sid)
                        asyncio.create_task(consume_stream(c, sid, aid))
            await asyncio.sleep(1.5)

    try:
        async with httpx.AsyncClient(base_url=BASE, headers=headers, timeout=timeout) as c:
            r = await c.post(
                "/clinic/session-token",
                headers={"X-Registry-Key": REGISTRY_KEY},
                json={"user_id": user_id, "jwt": "smoke-dummy-jwt"},
            )
            print(f"[smoke] session-token -> {r.status_code}")

            leader_task = asyncio.create_task(
                consume_stream(c, session_id, agent_id),
            )
            watcher = asyncio.create_task(watch_workers(c))
            await asyncio.sleep(1)  # 先建立订阅再触发

            r = await c.post(
                "/chat/",
                json={
                    "agent_id": agent_id,
                    "session_id": session_id,
                    "input": {
                        "name": "patient",
                        "role": "user",
                        "content": [{"type": "text", "text": question}],
                    },
                },
            )
            print(f"[smoke] chat trigger -> {r.status_code} {r.text[:80]}")
            if r.status_code >= 400:
                return 2

            try:
                await asyncio.wait_for(done.wait(), timeout=150)
            except asyncio.TimeoutError:
                print("[smoke] WARN: 150s 未见 REPLY_END，按已有事件判定")
            # 等 worker 流的尾部事件/TeamSay 到达
            await asyncio.sleep(3)
            leader_task.cancel()
            watcher.cancel()
    finally:
        await rds.aclose()

    print(f"[smoke] clinic tools called: {tool_calls or 'NONE'}")
    ok = bool(tool_calls)
    print(f"[smoke] {'PASS' if ok else 'FAIL'} — 回复片段: {' '.join(final_text)[:300]}")
    return 0 if ok else 1


if __name__ == "__main__":
    uid = sys.argv[1] if len(sys.argv) > 1 else "patient:7"
    q = sys.argv[2] if len(sys.argv) > 2 else "查一下口腔科明天有没有号"
    sys.exit(asyncio.run(run(uid, q)))
