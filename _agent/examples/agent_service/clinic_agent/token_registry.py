# -*- coding: utf-8 -*-
"""患者 JWT 注册表：短 TTL 存 Redis，工具调用时按 user_id 解析。"""
import redis.asyncio as redis

TTL_SECONDS = 600


class TokenRegistry:
    def __init__(self, host: str = "localhost", port: int = 6379) -> None:
        self._r = redis.Redis(host=host, port=port, decode_responses=True)

    async def set(self, user_id: str, jwt: str) -> None:
        await self._r.set(f"clinic:token:{user_id}", jwt, ex=TTL_SECONDS)

    async def get(self, user_id: str) -> str | None:
        # 读即续期：每次对话前代理层都会重新 set，正常对话中不会过期
        return await self._r.get(f"clinic:token:{user_id}")


# 包级单例：router（写）与 factory（读）共用
registry = TokenRegistry()
