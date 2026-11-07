# -*- coding: utf-8 -*-
"""The example script to start the agent service."""
import os
import sys

import uvicorn
from fastapi.middleware import Middleware
from fastapi.middleware.cors import CORSMiddleware

from agentscope.agent import Agent, InjectionConfig
from agentscope.app import create_app, SubAgentTemplate
from agentscope.app.channel import (
    DingTalkChannel,
    DiscordChannel,
    FeishuChannel,
)
from agentscope.app.hub import ClawSkillHub, GitHubMCPHub
from agentscope.app.message_bus import InMemoryMessageBus
from agentscope.app.rag.knowledge_base_manager import CollectionPerKbManager
from agentscope.app.storage import RedisStorage
from agentscope.app.workspace_manager import LocalWorkspaceManager
from agentscope.mcp import MCPClient, StdioMCPConfig, HttpMCPConfig
from agentscope.middleware import AgenticMemoryMiddleware, MiddlewareBase
from agentscope.permission import (
    PermissionBehavior,
    PermissionContext,
    PermissionMode,
    PermissionRule,
)
from agentscope.rag import ApproxTokenChunker, QdrantStore
from agentscope.workspace import WorkspaceBase

# Clinic miniapp agent capability (see clinic_agent/ package) — mounted
# into this main service so patient conversations are visible in the
# same web UI as personal agents.
from clinic_agent.factory import clinic_tool_factory
from clinic_agent.prompts import WORKER_PROMPT
from clinic_agent.report_sanitizer import clinic_report_guard_factory
from clinic_agent.router import clinic_router

default_mcps = [
    MCPClient(
        name="playwright",
        mcp_config=StdioMCPConfig(
            command="npx",
            args=["@playwright/mcp@latest"],
        ),
        is_stateful=True,
    ),
]

if os.getenv("AMAP_API_KEY"):
    default_mcps.append(
        MCPClient(
            name="amap",
            mcp_config=HttpMCPConfig(
                url=f"https://mcp.amap.com/mcp?key="
                f"{os.environ['AMAP_API_KEY']}",
            ),
            is_stateful=False,
        ),
    )

storage = RedisStorage(
    host="localhost",
    port=6379,
)

vector_store = QdrantStore(location=":memory:")


class ClinicAgent(Agent):
    """本服务所有 agent（诊所助手 leader + clinic-business worker）的运行时
    状态注入统一用北京时间。

    框架默认 InjectionConfig(timezone="UTC")：下午四点问"现在几点"，模型拿到
    <current-time>08:06</current-time><timezone>UTC</timezone> 会如实回答
    "现在是早上8:06"（实测）。凌晨 00:00-08:00 问"明天"还会把日期算错一天。
    服务端建 agent 时不传 injection_config（app 层 AgentData 无此字段），这里
    经 custom_agent_cls 统一补默认值，不改框架源码。
    """

    def __init__(self, *args, **kwargs):
        kwargs.setdefault(
            "injection_config",
            InjectionConfig(timezone="Asia/Shanghai"),
        )
        super().__init__(*args, **kwargs)


async def longterm_memory_factory(
    user_id: str,
    agent_id: str,
    session_id: str,
    workspace: WorkspaceBase,
) -> list[MiddlewareBase]:
    """Attach Markdown-file long-term memory, stored under the session's
    workspace so it is reachable through whichever backend is bound.

    患者侧（patient:*）不挂：记忆靠模型调 Write 工具落盘，DEFAULT
    权限模式下每次都会 ASK 患者确认"写文件"，把内部记忆卡片漏给
    患者且 parked 确认会阻塞会话。一期患者对话不需要长期记忆。
    """
    del agent_id, session_id
    if user_id.startswith("patient:"):
        return []
    return [
        AgenticMemoryMiddleware(
            workdir=workspace.workdir,
            backend=workspace.get_backend(),
        ),
    ]


async def agent_middlewares_factory(
    user_id: str,
    agent_id: str,
    session_id: str,
    workspace: WorkspaceBase,
) -> list[MiddlewareBase]:
    """extra_agent_middlewares 只收一个工厂，这里合并两路装配：
    长期记忆（患者侧不挂）+ worker TeamSay 汇报黑名单安检。"""
    middlewares = await longterm_memory_factory(
        user_id, agent_id, session_id, workspace,
    )
    middlewares += await clinic_report_guard_factory(
        user_id, agent_id, session_id, workspace,
    )
    return middlewares


app = create_app(
    storage=storage,
    custom_agent_cls=ClinicAgent,
    message_bus=InMemoryMessageBus(),
    # -- To use a Redis-backed message bus instead (recommended for
    # -- multi-process / production deployments), uncomment the lines
    # -- below and replace the InMemoryMessageBus() above:
    #
    # from agentscope.app.message_bus import RedisMessageBus
    # message_bus=RedisMessageBus(
    #     host="localhost",
    #     port=6379,
    # ),
    workspace_manager=LocalWorkspaceManager(
        basedir=os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "workspaces",
        ),
        # The default MCP servers that will be added into the workspace
        default_mcps=default_mcps,
        # Clinic patients (user_id "patient:*") get no default MCPs —
        # playwright/amap are personal-assistant tooling, useless for
        # clinic business agents and costly (subprocess per workspace).
        default_mcps_filter=(
            lambda user_id: [] if user_id.startswith("patient:") else default_mcps
        ),
    ),
    # Knowledge base feature — backed by an in-memory Qdrant store. The
    # CollectionPerKbManager allocates one collection per knowledge base,
    # so any embedding dimension is allowed.
    knowledge_base_manager=CollectionPerKbManager(
        storage=storage,
        vector_store=vector_store,
    ),
    # Chunker classes users can pick from when creating a knowledge base;
    # the chosen type and parameters are pinned on the knowledge base.
    knowledge_chunkers=[ApproxTokenChunker],
    # Resource hubs the UI browses under /hub. Neither needs credentials
    # of its own — an individual MCP card declares whatever key it wants
    # from the user in its ``inputs_schema``. Passing a ClawHub token
    # only raises the rate limit.
    mcp_hubs=[GitHubMCPHub()],
    skill_hubs=[ClawSkillHub(api_token=os.getenv("CLAWHUB_API_TOKEN"))],
    # Customize your own subagent templates
    custom_subagent_templates=[
        SubAgentTemplate(
            type="explorer",
            description=(
                "Read-only agents specialized in exploration tasks. It can "
                "read files but cannot modify, create, or delete them. Use "
                "this agent type when you need to investigate the codebase, "
                "understand its structure, or gather information from files "
                "to support planning—without making any changes."
            ),
            system_prompt_template="""You are {member_name}, an explorer \
agent in team '{team_name}' led by {leader_name}.

Team purpose: {team_description}

Your role: {member_description}

## Responsibilities
- Complete the exploration tasks assigned by the team leader.
- You are read-only: you may inspect files and the codebase, but you must \
never modify, create, or delete anything.

## Reporting
- Always report the task result back to {leader_name} using the TeamSay \
tool, whether the task succeeds or fails.
- Keep your private reasoning private; only share conclusions and findings \
that the leader needs.

Note: `TeamSay` is your ONLY channel to communicate with {leader_name} and \
the other team members. Any other output you produce is invisible to them, \
so anything you want them to see MUST be sent through `TeamSay`.""",
            permission_context=PermissionContext(
                # Read-only
                mode=PermissionMode.EXPLORE,
            ),
        ),
        # Clinic miniapp business worker — spawned by the patient-facing
        # leader agent ("诊所助手"). Business tools are injected by
        # clinic_tool_factory only for patient:* team workers.
        SubAgentTemplate(
            type="clinic-business",
            description=(
                "诊所业务专员：预约挂号、排班查询、药单与病历查询。"
                "涉及诊所业务操作的任务都交给它。"
            ),
            system_prompt_template=WORKER_PROMPT,
            permission_context=PermissionContext(
                # 业务 worker 只该用诊所工具 + TeamSay 汇报：
                # 显式拒绝一切内建文件/命令类工具，写操作由
                # 诊所工具自身的 check_permissions 返回 ASK 走 HITL。
                deny_rules={
                    name: [PermissionRule(
                        tool_name=name,
                        rule_content=None,
                        behavior=PermissionBehavior.DENY,
                        source="clinic-business-template",
                    )]
                    for name in (
                        "AskUser", "Bash", "PowerShell",
                        "Read", "Write", "Edit",
                        "Glob", "Grep", "ResetTools", "SkillViewer",
                    )
                },
            ),
        ),
    ],
    # Clinic business tools (12) — gated to patient:* team workers
    extra_agent_tools=clinic_tool_factory,
    # Long-term memory (patient:* gets none) + TeamSay 汇报黑名单安检
    # (clinic workers only — 硬校验，见 clinic_agent/report_sanitizer.py)
    extra_agent_middlewares=agent_middlewares_factory,
    extra_middlewares=[
        Middleware(
            CORSMiddleware,
            allow_origins=["*"],
            allow_methods=["*"],
            allow_headers=["*"],
        ),
    ],
    channels=[
        DingTalkChannel,
        DiscordChannel,
        FeishuChannel,
    ],
)

# Clinic miniapp endpoints (patient JWT registry, gated by X-Registry-Key)
app.include_router(clinic_router)


if __name__ == "__main__":
    # Start the service
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        # Hot reload forces a SelectorEventLoop on Windows, which cannot
        # spawn the subprocesses that the builtin tools rely on
        reload=sys.platform != "win32",
    )
