# -*- coding: utf-8 -*-
"""HITL 确认流冒烟（真实 glm-5.2 + clinic-api）：

患者发写操作请求 → worker 调写工具 → 权限引擎 ASK → leader 流出现
REQUIRE_USER_CONFIRM → 脚本回 USER_CONFIRM_RESULT → 工具按确认执行。

默认走【取消预约 + 拒绝】分支：拒绝不改变任何状态，预约一直可被
再次取消，脚本因此可无限次重跑；断言 TOOL_RESULT 终态为 denied。
允许分支（挂号，expect=success）需环境里存在可约排班，重复跑会
命中幂等键（duplicated=true），也算 PASS。

前置（dev 环境）：
- clinic-api :8080 可达，CLINIC_JWT（环境变量或 argv[3]）是该用户的
  有效小程序 JWT；默认分支要求该用户名下有一笔可取消的挂号。

用法：
  python -m clinic_agent.smoke_hitl [user_id] [话术] [jwt] [allow|deny]
  # jwt 缺省读环境变量 CLINIC_JWT；第 4 参缺省 deny
"""
import asyncio
import json
import os
import sys

import httpx
import redis.asyncio as aioredis

from .bootstrap import BASE, ensure_patient_agent
from .smoke_test import REGISTRY_KEY, _team_worker_agents

WRITE_TOOLS = {"create_registration", "cancel_registration"}


def _find_confirm_nodes(blob: str) -> list[dict]:
    """从 SSE 帧里递归挖 REQUIRE_USER_CONFIRM 节点（可能是投影包装）。"""
    found: list[dict] = []

    def walk(node: object) -> None:
        if isinstance(node, dict):
            if node.get("type") == "REQUIRE_USER_CONFIRM" and "tool_calls" in node:
                found.append(node)
            for v in node.values():
                walk(v)
        elif isinstance(node, list):
            for v in node:
                walk(v)

    try:
        walk(json.loads(blob))
    except ValueError:
        pass
    return found


async def run(user_id: str, question: str, jwt: str, allow: bool) -> int:
    agent_id, session_id = await ensure_patient_agent(user_id)
    expect_state = "success" if allow else "denied"
    print(f"[hitl] user={user_id} agent={agent_id} session={session_id} "
          f"branch={'allow' if allow else 'deny'}")

    headers = {"X-User-ID": user_id}
    timeout = httpx.Timeout(connect=5, read=130, write=10, pool=5)
    rds = aioredis.Redis(host="localhost", port=6379, decode_responses=True)

    pending: dict[str, dict] = {}   # tool_call_id -> confirm 节点
    results: dict[str, str] = {}    # tool_call_id -> tool_result state
    confirm_seen = asyncio.Event()
    result_seen = asyncio.Event()

    def on_event(blob: str, evt_type: str) -> None:
        if evt_type == "REQUIRE_USER_CONFIRM":
            for node in _find_confirm_nodes(blob):
                for tc in node.get("tool_calls", []):
                    if tc.get("name") in WRITE_TOOLS and tc.get("id"):
                        pending[tc["id"]] = node
                        print(f"[hitl] REQUIRE_USER_CONFIRM: {tc['name']} "
                              f"{tc.get('input', '')[:80]}")
                        confirm_seen.set()
        if evt_type == "TOOL_RESULT_END":
            try:
                evt = json.loads(blob)
            except ValueError:
                return
            tc_id = evt.get("tool_call_id") or evt.get("id")
            state = evt.get("state", "")
            if tc_id in pending:
                results[tc_id] = state
                print(f"[hitl] tool_result({tc_id[:10]}): {state}")
                if state == expect_state:
                    result_seen.set()

    async def consume_stream(c: httpx.AsyncClient, sid: str, aid: str) -> None:
        try:
            async with c.stream(
                "GET", f"/sessions/{sid}/stream",
                params={"agent_id": aid},
            ) as resp:
                if resp.status_code != 200:
                    return
                print(f"[hitl] stream open: {sid[:8]} (agent {aid[:8]})")
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
        subscribed: set[str] = set()
        while not result_seen.is_set():
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

    async with httpx.AsyncClient(base_url=BASE, headers=headers, timeout=timeout) as c:
        r = await c.post(
            "/clinic/session-token",
            headers={"X-Registry-Key": REGISTRY_KEY},
            json={"user_id": user_id, "jwt": jwt},
        )
        print(f"[hitl] session-token -> {r.status_code}")
        if r.status_code >= 400:
            return 2

        leader_task = asyncio.create_task(consume_stream(c, session_id, agent_id))
        watcher = asyncio.create_task(watch_workers(c))
        await asyncio.sleep(1)

        r = await c.post(
            "/chat/",
            json={
                "agent_id": agent_id,
                "session_id": session_id,
                "input": {
                    "name": "patient", "role": "user",
                    "content": [{"type": "text", "text": question}],
                },
            },
        )
        print(f"[hitl] chat trigger -> {r.status_code}")
        if r.status_code >= 400:
            return 2

        try:
            await asyncio.wait_for(confirm_seen.wait(), timeout=150)
        except asyncio.TimeoutError:
            print("[hitl] FAIL: 150s 未出现 REQUIRE_USER_CONFIRM")
            return 1

        # 回确认（对第一个写工具； polarity 由分支决定）
        await asyncio.sleep(1)
        tc_id, node = next(iter(pending.items()))
        tc = next(t for t in node["tool_calls"] if t.get("id") == tc_id)
        r = await c.post(
            "/chat/",
            json={
                "agent_id": agent_id,
                "session_id": session_id,
                "input": {
                    "type": "USER_CONFIRM_RESULT",
                    "reply_id": node["reply_id"],
                    "confirm_results": [
                        {"confirmed": allow, "tool_call": tc, "rules": None},
                    ],
                },
            },
        )
        print(f"[hitl] confirm({'allow' if allow else 'deny'} {tc['name']}) "
              f"-> {r.status_code}")
        if r.status_code >= 400:
            return 2

        try:
            await asyncio.wait_for(result_seen.wait(), timeout=150)
        except asyncio.TimeoutError:
            print(f"[hitl] FAIL: 确认后 150s 未见 {expect_state} 结果，"
                  f"states={results}")
            return 1
        leader_task.cancel()
        watcher.cancel()
    await rds.aclose()

    print(f"[hitl] PASS — 写工具经患者"
          f"{'允许' if allow else '拒绝'}后到达终态 {expect_state} "
          f"(results={results or '未捕获'})")
    return 0


if __name__ == "__main__":
    uid = sys.argv[1] if len(sys.argv) > 1 else "patient:35"
    q = sys.argv[2] if len(sys.argv) > 2 else "帮我取消张三明天上午王建国医生的预约"
    j = sys.argv[3] if len(sys.argv) > 3 else os.environ.get("CLINIC_JWT", "")
    branch = (sys.argv[4] if len(sys.argv) > 4 else "deny").lower()
    if not j:
        print("需要患者 JWT：argv[3] 或环境变量 CLINIC_JWT")
        sys.exit(2)
    sys.exit(asyncio.run(run(uid, q, j, branch == "allow")))
