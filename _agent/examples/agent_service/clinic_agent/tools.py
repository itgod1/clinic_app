# -*- coding: utf-8 -*-
"""诊所业务工具：httpx 调 clinic-api，患者 JWT 鉴权。

- 读工具失败重试 1 次；写工具（创建/取消挂号）不自动重试，靠幂等键防重
- 同名+同参连续第 3 次调用直接返回纠正性结果（循环防护）
- 复用的 admin 接口无患者归属过滤，工具层按就诊人集合做确定性过滤
"""
import asyncio
import hashlib
import json
from dataclasses import dataclass, field
from datetime import date, timedelta
from typing import Any

import httpx
from agentscope.message import TextBlock, ToolResultState
from agentscope.permission import PermissionBehavior, PermissionContext, PermissionDecision
from agentscope.tool import ToolBase
from agentscope.tool._response import ToolChunk

API_BASE = "http://localhost:8080/api"
_TIMEOUT = httpx.Timeout(connect=3.0, read=10.0, write=10.0, pool=3.0)

_client: httpx.AsyncClient | None = None


def init_client() -> None:
    """创建进程级共享连接池（幂等）。"""
    global _client
    if _client is None:
        _client = httpx.AsyncClient(base_url=API_BASE, timeout=_TIMEOUT)


@dataclass
class ToolSpec:
    name: str
    desc: str
    method: str
    path: str
    props: dict
    required: list[str] = field(default_factory=list)
    write: bool = False   # 写操作：不自动重试
    max_items: int = 20   # 列表截断


SPECS: list[ToolSpec] = [
    ToolSpec("list_departments", "查询诊所科室列表", "GET", "/miniapp/departments",
             {"clinicId": {"type": "integer", "description": "诊所ID，不传自动取患者所属诊所"}}),
    ToolSpec("list_doctors", "按科室或姓名查在职医生", "GET", "/miniapp/doctors",
             {"clinicId": {"type": "integer", "description": "诊所ID，不传自动取患者所属诊所"},
              "deptId": {"type": "integer", "description": "科室ID（list_departments 的返回值）"},
              "keyword": {"type": "string", "description": "医生姓名关键词"}}),
    ToolSpec("list_schedules", "查某位医生的排班与可约号源（不传日期默认今天起14天）",
             "GET", "/miniapp/schedules",
             {"doctorId": {"type": "integer"},
              "clinicId": {"type": "integer"},
              "startDate": {"type": "string", "description": "YYYY-MM-DD"},
              "endDate": {"type": "string", "description": "YYYY-MM-DD"}},
             ["doctorId"], max_items=14),
    ToolSpec("list_my_registrations", "查当前患者的挂号记录", "GET", "/miniapp/registration/my-list",
             {"status": {"type": "integer",
                         "description": "状态筛选：1已挂号 2已签到 3就诊中 4已完成 5已取消 6已退号 7过号"}}),
    ToolSpec("get_queue_status", "查某次挂号的排队进度（当前叫号/前方人数）",
             "GET", "/miniapp/registration/{id}/queue",
             {"id": {"type": "integer"}}, ["id"]),
    ToolSpec("create_registration",
             "创建挂号（写操作，需用户确认后调用）。regTime 格式必须为 'HH:mm-HH:mm'（如 '09:00-09:30'），"
             "取值须来自 list_schedules 返回的时段。调用前必须已向用户复述：医生/日期时间/就诊人。",
             "POST", "/miniapp/registration/create",
             {"doctorId": {"type": "integer"},
              "patientId": {"type": "integer", "description": "就诊人ID（list_patients 返回）"},
              "regDate": {"type": "string", "description": "YYYY-MM-DD"},
              "regTime": {"type": "string", "description": "如 09:00-09:30"},
              "deptId": {"type": "integer"},
              "clinicId": {"type": "integer", "description": "诊所ID（工具会按就诊人档案自动补齐）"},
              "chiefComplaint": {"type": "string", "description": "主诉，可选"}},
             ["doctorId", "patientId", "regDate", "regTime"], write=True),
    ToolSpec("cancel_registration",
             "取消挂号（写操作，需用户确认后调用；只能取消当前患者自己的挂号）",
             "POST", "/admin/registration/cancel",
             {"id": {"type": "integer"}, "reason": {"type": "string"}}, ["id"], write=True),
    ToolSpec("list_prescriptions", "查当前患者的药单（处方）列表", "GET", "/admin/prescription/list",
             {"paymentStatus": {"type": "integer", "description": "0未支付 1已支付 2已退款"}}),
    ToolSpec("get_prescription", "查药单明细（含药品与用法）", "GET", "/admin/prescription/{id}",
             {"id": {"type": "integer"}}, ["id"]),
    ToolSpec("list_medical_records", "查当前患者的病历/就诊记录列表", "GET", "/miniapp/medical/record/list", {}),
    ToolSpec("get_medical_record", "查病历详情", "GET", "/miniapp/medical/record/{id}",
             {"id": {"type": "integer"}}, ["id"]),
    ToolSpec("list_patients", "查当前账户下的就诊人（挂号时选择用）", "GET", "/miniapp/patient/list", {}),
]


def _unwrap(data: Any) -> Any:
    """Page<T> ({records,total}) → 纯列表。"""
    if isinstance(data, dict) and isinstance(data.get("records"), list):
        return data["records"]
    return data


def _truncate(data: Any, max_items: int) -> Any:
    if isinstance(data, list) and len(data) > max_items:
        return data[:max_items] + [f"（共{len(data)}条，已截断，请让用户缩小范围）"]
    return data


class ClinicHttpTool(ToolBase):
    """通用诊所业务工具：按 ToolSpec 声明调用 clinic-api。"""

    # 框架契约（agent/_agent.py:_batch_tool_calls）：工具须声明并发/只读语义。
    # 同一工具类承载写操作（创建/取消挂号），保守取串行、非只读。
    is_concurrency_safe = False
    is_read_only = False

    _call_counts: dict[tuple, int] = {}  # 类级循环计数（进程内）

    def __init__(self, spec: ToolSpec, registry: Any, user_id: str) -> None:
        self.name = spec.name
        self.description = spec.desc
        self.input_schema = {
            "type": "object",
            "properties": spec.props,
            "required": spec.required,
        }
        self._spec = spec
        self._registry = registry
        self._user_id = user_id
        super().__init__()

    async def check_permissions(
        self,
        tool_input: dict[str, Any],
        context: PermissionContext,
    ) -> PermissionDecision:
        """ToolBase 抽象方法：查询放行；写操作一律 ASK 触发 HITL 患者确认。

        bypass_immune=True 把 ASK 标记为安全级——allow 规则与
        ACCEPT_EDITS 等模式都不能越过患者的显式确认。
        """
        if self._spec.write:
            return PermissionDecision(
                behavior=PermissionBehavior.ASK,
                message=f"{self.name} 是写操作（{self._spec.desc.split('（')[0]}），需患者确认后执行",
                bypass_immune=True,
            )
        return PermissionDecision(
            behavior=PermissionBehavior.ALLOW,
            message="诊所查询工具放行",
        )

    # ---- 结果构造（框架惯例：错误也返回 ERROR 块，不向外抛异常） ----

    def _ok(self, text: str) -> ToolChunk:
        return ToolChunk(content=[TextBlock(text=text)], state=ToolResultState.SUCCESS)

    def _fail(self, text: str) -> ToolChunk:
        return ToolChunk(content=[TextBlock(text=text)], state=ToolResultState.ERROR)

    # ---- 请求 ----

    async def _request(
        self,
        method: str,
        path: str,
        jwt: str,
        params: dict | None = None,
        json_body: dict | None = None,
        extra_headers: dict | None = None,
        retries: int = 1,
    ) -> httpx.Response:
        headers = {"Authorization": f"Bearer {jwt}"}
        if extra_headers:
            headers.update(extra_headers)
        last_exc: Exception | None = None
        for attempt in range(retries + 1):
            try:
                return await _client.request(
                    method, path, params=params, json=json_body, headers=headers,
                )
            except (httpx.TimeoutException, httpx.TransportError) as e:
                last_exc = e
                if attempt < retries:
                    await asyncio.sleep(0.5 * (attempt + 1))
        raise last_exc  # type: ignore[misc]

    async def _request_data(
        self, method: str, path: str, jwt: str,
        params: dict | None = None, json_body: dict | None = None,
        extra_headers: dict | None = None, retries: int = 1,
    ) -> Any:
        """成功返回 data（已解包）；失败返回 ToolChunk(ERROR)。"""
        resp = await self._request(method, path, jwt, params, json_body, extra_headers, retries)
        if resp.status_code == 401:
            return self._fail("登录已过期，请提示用户刷新登录后重试")
        body = resp.json()
        if resp.status_code >= 400 or body.get("code", 200) not in (200, 0):
            return self._fail(f"业务失败：{str(body.get('message') or body)[:200]}")
        return _unwrap(body.get("data", body))

    # ---- 患者上下文（复用 admin 接口所需的归属过滤） ----

    async def _registration_clinic_ids(self, jwt: str) -> list[int]:
        """最近挂号涉及的诊所（去重，最近的在前）。"""
        ids: list[int] = []
        data = await self._request_data(
            "GET", "/miniapp/registration/my-list", jwt,
            params={"page": 1, "pageSize": 5},
        )
        if isinstance(data, dict):
            for r in (data.get("list") or []):
                cid = r.get("clinicId")
                if cid and cid not in ids:
                    ids.append(cid)
        return ids

    async def _my_clinic_ids(self, jwt: str) -> list[int]:
        """患者可达的诊所全集：挂号过的诊所优先，其余按诊所列表顺序补全。"""
        ids = await self._registration_clinic_ids(jwt)
        clinics = await self._request_data("GET", "/miniapp/clinics", jwt)
        if isinstance(clinics, list):
            for c in clinics:
                cid = c.get("id")
                if cid and cid not in ids:
                    ids.append(cid)
        return ids

    async def _get_clinic_id(self, jwt: str) -> int | None:
        """默认诊所：最近挂号 > 就诊人所属 > 诊所列表第一个。"""
        reg_ids = await self._registration_clinic_ids(jwt)
        if reg_ids:
            return reg_ids[0]
        patients, _ = await self._my_patients(jwt)
        if patients and patients[0].get("clinicId"):
            return patients[0]["clinicId"]
        clinics = await self._request_data("GET", "/miniapp/clinics", jwt)
        if isinstance(clinics, list) and clinics:
            return clinics[0].get("id")
        return None

    async def _my_patients(self, jwt: str) -> tuple[list[dict], list[int]]:
        """跨诊所合并就诊人。

        /miniapp/patient/list 要求显式 userId+clinicId，且患者档案
        按诊所分开存（同一人在不同诊所是不同档案），逐诊所查询后
        按 id 去重合并；同时返回用过的诊所列表供调用方复用。
        """
        user_num = int(self._user_id.split(":", 1)[1])
        clinic_ids = await self._my_clinic_ids(jwt)
        merged: list[dict] = []
        seen: set[Any] = set()
        for cid in clinic_ids:
            data = await self._request_data(
                "GET", "/miniapp/patient/list", jwt,
                params={"userId": user_num, "clinicId": cid, "page": 1, "pageSize": 100},
            )
            if not isinstance(data, list):
                continue  # 单诊所查询失败不拖垮整体
            for p in data:
                pid = p.get("id")
                if pid and pid not in seen:
                    seen.add(pid)
                    merged.append(p)
        return merged, clinic_ids

    # ---- 病历专属（miniapp 接口按 clinicId 必填，逐诊所合并） ----

    async def _list_medical_records(self, jwt: str) -> ToolChunk:
        """/miniapp/medical/record/list 必填 clinicId 且按诊所过滤，
        逐诊所查询后合并，返回 {list,total} 分页 dict 需解包。
        归属由接口自身按 userId→就诊人过滤，工具层无需再滤。"""
        patients, clinic_ids = await self._my_patients(jwt)
        if not patients:
            return self._fail("当前账户下没有就诊人档案，无法查询病历")
        mine: list[Any] = []
        for cid in clinic_ids:
            data = await self._request_data(
                "GET", "/miniapp/medical/record/list", jwt,
                params={"clinicId": cid, "pageNum": 1, "pageSize": 50},
            )
            if isinstance(data, dict):  # {list, total} 分页包装
                data = data.get("list")
            if isinstance(data, list):
                mine.extend(data)
        return self._ok(self._serialize(_truncate(mine, self._spec.max_items)))

    # ---- 药单专属（admin 接口，工具层归属过滤） ----

    async def _list_prescriptions(self, jwt: str) -> ToolChunk:
        patients, clinic_ids = await self._my_patients(jwt)
        if not patients:
            return self._fail("当前账户下没有就诊人档案，无法查询药单")
        patient_ids = {p.get("id") for p in patients}
        mine: list[Any] = []
        for cid in clinic_ids:
            data = await self._request_data(
                "GET", "/admin/prescription/list", jwt,
                params={"clinicId": cid, "page": 1, "pageSize": 50},
            )
            if isinstance(data, dict):  # {list, total} 分页包装
                data = data.get("list")
            if isinstance(data, list):
                mine.extend(r for r in data if r.get("patientId") in patient_ids)
        return self._ok(self._serialize(_truncate(mine, self._spec.max_items)))

    async def _get_prescription(self, jwt: str, prescription_id: Any) -> ToolChunk:
        patients, _ = await self._my_patients(jwt)
        patient_ids = {p.get("id") for p in patients}
        data = await self._request_data(
            "GET", f"/admin/prescription/{prescription_id}", jwt,
        )
        if isinstance(data, ToolChunk):
            return data
        if not patients or not isinstance(data, dict) \
                or data.get("patientId") not in patient_ids:
            return self._fail("无权查看该药单")
        return self._ok(self._serialize(data))

    # ---- 序列化 ----

    def _serialize(self, data: Any) -> str:
        text = json.dumps(data, ensure_ascii=False, default=str)
        if len(text) > 6000:
            text = text[:6000] + "…（结果过长已截断）"
        return text

    # ---- 主入口 ----

    async def call(self, **kw: Any) -> ToolChunk:
        # 1) 循环防护：同名+同参第 3 次调用直接返回纠正性结果
        key = (self.name, json.dumps(kw, sort_keys=True, default=str))
        n = ClinicHttpTool._call_counts.get(key, 0) + 1
        if n >= 3:
            ClinicHttpTool._call_counts[key] = 0
            return self._fail(
                "你已连续3次以相同参数调用此工具。请停止重复，基于已获取的信息回答用户，或向用户澄清需求。"
            )
        ClinicHttpTool._call_counts[key] = n

        # 2) 患者 JWT
        jwt = await self._registry.get(self._user_id)
        if not jwt:
            return self._fail("登录已过期，请提示用户刷新登录后重试")

        # 3) 专属分支
        if self.name == "list_patients":
            # patient/list 要求显式 userId+clinicId，专属分支跨诊所合并
            patients, _ = await self._my_patients(jwt)
            if not patients:
                return self._fail(
                    "当前账户下没有就诊人档案。请提示用户先在小程序\"就诊人管理\"里添加就诊人。"
                )
            return self._ok(self._serialize(_truncate(patients, self._spec.max_items)))

        if self.name == "list_prescriptions":
            return await self._list_prescriptions(jwt)
        if self.name == "get_prescription":
            return await self._get_prescription(jwt, kw.get("id"))
        if self.name == "list_medical_records":
            return await self._list_medical_records(jwt)

        # 4) 通用 pre 处理
        if self.name == "list_schedules":
            kw.setdefault("startDate", date.today().isoformat())
            kw.setdefault("endDate", (date.today() + timedelta(days=13)).isoformat())

        if self.name in ("list_departments", "list_doctors") and not kw.get("clinicId"):
            # 不带 clinicId 时 clinic-api 返回空列表，工具层自动推导
            clinic_id = await self._get_clinic_id(jwt)
            if clinic_id:
                kw["clinicId"] = clinic_id

        if self.name == "cancel_registration":
            # 归属预检：只能取消"我的挂号"里出现过的单
            data = await self._request_data(
                "GET", "/miniapp/registration/my-list", jwt,
                params={"page": 1, "pageSize": 100},
            )
            own_ids = {r.get("id") for r in (data.get("list") or [])} \
                if isinstance(data, dict) else set()
            if kw.get("id") not in own_ids:
                return self._fail("该挂号不在当前患者的挂号记录中，无权取消")

        if self.name == "create_registration" and not kw.get("clinicId"):
            # clinic-api 的排班校验按 clinicId 查（缺了报"没有排班"）；
            # 从就诊人档案推导补齐（同一患者在诊所内挂号）
            patients, _ = await self._my_patients(jwt)
            archive = next((p for p in patients if p.get("id") == kw.get("patientId")), None)
            if archive and archive.get("clinicId"):
                kw["clinicId"] = archive["clinicId"]
            else:
                return self._fail("找不到该就诊人的诊所档案，无法创建挂号")

        # 5) 发请求
        path_params = {k: v for k, v in kw.items() if "{" + k + "}" in self._spec.path}
        path = self._spec.path.format(**path_params)
        params = ({k: v for k, v in kw.items()
                   if v is not None and "{" + k + "}" not in self._spec.path}
                  if self._spec.method == "GET" else None)
        json_body = kw if self._spec.method == "POST" else None
        extra_headers = {}
        if self.name == "create_registration":
            digest = hashlib.md5(key[1].encode()).hexdigest()[:32]
            extra_headers["X-Client-Request-Id"] = f"agent:{self._user_id}:{digest}"

        retries = 0 if self._spec.write else 1
        try:
            data = await self._request_data(
                self._spec.method, path, jwt, params, json_body, extra_headers, retries,
            )
        except (httpx.TimeoutException, httpx.TransportError):
            return self._fail("诊所服务暂时不可达，请向用户说明并建议稍后再试")

        if isinstance(data, ToolChunk):
            return data
        if data is None:
            return self._ok("操作成功")
        return self._ok(self._serialize(_truncate(data, self._spec.max_items)))


def build_tools(registry: Any, user_id: str) -> list[ToolBase]:
    init_client()
    return [ClinicHttpTool(s, registry, user_id) for s in SPECS]
