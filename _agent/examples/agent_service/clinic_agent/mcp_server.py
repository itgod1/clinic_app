# -*- coding: utf-8 -*-
"""诊所工具 MCP server（streamable HTTP，默认 127.0.0.1:8100）。

把 clinic_agent.tools 的只读工具经 MCP 协议暴露给外部消费方
（web UI workspace、开发工具、管理 agent）。一期定位（2026-09-28
批准）：先建 server，不动患者链路——小程序对话链路（8080 代理 →
:8000 clinic_tool_factory 直挂 ToolBase）保持原样，本 server 是平行的
第二个消费面，同一份 SPECS 不重写。

只暴露 10 个只读工具；两个写操作（创建/取消挂号）不进 MCP——写操作
的 HITL 患者确认语义长在 :8000 的 PermissionRule ASK 上，MCP 转发会
绕过它。等消费者侧有等价确认机制再开放。

复用而非重写：每次 MCP 工具调用现造一个 ClinicHttpTool（解包/截断/
重试/循环防护/归属过滤全部继承），仅凭据来源不同——患者链路 JWT 存
Redis token_registry，这里经 contextvar 按请求注入：

    X-Clinic-JWT 请求头（消费方自带，优先）
      > CLINIC_MCP_JWT 环境变量（server 自持，管理视角兜底）

userId 从 JWT claims 解出（patient:{userId}，/miniapp/patient/list 的
显式 userId 参数需要）。不做签名校验：同一个 JWT 就是下游 8080 每次
调用的 Bearer 凭据，篡改 claims 会先在 8080 签名校验处 401，身份与
凭据自洽。凭据绝不打日志。

启动：python -m clinic_agent.mcp_server
挂载：web UI workspace 页新增 MCP
    url = http://127.0.0.1:8100/mcp
    headers = {"X-Clinic-JWT": "<患者 JWT>"}（或起服务前设 CLINIC_MCP_JWT）
"""
import asyncio
import base64
import json
import logging
import os
from contextvars import ContextVar
from inspect import Parameter, Signature
from typing import Any

from mcp.server.fastmcp import FastMCP
from starlette.responses import JSONResponse
from starlette.types import ASGIApp, Receive, Scope, Send

from .tools import SPECS, ClinicHttpTool, ToolSpec, init_client

# agentscope 的日志走 "as" logger；本模块事件少，沿用同一 logger。
logger = logging.getLogger("as")

# 只读工具（SPECS 里 write=False 的全部 10 个）
EXPOSED_SPECS: list[ToolSpec] = [s for s in SPECS if not s.write]

# 请求级凭据上下文：JwtAuthMiddleware 写入，_ContextRegistry/handler 读取
_jwt_ctx: ContextVar[str] = ContextVar("clinic_mcp_jwt", default="")
_user_id_ctx: ContextVar[str] = ContextVar("clinic_mcp_user_id", default="")


def user_id_from_jwt(jwt: str) -> str | None:
    """解 JWT payload 取 userId → "patient:N"（JwtUtil claims：userId/
    username/clinicId/realName）。payload 非法或缺 userId → None。"""
    try:
        seg = jwt.split(".")[1]
        seg += "=" * (-len(seg) % 4)
        claims = json.loads(base64.urlsafe_b64decode(seg))
        uid = claims.get("userId")
        return f"patient:{int(uid)}" if uid is not None else None
    except Exception:
        return None


class _ContextRegistry:
    """token_registry 适配器：get() 返回当前请求 contextvar 里的 JWT
    （患者链路是 Redis registry，这里是请求头直传，二选一装配）。"""

    async def get(self, user_id: str) -> str:
        del user_id  # 身份在 JWT 自身，构造参数仅沿用 ClinicHttpTool 签名
        return _jwt_ctx.get()


def _make_handler(spec: ToolSpec):
    """从 ToolSpec 生成 FastMCP 工具函数：签名驱动 schema（integer→int，
    string→str，required→必填），参数描述进 docstring。"""
    params = []
    for pname, pdef in spec.props.items():
        typ = int if pdef.get("type") == "integer" else str
        if pname in spec.required:
            params.append(
                Parameter(pname, Parameter.POSITIONAL_OR_KEYWORD, annotation=typ),
            )
        else:
            params.append(
                Parameter(pname, Parameter.POSITIONAL_OR_KEYWORD,
                          annotation=typ, default=None),
            )

    async def handler(**kwargs: Any) -> str:
        tool = ClinicHttpTool(spec, _ContextRegistry(), _user_id_ctx.get())
        chunk = await tool.call(**kwargs)
        parts = [getattr(b, "text", "") for b in chunk.content]
        return "\n".join(p for p in parts if p) or "（空结果）"

    legend = "\n".join(
        f"- {name}: {pdef.get('description', pdef.get('type', ''))}"
        for name, pdef in spec.props.items()
    )
    handler.__name__ = spec.name
    handler.__qualname__ = spec.name
    handler.__doc__ = f"{spec.desc}\n\n参数：\n{legend}" if legend else spec.desc
    handler.__signature__ = Signature(params, return_annotation=str)
    return handler


def build_mcp() -> FastMCP:
    # 患者链路经 build_tools() 建 httpx 连接池；这里不走 build_tools，
    # 组装点显式初始化（幂等），否则首次工具调用 _client 为 None。
    init_client()
    mcp = FastMCP(
        "clinic",
        instructions=(
            "诊所业务只读工具：科室/医生/排班查询、挂号记录与排队进度、"
            "药单与病历查询、就诊人查询。写操作（创建/取消挂号）不在 MCP "
            "范围，请引导用户使用小程序。"
        ),
    )
    for spec in EXPOSED_SPECS:
        mcp.tool()(_make_handler(spec))
    return mcp


class JwtAuthMiddleware:
    """纯 ASGI 鉴权中间件（不用 BaseHTTPMiddleware：那会把下游包进
    额外 task，contextvar 传播依赖实现细节；纯 ASGI 在同一协程链展开，
    set 后必然到达 MCP session handler）。lifespan scope 直接透传，
    FastMCP streamable_http_app 自带的 session_manager 生命周期不受影响。
    """

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return
        path = scope.get("path", "")
        if path == "/healthz":
            resp = JSONResponse({
                "ok": True,
                "tools": [s.name for s in EXPOSED_SPECS],
            })
            await resp(scope, receive, send)
            return
        headers = {
            k.decode("latin-1").lower(): v.decode("latin-1")
            for k, v in scope.get("headers", [])
        }
        jwt = (headers.get("x-clinic-jwt", "")
               or os.getenv("CLINIC_MCP_JWT", "")).strip()
        if not jwt:
            resp = JSONResponse(
                {"error": "missing credential: set X-Clinic-JWT header "
                          "or CLINIC_MCP_JWT env"},
                status_code=401,
            )
            await resp(scope, receive, send)
            return
        user_id = user_id_from_jwt(jwt)
        if not user_id:
            resp = JSONResponse(
                {"error": "invalid JWT payload (no userId claim)"},
                status_code=401,
            )
            await resp(scope, receive, send)
            return
        _jwt_ctx.set(jwt)
        _user_id_ctx.set(user_id)
        await self.app(scope, receive, send)


def create_app() -> tuple[FastMCP, ASGIApp]:
    """(mcp 实例, 包好鉴权的 ASGI app)。app 交给 uvicorn 直跑。"""
    mcp = build_mcp()
    return mcp, JwtAuthMiddleware(mcp.streamable_http_app())


def main() -> None:
    logging.basicConfig(level=logging.INFO)
    host = os.getenv("MCP_HOST", "127.0.0.1")
    port = int(os.getenv("MCP_PORT", "8100"))
    import uvicorn

    mcp, app = create_app()
    logger.info(
        "clinic-mcp server starting: %s tools, http://%s:%s/mcp "
        "(credential: X-Clinic-JWT header or CLINIC_MCP_JWT env)",
        len(EXPOSED_SPECS), host, port,
    )
    # 只绑 127.0.0.1（默认）：管理/开发消费方都在本机，MCP 端点不对外。
    # 患者链路 :8000 才是 0.0.0.0（小程序经 8080 内网转发）。
    uvicorn.run(app, host=host, port=port)


if __name__ == "__main__":
    main()
