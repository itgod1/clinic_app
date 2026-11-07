# -*- coding: utf-8 -*-
"""ClinicHttpTool 单测（不依赖网络）：权限判定 + 多诊所合并 + clinicId 注入。

运行：python -m pytest clinic_agent/test_tools.py -v
（examples 目录下无统一测试配置，直接用相对导入跑。）
"""
import asyncio

import pytest

from clinic_agent.tools import SPECS, ClinicHttpTool, ToolSpec, init_client

SPEC_BY_NAME = {s.name: s for s in SPECS}


class FakeRegistry:
    async def get(self, user_id: str) -> str:
        return "jwt-fake"


def make_tool(name: str) -> ClinicHttpTool:
    return ClinicHttpTool(SPEC_BY_NAME[name], FakeRegistry(), "patient:35")


# ---------------------------------------------------------------- 权限判定

@pytest.mark.parametrize("name", [
    "list_departments", "list_doctors", "list_schedules",
    "list_my_registrations", "get_queue_status", "list_prescriptions",
    "get_prescription", "list_medical_records", "get_medical_record",
    "list_patients",
])
def test_read_tools_allow(name):
    d = asyncio.run(make_tool(name).check_permissions({}, None))
    assert d.behavior.value == "allow"


@pytest.mark.parametrize("name", ["create_registration", "cancel_registration"])
def test_write_tools_ask_and_bypass_immune(name):
    d = asyncio.run(make_tool(name).check_permissions({}, None))
    assert d.behavior.value == "ask"
    assert d.bypass_immune is True  # allow 规则也不能越过患者确认


# ---------------------------------------------------------------- 多诊所合并

class StubTool(ClinicHttpTool):
    """按 (method, path, clinicId) 返回预设数据，模拟 clinic-api。"""

    def __init__(self, spec, routes):
        super().__init__(spec, FakeRegistry(), "patient:35")
        self._routes = routes  # {(path, clinic_id): data}

    async def _request_data(self, method, path, jwt, params=None,
                            json_body=None, extra_headers=None, retries=1):
        key = (path, (params or {}).get("clinicId"))
        return self._routes.get(key, [])


def _patient_routes() -> dict:
    clinics = [{"id": 8, "name": "北海诊所"}, {"id": 1, "name": "阳光口腔"}]
    return {
        ("/miniapp/registration/my-list", None): {"list": []},  # 无挂号记录
        ("/miniapp/clinics", None): clinics,
        # 诊所 8 无档案，诊所 1 有张三
        ("/miniapp/patient/list", 8): [],
        ("/miniapp/patient/list", 1): [{"id": 8, "clinicId": 1, "patientName": "张三"}],
    }


def test_my_patients_merges_across_clinics():
    t = StubTool(SPEC_BY_NAME["list_patients"], _patient_routes())
    patients, clinic_ids = asyncio.run(t._my_patients("jwt"))
    assert [p["patientName"] for p in patients] == ["张三"]
    assert clinic_ids == [8, 1]  # 全部诊所，去重保序


def test_default_clinic_falls_back_to_patient_archive():
    """无挂号记录时，默认诊所取第一个就诊人档案的诊所（诊所 1，而非列表头诊所 8）。"""
    t = StubTool(SPEC_BY_NAME["list_doctors"], _patient_routes())
    cid = asyncio.run(t._get_clinic_id("jwt"))
    assert cid == 1


def test_default_clinic_prefers_recent_registration():
    routes = _patient_routes()
    routes["/miniapp/registration/my-list", None] = None  # 覆盖占位
    routes[("/miniapp/registration/my-list", None)] = {"list": [{"clinicId": 3}]}
    t = StubTool(SPEC_BY_NAME["list_doctors"], routes)
    assert asyncio.run(t._get_clinic_id("jwt")) == 3


def test_list_prescriptions_unwraps_page_dict_and_filters():
    """admin 药单接口返回 {list,total} 分页 dict，须解包并按就诊人过滤。"""
    routes = _patient_routes()
    routes[("/admin/prescription/list", 1)] = {
        "total": 2, "list": [{"id": 1, "patientId": 8}, {"id": 2, "patientId": 999}],
    }
    t = StubTool(SPEC_BY_NAME["list_prescriptions"], routes)
    chunk = asyncio.run(t.call())
    import json
    got = json.loads(chunk.content[0].text)
    assert got == [{"id": 1, "patientId": 8}]  # 只留张三的，他人药单被滤掉


def test_list_medical_records_merges_clinics():
    """病历接口必填 clinicId 且返回 {list,total}，逐诊所解包合并。"""
    import json
    routes = _patient_routes()
    routes[("/miniapp/medical/record/list", 1)] = {
        "total": 1, "list": [{"id": 1, "clinicId": 1}],
    }
    routes[("/miniapp/medical/record/list", 8)] = {
        "total": 1, "list": [{"id": 2, "clinicId": 8}],
    }
    t = StubTool(SPEC_BY_NAME["list_medical_records"], routes)
    chunk = asyncio.run(t.call())
    got = json.loads(chunk.content[0].text)
    # clinic_ids 顺序 = 诊所列表顺序 [8,1]，合并结果随之
    assert got == [{"id": 2, "clinicId": 8}, {"id": 1, "clinicId": 1}]


# ---------------------------------------------------------------- clinicId 注入

def test_create_registration_injects_clinic_from_archive():
    init_client()
    routes = _patient_routes()
    captured = {}

    async def fake_create(self, method, path, jwt, params=None,
                          json_body=None, extra_headers=None, retries=1):
        if path == "/miniapp/registration/create":
            captured.update(json_body or {})
            return {"duplicated": False, "registration": {"id": 31}}
        return routes.get((path, (params or {}).get("clinicId")), [])

    StubTool._request_data = fake_create
    t = StubTool(SPEC_BY_NAME["create_registration"], routes)
    chunk = asyncio.run(t.call(
        deptId=1, doctorId=1, patientId=8,
        regDate="2026-09-25", regTime="08:00-12:00",
    ))
    assert chunk.state.value == "success"
    assert captured["clinicId"] == 1  # 从张三档案推导补齐
    del StubTool._request_data  # 还原，避免污染其他用例


def test_create_registration_without_archive_fails_cleanly():
    init_client()
    routes = {("/miniapp/registration/my-list", None): {"list": []},
              ("/miniapp/clinics", None): [{"id": 8}]}
    captured = {}

    async def fake_create(self, method, path, jwt, params=None,
                          json_body=None, extra_headers=None, retries=1):
        if path == "/miniapp/registration/create":
            captured.update(json_body or {})
            return {}
        return routes.get((path, (params or {}).get("clinicId")), [])

    StubTool._request_data = fake_create
    t = StubTool(SPEC_BY_NAME["create_registration"], routes)
    chunk = asyncio.run(t.call(
        deptId=1, doctorId=1, patientId=999,  # 无此档案
        regDate="2026-09-25", regTime="08:00-12:00",
    ))
    assert chunk.state.value == "error"
    assert "诊所档案" in chunk.content[0].text
    assert captured == {}  # 未发出写请求
    del StubTool._request_data
