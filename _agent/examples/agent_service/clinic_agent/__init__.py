# -*- coding: utf-8 -*-
"""诊所小程序 agent 包：挂载到 examples/agent_service 主服务（:8000）。

- router.py     /clinic/session-token（clinic-api 注册患者 JWT）
- factory.py    extra_agent_tools 工厂（门控：仅 patient:* 的 team worker）
- tools.py      12 个业务工具
- prompts.py    总/业务 agent 提示词
- token_registry.py  患者 JWT 注册表（Redis 短 TTL）
"""
