# -*- coding: utf-8 -*-
"""mcp_server 单测：JWT→userId 解析、只读工具暴露面、动态 schema、
handler 全链路（复用 ClinicHttpTool 逻辑）、鉴权中间件四分支
（缺凭据 401/坏 payload 401/头凭据通过/环境变量兜底）+ healthz 免鉴权。"""
import asyncio
import base64
import json

import httpx
import pytest
from starlette.applications import Starlette
from starlette.responses import JSONResponse
from starlette.routing import Route
from starlette.testclient import TestClient

from clinic_agent import mcp_server as M
from clinic_agent import tools as T


def make_jwt(claims):
    enc = lambda obj: base64.urlsafe_b64encode(  # noqa: E731
        json.dumps(obj).encode()).decode().rstrip("=")
    return f"{enc({'alg': 'HS256'})}.{enc(claims)}.sig"


# ---------------------------------------------------------------- userId 解析

def test_user_id_from_jwt():
    assert M.user_id_from_jwt(make_jwt({"userId": 35})) == "patient:35"


def test_user_id_from_garbage_returns_none():
    assert M.user_id_from_jwt("not-a-jwt") is None
    assert M.user_id_from_jwt("") is None


def test_user_id_missing_claim_returns_none():
    assert M.user_id_from_jwt(make_jwt({"username": "x"})) is None


# ---------------------------------------------------------------- 暴露面与 schema

def test_only_read_tools_exposed():
    names = {s.name for s in M.EXPOSED_SPECS}
    assert "create_registration" not in names
    assert "cancel_registration" not in names
    assert len(names) == 10  # SPECS 12 个 - 2 个写操作


def test_build_mcp_registers_all_with_schema():
    tools = asyncio.run(M.build_mcp().list_tools())
    by_name = {t.name: t for t in tools}
    assert set(by_name) == {s.name for s in M.EXPOSED_SPECS}
    sched = by_name["list_schedules"].inputSchema
    assert sched["required"] == ["doctorId"]
    assert sched["properties"]["doctorId"]["type"] == "integer"
    # 参数描述进 docstring（SPECS 的描述不丢）
    assert "YYYY-MM-DD" in by_name["list_schedules"].description


# ---------------------------------------------------------------- handler 全链路

def run_handler(handler, **kwargs):
    T._client = httpx.AsyncClient(
        transport=httpx.MockTransport(
            lambda req: httpx.Response(200, json={"code": 200, "data": [
                {"id": 1, "name": "口腔科"},
            ]}),
        ),
        base_url="http://x/api",
    )
    T.ClinicHttpTool._call_counts.clear()
    return asyncio.run(handler(**kwargs))


def test_handler_uses_contextvar_credential_and_user():
    """带 clinicId 的 list_departments 单请求即回；_ContextRegistry 从
    contextvar 取 JWT（空则走"登录已过期"分支）。"""
    handler = M._make_handler(T.SPECS[0])  # list_departments
    # 未设凭据 → ClinicHttpTool 拿不到 JWT → 错误文案原样返回
    M._jwt_ctx.set("")
    M._user_id_ctx.set("")
    assert "登录已过期" in run_handler(handler, clinicId=8)

    # 设凭据 → 正常穿透到 mock 的 8080
    M._jwt_ctx.set(make_jwt({"userId": 35}))
    M._user_id_ctx.set("patient:35")
    assert "口腔科" in run_handler(handler, clinicId=8)


# ---------------------------------------------------------------- 鉴权中间件

def make_client():
    async def echo(request):
        return JSONResponse({
            "user": M._user_id_ctx.get(),
            "jwt_ok": bool(M._jwt_ctx.get()),
        })

    inner = Starlette(routes=[Route("/echo", echo)])
    return TestClient(M.JwtAuthMiddleware(inner))


def test_middleware_401_without_credential():
    r = make_client().get("/echo")
    assert r.status_code == 401
    assert "X-Clinic-JWT" in r.json()["error"]


def test_middleware_401_on_bad_payload():
    r = make_client().get(
        "/echo", headers={"X-Clinic-JWT": "garbage.token.here"},
    )
    assert r.status_code == 401


def test_middleware_passes_header_credential():
    r = make_client().get(
        "/echo", headers={"X-Clinic-JWT": make_jwt({"userId": 35})},
    )
    assert r.status_code == 200
    assert r.json() == {"user": "patient:35", "jwt_ok": True}


def test_middleware_env_fallback(monkeypatch):
    monkeypatch.setenv("CLINIC_MCP_JWT", make_jwt({"userId": 7}))
    r = make_client().get("/echo")  # 不带请求头
    assert r.status_code == 200
    assert r.json()["user"] == "patient:7"


def test_healthz_bypasses_auth():
    r = make_client().get("/healthz")
    assert r.status_code == 200
    assert r.json()["ok"] is True
    assert "create_registration" not in r.json()["tools"]
