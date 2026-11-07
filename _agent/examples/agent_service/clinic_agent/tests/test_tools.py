# -*- coding: utf-8 -*-
"""tools.py 单测：正常/解包/401/业务错/循环防护/归属过滤/重试策略。"""
import asyncio

import httpx
import pytest
from agentscope.message import ToolResultState

from clinic_agent import tools as T
from clinic_agent.tools import build_tools


class FakeRegistry:
    async def get(self, uid):
        return "jwt-token"


def ok(data):
    return {"code": 200, "data": data}


def make_tools(handler):
    T._client = httpx.AsyncClient(
        transport=httpx.MockTransport(handler), base_url="http://x/api"
    )
    T.ClinicHttpTool._call_counts.clear()
    return {t.name: t for t in build_tools(FakeRegistry(), "patient:7")}


def chunk_text(chunk):
    return chunk.content[0].text


def test_list_departments_ok():
    ts = make_tools(lambda req: httpx.Response(200, json=ok([{"id": 1, "name": "口腔科"}])))
    chunk = asyncio.run(ts["list_departments"].call())
    assert chunk.state == ToolResultState.SUCCESS
    assert "口腔科" in chunk_text(chunk)


def test_page_records_unwrapped():
    ts = make_tools(lambda req: httpx.Response(
        200, json=ok({"records": [{"id": 5, "doctorName": "张医生"}], "total": 3})))
    chunk = asyncio.run(ts["list_doctors"].call())
    assert chunk.state == ToolResultState.SUCCESS
    assert "张医生" in chunk_text(chunk)
    assert "records" not in chunk_text(chunk)


def test_401_returns_error_chunk():
    ts = make_tools(lambda req: httpx.Response(401, json={"message": "expired"}))
    chunk = asyncio.run(ts["list_patients"].call())
    assert chunk.state == ToolResultState.ERROR
    assert "登录已过期" in chunk_text(chunk)


def test_business_error_returns_error_chunk():
    ts = make_tools(lambda req: httpx.Response(200, json={"code": 400, "message": "参数错误"}))
    chunk = asyncio.run(ts["list_departments"].call())
    assert chunk.state == ToolResultState.ERROR
    assert "业务失败" in chunk_text(chunk)


def test_loop_guard_blocks_third_repeat():
    calls = {"n": 0}

    def handler(req):
        calls["n"] += 1
        return httpx.Response(200, json=ok([]))

    ts = make_tools(handler)
    call = ts["list_schedules"].call
    assert asyncio.run(call()).state == ToolResultState.SUCCESS
    assert asyncio.run(call()).state == ToolResultState.SUCCESS
    third = asyncio.run(call())
    assert third.state == ToolResultState.ERROR
    assert "重复" in chunk_text(third)
    assert calls["n"] == 2  # 第3次没有真正发出请求


def _route(req):
    """去掉 base_url 的 /api 前缀，得到纯路由路径。"""
    return req.url.path.removeprefix("/api")


def _patient_ctx_handler(extra=None):
    """routing：无挂号→诊所列表兜底→就诊人列表。"""
    extra = extra or {}

    def handler(req):
        path = _route(req)
        if path == "/miniapp/registration/my-list":
            return httpx.Response(200, json=ok({"list": [], "total": 0}))
        if path == "/miniapp/clinics":
            return httpx.Response(200, json=ok([{"id": 2, "clinicName": "口腔诊所"}]))
        if path == "/miniapp/patient/list":
            return httpx.Response(200, json=ok(
                [{"id": 11, "clinicId": 2, "patientName": "张三"},
                 {"id": 12, "clinicId": 2, "patientName": "李四"}]))
        if path in extra:
            return httpx.Response(200, json=ok(extra[path]))
        raise AssertionError(f"unexpected path: {path}")

    return handler


def test_prescription_list_filters_other_patients():
    ts = make_tools(_patient_ctx_handler({
        "/admin/prescription/list": [
            {"id": 1, "patientId": 11, "patientName": "张三"},
            {"id": 2, "patientId": 99, "patientName": "别人"},
        ],
    }))
    chunk = asyncio.run(ts["list_prescriptions"].call())
    assert chunk.state == ToolResultState.SUCCESS
    text = chunk_text(chunk)
    assert "张三" in text
    assert "别人" not in text


def test_prescription_detail_foreign_rejected():
    ts = make_tools(_patient_ctx_handler({
        "/admin/prescription/9": {"id": 9, "patientId": 99, "patientName": "别人"},
    }))
    chunk = asyncio.run(ts["get_prescription"].call(id=9))
    assert chunk.state == ToolResultState.ERROR
    assert "无权查看" in chunk_text(chunk)


def test_cancel_requires_own_registration():
    hits = {"admin_cancel": 0}

    def handler(req):
        path = _route(req)
        if path == "/miniapp/registration/my-list":
            return httpx.Response(200, json=ok({"list": [], "total": 0}))
        if path == "/admin/registration/cancel":
            hits["admin_cancel"] += 1
            return httpx.Response(200, json=ok("取消成功"))
        raise AssertionError(f"unexpected path: {path}")

    ts = make_tools(handler)
    chunk = asyncio.run(ts["cancel_registration"].call(id=5, reason="误约"))
    assert chunk.state == ToolResultState.ERROR
    assert "无权取消" in chunk_text(chunk)
    assert hits["admin_cancel"] == 0  # 预检拦截，未触达写接口


def test_departments_derives_clinic_id():
    seen = {}

    def handler(req):
        path = _route(req)
        seen[path] = dict(req.url.params)
        if path == "/miniapp/registration/my-list":
            return httpx.Response(200, json=ok({"list": [], "total": 0}))
        if path == "/miniapp/clinics":
            return httpx.Response(200, json=ok([{"id": 8, "clinicName": "北海诊所"}]))
        if path == "/miniapp/departments":
            return httpx.Response(200, json=ok([{"id": 3, "name": "口腔科"}]))
        raise AssertionError(f"unexpected path: {path}")

    ts = make_tools(handler)
    chunk = asyncio.run(ts["list_departments"].call())
    assert chunk.state == ToolResultState.SUCCESS
    # 不传 clinicId 时自动推导（my-list 空 → clinics 兜底 id=8）
    assert seen["/miniapp/departments"].get("clinicId") == "8"


def test_write_tool_does_not_retry():
    posts = {"n": 0, "idem": None}

    def handler(req):
        posts["n"] += 1
        posts["idem"] = req.headers.get("X-Client-Request-Id")
        raise httpx.ConnectError("boom")

    ts = make_tools(handler)
    chunk = asyncio.run(ts["create_registration"].call(
        doctorId=1, patientId=11, regDate="2026-09-24", regTime="09:00-09:30"))
    assert chunk.state == ToolResultState.ERROR
    assert "不可达" in chunk_text(chunk)
    assert posts["n"] == 1  # 写操作不重试
    assert posts["idem"] and posts["idem"].startswith("agent:patient:7:")  # 幂等键格式（R3）


def test_read_tool_retries_once_then_succeeds():
    gets = {"n": 0}

    def handler(req):
        gets["n"] += 1
        if gets["n"] == 1:
            raise httpx.ConnectError("boom")
        return httpx.Response(200, json=ok([{"id": 1, "diagnosis": "龋齿"}]))

    # list_medical_records 不做 clinicId 推导，HTTP 次数即工具调用次数
    ts = make_tools(handler)
    chunk = asyncio.run(ts["list_medical_records"].call())
    assert chunk.state == ToolResultState.SUCCESS
    assert gets["n"] == 2
