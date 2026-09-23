# 诊所小程序对话 Agent（一期）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 患者通过小程序与多 agent（总agent+业务子agent）对话完成挂号/排班/药单/病历业务，写操作经确认后执行。

**Architecture:** 小程序 → clinic-api 代理（JWT 鉴权+SSE 透传）→ clinic-agent 服务（AgentScope create_app，:8001）→ 业务工具（httpx+患者JWT 回调 clinic-api）。设计文档为唯一依据。

**Tech Stack:** AgentScope（`E:\workcode\clinic\_agent`，venv `E:\workcode\clinic\_agent\.venv`）、Spring Boot（clinic-api）、uni-app Vue3（clinic-miniapp）、Redis（docker `agentscope-redis`）。

**Spec:** `E:\workcode\clinic\clinic_app\docs\superpowers\specs\2026-09-23-miniapp-agent-design.md`

## Global Constraints

- clinic-api base `http://localhost:8080/api`；小程序业务接口软鉴权模式：`SecurityUtils.getCurrentUserId()` 判空返回 `Result.badRequest("用户未登录")`。
- clinic-agent 端口 **8001**（不占用 :8000）；Redis `localhost:6379`；模型凭据复用 Redis 里已配好的 "glm"（okflow 中转，base_url 带 /v1）。
- 患者身份键：agent 侧 `X-User-ID: patient:{userId}`；JWT 仅存 token 注册表（TTL 600s 滑动），禁止进入对话/日志/事件。
- 写操作（创建/取消挂号）不自动重试；幂等键 `clientRequestId`（agent 发起时前缀 `agent:`）。
- 工具循环防护：同名+同参第 3 次调用直接返回纠正性文本。
- 所有新 Python 代码放 `E:\workcode\clinic\_agent\clinic_agent_service\`；所有 git 提交用各仓库现有风格（clinic_app 中文描述、`_agent` 仓库英文 conventional）。
- Java 控制器遵循现有模式：`@RestController` + `@RequiredArgsConstructor` + `Result<T>` 包装 + 写操作加 `@OperationLog`。

## 文件结构总览

```
clinic_app/clinic-api/src/main/java/com/clinic/
├─ controller/miniapp/RegistrationController.java   # 修改：+create/+cancel
├─ controller/miniapp/PrescriptionController.java   # 新建：患者侧药单
├─ controller/miniapp/AgentProxyController.java     # 新建：agent 代理(SSE)
├─ service/RegistrationService.java                 # 修改：+createForUser/+cancelForUser
├─ service/PrescriptionService.java                 # 修改：+listForUser/+getForUser
├─ service/impl/...Impl.java                        # 对应实现
├─ mapper/PrescriptionMapper.java(+xml)             # 修改：患者归属查询
└─ resources/db/migration/V{N}__reg_client_request_id.sql

_agent/clinic_agent_service/
├─ main.py            # create_app + /clinic/session-token 路由 + 8001
├─ token_registry.py  # JWT 注册表(Redis TTL)
├─ tools.py           # 12 工具 + LoopGuard 中间件
├─ prompts.py         # 总/业务 agent 提示词
├─ factory.py         # clinic_tool_factory(门控)
└─ tests/test_tools.py

clinic_app/clinic-miniapp/src/
├─ api/agent.js       # SSE 客户端
└─ pages/agent/chat.vue
```

---

### Task 1: clinic-api 患者侧挂号写接口（幂等+归属校验）

**Files:**
- Create: `clinic-api/src/main/resources/db/migration/V{N}__reg_client_request_id.sql`（N=现有最大版本+1，执行 `ls db/migration` 确定）
- Modify: `entity/Registration.java`（加字段）、`service/RegistrationService.java`、`service/impl/RegistrationServiceImpl.java`、`controller/miniapp/RegistrationController.java`
- Test: `clinic-api/src/test/java/com/clinic/service/RegistrationIdempotentTest.java`

**Interfaces:**
- Produces: `Result<Map<String,Object>> create(@RequestBody Registration reg, @RequestHeader(value="X-Client-Request-Id", required=false) String clientRequestId)`；`POST /miniapp/registration/create`、`POST /miniapp/registration/cancel`。agent 工具（Task 4）按此调用。

- [ ] **Step 1: 写失败测试**（Mockito 单测，不依赖 Spring 上下文）

```java
package com.clinic.service;

// 测试：同 clientRequestId 重复创建返回同一单，不重复落库
@ExtendWith(MockitoExtension.class)
class RegistrationIdempotentTest {
    @Mock RegistrationMapper mapper;
    @Mock RegistrationService self;
    @InjectMocks RegistrationServiceImpl svc; // 若 Impl 依赖多，按构造器实际参数补 @Mock

    @Test
    void duplicateClientRequestIdReturnsExisting() {
        Registration existing = new Registration();
        existing.setId(99L);
        when(mapper.selectByClientRequestId("agent:abc")).thenReturn(existing);
        Registration reg = new Registration();
        Map<String, Object> out = svc.createForUser(7L, reg, "agent:abc");
        assertEquals(99L, ((Registration) out.get("registration")).getId());
        verify(mapper, never()).insert(any());
    }

    @Test
    void cancelByOtherUserRejected() {
        Registration other = new Registration();
        other.setUserId(8L);
        when(mapper.selectById(5L)).thenReturn(other);
        assertThrows(RuntimeException.class, () -> svc.cancelForUser(7L, 5L, "误约"));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**：`cd clinic-api && mvn test -Dtest=RegistrationIdempotentTest` → FAIL（方法不存在）
- [ ] **Step 3: 实现**。migration：

```sql
ALTER TABLE registration ADD COLUMN client_request_id VARCHAR(64) NULL UNIQUE KEY uk_client_req (client_request_id);
```

`Registration.java` 加 `private String clientRequestId;`（lombok @Data 自动 getter/setter，照现有字段注释风格）。ServiceImpl 加：

```java
@Override
@Transactional
public Map<String, Object> createForUser(Long userId, Registration reg, String clientRequestId) {
    if (clientRequestId != null) {
        Registration exist = registrationMapper.selectByClientRequestId(clientRequestId);
        if (exist != null) return Map.of("registration", exist, "duplicated", true);
    }
    reg.setUserId(userId);            // 归属当前患者
    reg.setClientRequestId(clientRequestId);
    validateRegistration(reg);
    create(reg);
    return Map.of("registration", reg, "duplicated", false);
}

@Override
public void cancelForUser(Long userId, Long regId, String reason) {
    Registration reg = registrationMapper.selectById(regId);
    if (reg == null || !userId.equals(reg.getUserId()))
        throw new RuntimeException("无权操作该挂号");   // 项目现有异常风格：BusinessException 如存在则替换
    cancel(regId, reason);
}
```

Mapper 接口+xml 加 `selectByClientRequestId`。控制器（miniapp RegistrationController 追加，含 @OperationLog）：

```java
@ApiOperation("患者创建挂号(支持幂等)")
@OperationLog(module = "小程序-挂号", type = OperationType.CREATE, desc = "患者/agent创建挂号")
@PostMapping("/registration/create")
public Result<Map<String, Object>> create(@RequestBody Registration reg,
        @RequestHeader(value = "X-Client-Request-Id", required = false) String clientRequestId) {
    Long userId = SecurityUtils.getCurrentUserId();
    if (userId == null) return Result.badRequest("用户未登录");
    return Result.success(registrationService.createForUser(userId, reg, clientRequestId));
}

@ApiOperation("患者取消挂号")
@OperationLog(module = "小程序-挂号", type = OperationType.UPDATE, desc = "患者/agent取消挂号")
@PostMapping("/registration/cancel")
public Result<String> cancel(@RequestBody Map<String, Object> params) {
    Long userId = SecurityUtils.getCurrentUserId();
    if (userId == null) return Result.badRequest("用户未登录");
    registrationService.cancelForUser(userId, Long.valueOf(params.get("id").toString()),
            params.get("reason") != null ? params.get("reason").toString() : null);
    return Result.success("取消成功");
}
```

- [ ] **Step 4: 跑测试通过**：`mvn test -Dtest=RegistrationIdempotentTest` → PASS
- [ ] **Step 5: 启动冒烟**：`mvn spring-boot:run` 后 curl（先 /auth/miniapp/login 拿 token）POST create 两次同 `X-Client-Request-Id`，断言第二次 `duplicated=true`
- [ ] **Step 6: Commit**：`git add -A clinic-api && git commit -m "feat: 患者侧挂号创建/取消接口（幂等键+归属校验）"`

---

### Task 2: clinic-api 患者侧药单接口（归属过滤）

**Files:**
- Create: `controller/miniapp/PrescriptionController.java`
- Modify: `service/PrescriptionService.java`、`service/impl/PrescriptionServiceImpl.java`、`mapper/PrescriptionMapper.java` + `resources/mapper/PrescriptionMapper.xml`
- Test: `clinic-api/src/test/java/com/clinic/service/PrescriptionForUserTest.java`

**Interfaces:**
- Produces: `GET /miniapp/prescriptions?pageNum&pageSize`、`GET /miniapp/prescriptions/{id}`（Result 包装，明细含药品条目）。

- [ ] **Step 1: 失败测试**

```java
@Test
void getForUserOwnershipChecked() {
    Prescription p = new Prescription(); p.setId(3L); p.setPatientId(11L);
    when(mapper.selectById(3L)).thenReturn(p);
    when(patientMapper.selectByUserId(7L)).thenReturn(List.of(patientOf(11L)));
    assertEquals(3L, svc.getForUser(7L, 3L).getId());
    assertThrows(RuntimeException.class, () -> svc.getForUser(7L, 9L)); // p9 属他人
}
```

- [ ] **Step 2: 确认失败**（方法不存在）
- [ ] **Step 3: 实现**。Mapper xml（照现有 list 查询风格）：

```xml
<select id="listForUser" resultType="com.clinic.entity.Prescription">
    SELECT p.* FROM prescription p
    JOIN patient pt ON p.patient_id = pt.id
    WHERE pt.user_id = #{userId}
    ORDER BY p.create_time DESC LIMIT #{offset}, #{pageSize}
</select>
```

Service：`Map<String,Object> listForUser(Long userId, Integer pageNum, Integer pageSize)`（分页照 listByUserId 模式）、`Prescription getForUser(Long userId, Long id)`（先查归属 patient 集合再校验）。控制器照 Task 1 模式（GET 两个端点，userId 判空）。
- [ ] **Step 4: 测试通过**
- [ ] **Step 5: curl 冒烟**（自己的 patient 有药单时返回明细；无则空列表）
- [ ] **Step 6: Commit**：`feat: 患者侧药单查询接口（归属过滤）`

---

### Task 3: clinic-agent 服务骨架 + JWT 注册表

**Files:**
- Create: `_agent/clinic_agent_service/token_registry.py`、`_agent/clinic_agent_service/main.py`

**Interfaces:**
- Produces: `POST /clinic/session-token`（header `X-Registry-Key: {CLINIC_AGENT_SECRET}`，body `{"user_id": "patient:7", "jwt": "..."}` → 204）；`TokenRegistry.set/get(user_id)`；服务 `http://localhost:8001`，`GET /health` 可探活。

- [ ] **Step 1: token_registry.py**

```python
# -*- coding: utf-8 -*-
"""患者 JWT 注册表：短 TTL 存储，工具调用时按 user_id 解析。"""
import redis.asyncio as redis

TTL_SECONDS = 600

class TokenRegistry:
    def __init__(self, host: str = "localhost", port: int = 6379) -> None:
        self._r = redis.Redis(host=host, port=port, decode_responses=True)

    async def set(self, user_id: str, jwt: str) -> None:
        await self._r.set(f"clinic:token:{user_id}", jwt, ex=TTL_SECONDS)

    async def get(self, user_id: str) -> str | None:
        return await self._r.get(f"clinic:token:{user_id}")  # 读即续期由代理层每次对话前 set 完成
```

- [ ] **Step 2: main.py**

```python
# -*- coding: utf-8 -*-
"""诊所小程序 agent 服务：AgentScope create_app + 患者token注册端点。"""
import os
import sys

import uvicorn
from fastapi import APIRouter, Header, HTTPException, Request
from pydantic import BaseModel

from agentscope.app import create_app, SubAgentTemplate
from agentscope.app.message_bus import InMemoryMessageBus
from agentscope.app.storage import RedisStorage
from agentscope.app.workspace_manager import LocalWorkspaceManager

from token_registry import TokenRegistry
from factory import clinic_tool_factory          # Task 5 提供
from prompts import LEADER_PROMPT, WORKER_PROMPT  # Task 5 提供

REGISTRY_KEY = os.environ.get("CLINIC_AGENT_SECRET", "dev-clinic-secret")
registry = TokenRegistry()

app = create_app(
    storage=RedisStorage(host="localhost", port=6379),
    message_bus=InMemoryMessageBus(),
    workspace_manager=LocalWorkspaceManager(
        basedir=os.path.join(os.path.dirname(os.path.abspath(__file__)), "workspaces"),
        default_mcps=[],
    ),
    extra_agent_tools=clinic_tool_factory,
    custom_subagent_templates=[
        SubAgentTemplate(
            type="clinic-business",
            description="诊所业务专员：预约挂号、排班、药单、病历查询与办理。",
            system_prompt_template=WORKER_PROMPT,
        ),
    ],
)

clinic_router = APIRouter(prefix="/clinic")

class TokenBody(BaseModel):
    user_id: str
    jwt: str

@clinic_router.post("/session-token")
async def session_token(body: TokenBody, x_registry_key: str = Header()) -> None:
    if x_registry_key != REGISTRY_KEY:
        raise HTTPException(status_code=401, detail="bad registry key")
    await registry.set(body.user_id, body.jwt)

app.include_router(clinic_router)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=8001,
                reload=sys.platform != "win32")
```

（factory/prompts 先建占位最小版让服务能启动：`clinic_tool_factory = None` 改为返回 `[]` 的 async 函数；WORKER_PROMPT/LEADER_PROMPT 先放一句话，Task 5 替换。）
- [ ] **Step 3: 启动验证**：`cd _agent/clinic_agent_service && ../.venv/Scripts/python.exe main.py` → 日志 `Uvicorn running on http://0.0.0.0:8001`；`curl -X POST localhost:8001/clinic/session-token -H "X-Registry-Key: dev-clinic-secret" -H "Content-Type: application/json" -d '{"user_id":"patient:7","jwt":"t"}'` → 无 401；错误 key → 401
- [ ] **Step 4: redis 验证**：`docker exec agentscope-redis redis-cli TTL clinic:token:patient:7` → 600 左右
- [ ] **Step 5: Commit**（`_agent` 仓库）：`git add clinic_agent_service && git commit -m "feat(clinic): agent service skeleton with patient token registry"`

---

### Task 4: 业务工具层（12 工具 + 循环防护）

**Files:**
- Create: `_agent/clinic_agent_service/tools.py`、`_agent/clinic_agent_service/tests/test_tools.py`

**Interfaces:**
- Consumes: Task 3 `TokenRegistry.get`；clinic-api 端点（Task 1/2 + 现有）
- Produces: `build_tools(token_registry) -> list[ToolBase]`（12 个工具实例）；工具名固定：`list_departments, list_doctors, list_schedules, list_my_registrations, get_queue_status, create_registration, cancel_registration, list_prescriptions, get_prescription, list_medical_records, get_medical_record, list_patients`

- [ ] **Step 1: 失败测试**（pytest，httpx MockTransport）

```python
# tests/test_tools.py
import asyncio, json
import httpx, pytest
from tools import build_tools, ToolError

class FakeRegistry:
    async def get(self, uid): return "jwt-token"

def _tools(client):
    import tools
    tools._CLIENT = client
    return {t.name: t for t in build_tools(FakeRegistry())}

def test_list_departments_ok():
    transport = httpx.MockTransport(lambda r: httpx.Response(200, json={"code":200,"data":[{"id":1,"name":"口腔科"}]}))
    ts = _tools(httpx.AsyncClient(transport=transport, base_url="http://x/api"))
    chunk = asyncio.run(ts["list_departments"].call())
    assert "口腔科" in chunk.content[0].text

def test_401_raises_toolerror():
    transport = httpx.MockTransport(lambda r: httpx.Response(401, json={"message":"expired"}))
    ts = _tools(httpx.AsyncClient(transport=transport, base_url="http://x/api"))
    with pytest.raises(ToolError, match="登录已过期"):
        asyncio.run(ts["list_patients"].call())

def test_loop_guard_blocks_third_repeat():
    transport = httpx.MockTransport(lambda r: httpx.Response(200, json={"code":200,"data":[]}))
    ts = _tools(httpx.AsyncClient(transport=transport, base_url="http://x/api"))
    call = ts["list_schedules"].call
    call(); call()
    out = call(doctor_name="张")
    assert "重复" in out.content[0].text  # 第3次同参被拦截
```

- [ ] **Step 2: 跑失败**：`cd _agent/clinic_agent_service && ../../.venv/Scripts/python.exe -m pytest tests/test_tools.py -v`（先 `../../.venv/Scripts/python.exe -m pip install pytest` 如缺）→ FAIL
- [ ] **Step 3: 实现 tools.py**

```python
# -*- coding: utf-8 -*-
"""诊所业务工具：httpx 调 clinic-api，患者 JWT 鉴权，含循环防护。"""
import asyncio
import json
from dataclasses import dataclass, field
from typing import Any

import httpx
from agentscope.message import TextBlock, ToolResultState
from agentscope.tool import ToolBase
from agentscope.tool._response import ToolChunk

API_BASE = "http://localhost:8080/api"
_TIMEOUT = httpx.Timeout(connect=3.0, read=10.0, write=10.0, pool=3.0)
_CLIENT: httpx.AsyncClient | None = None

class ToolError(Exception):
    pass

@dataclass
class ToolSpec:
    name: str
    desc: str
    method: str
    path: str
    props: dict[str, dict]           # json-schema properties
    required: list[str] = field(default_factory=list)
    write: bool = False              # 写工具不自动重试
    max_items: int = 20              # 列表截断

SPECS: list[ToolSpec] = [
    ToolSpec("list_departments", "查询诊所科室列表", "GET", "/miniapp/departments", {}),
    ToolSpec("list_doctors", "按科室或姓名查医生", "GET", "/miniapp/doctors",
             {"department_id": {"type": "integer", "description": "科室ID"},
              "keyword": {"type": "string", "description": "医生姓名关键词"}}),
    ToolSpec("list_schedules", "查医生排班与可约号源（最多14天）", "GET", "/miniapp/schedules",
             {"doctor_id": {"type": "integer"}, "department_id": {"type": "integer"},
              "start_date": {"type": "string", "description": "YYYY-MM-DD"},
              "end_date": {"type": "string", "description": "YYYY-MM-DD"}}, max_items=14),
    ToolSpec("list_my_registrations", "查当前患者的挂号记录", "GET", "/miniapp/registration/my-list",
             {"status": {"type": "integer", "description": "状态筛选，不传查全部"}}),
    ToolSpec("get_queue_status", "查某次挂号的排队进度", "GET", "/miniapp/registration/{id}/queue",
             {"id": {"type": "integer"}}, ["id"]),
    ToolSpec("create_registration", "创建挂号（写操作，需用户确认后调用）。调用前必须已向用户复述：医生/时间/就诊人",
             "POST", "/miniapp/registration/create",
             {"doctor_id": {"type": "integer"}, "schedule_id": {"type": "integer"},
              "patient_id": {"type": "integer", "description": "就诊人ID"},
              "reg_date": {"type": "string", "description": "YYYY-MM-DD"},
              "time_slot": {"type": "string", "description": "如 09:30"}},
             ["doctor_id", "patient_id", "reg_date"], write=True),
    ToolSpec("cancel_registration", "取消挂号（写操作，需用户确认后调用）", "POST", "/miniapp/registration/cancel",
             {"id": {"type": "integer"}, "reason": {"type": "string"}}, ["id"], write=True),
    ToolSpec("list_prescriptions", "查当前患者的药单（处方）列表", "GET", "/miniapp/prescriptions", {}),
    ToolSpec("get_prescription", "查药单明细（含药品与用法）", "GET", "/miniapp/prescriptions/{id}",
             {"id": {"type": "integer"}}, ["id"]),
    ToolSpec("list_medical_records", "查当前患者的病历/就诊记录列表", "GET", "/miniapp/medical/record/list", {}),
    ToolSpec("get_medical_record", "查病历详情", "GET", "/miniapp/medical/record/{id}",
             {"id": {"type": "integer"}}, ["id"]),
    ToolSpec("list_patients", "查当前账户下的就诊人（挂号时选择用）", "GET", "/miniapp/patient/list", {}),
]

class ClinicHttpTool(ToolBase):
    _call_counts: dict[tuple, int] = {}   # 类级循环计数（进程内）

    def __init__(self, spec: ToolSpec, registry) -> None:
        self.name = spec.name
        self.description = spec.desc
        self.input_schema = {"type": "object", "properties": spec.props,
                             "required": spec.required}
        self._spec = spec
        self._registry = registry
        super().__init__()

    async def call(self, **kw: Any) -> ToolChunk:
        key = (self.name, json.dumps(kw, sort_keys=True, default=str))
        n = ClinicHttpTool._call_counts.get(key, 0) + 1
        ClinicHttpTool._call_counts[key] = n
        if n >= 3:
            ClinicHttpTool._call_counts[key] = 0
            return self._text("你已连续3次以相同参数调用此工具。请停止重复，"
                              "基于已获取的信息回答用户，或向用户澄清需求。")
        jwt = await self._registry.get(_current_user())  # 工厂闭包注入当前user（见factory.py的contextvar）
        headers = {"Authorization": f"Bearer {jwt}"}
        path = self._spec.path.format(**{k: v for k, v in kw.items() if "{" + k + "}" in self._spec.path})
        params = {k: v for k, v in kw.items() if v is not None and "{" + k + "}" not in self._spec.path}
        if self._spec.name == "create_registration":
            headers["X-Client-Request-Id"] = f"agent:{_current_session()}:{key[1][:32]}"
        for attempt in (0, 1) if not self._spec.write else (0,):   # 写不重试
            try:
                resp = await _CLIENT.request(self._spec.method, path,
                                             params=params if self._spec.method == "GET" else None,
                                             json=kw if self._spec.method == "POST" else None,
                                             headers=headers)
                break
            except (httpx.TimeoutException, httpx.TransportError) as e:
                if attempt or self._spec.write:
                    raise ToolError(f"诊所服务暂时不可达（{type(e).__name__}），请向用户说明并建议稍后再试") from e
                await asyncio.sleep(0.5 * (attempt + 1))
        if resp.status_code == 401:
            raise ToolError("登录已过期，请提示用户刷新登录后重试")
        body = resp.json()
        if resp.status_code >= 400 or body.get("code", 200) not in (200, 0):
            raise ToolError(f"业务失败：{str(body)[:200]}")
        data = body.get("data", body)
        if isinstance(data, list) and len(data) > self._spec.max_items:
            data = data[: self._spec.max_items] + [f"……共{len(data)}条，已截断，请让用户缩小范围"]
        return self._text(json.dumps(data, ensure_ascii=False, default=str)[:6000])

    def _text(self, s: str) -> ToolChunk:
        return ToolChunk(content=[TextBlock(type="text", text=s)],
                         state=ToolResultState.SUCCESS)

def build_tools(registry) -> list[ToolBase]:
    return [ClinicHttpTool(s, registry) for s in SPECS]

# 当前请求上下文（factory.py 设置；测试中默认 patient:0）
import contextvars
_current_user = contextvars.ContextVar("clinic_user", default="patient:0")
_current_session = contextvars.ContextVar("clinic_session", default="s0")
```

注意：`_current_user` 在工厂调用时无法拿到"运行时"上下文——工具实例是每 run 构建时闭包绑定 user_id，因此实现改为：`ClinicHttpTool.__init__` 接收 `user_id`，`call` 内直接用 `self._user_id`；`build_tools(registry, user_id)`。测试相应传 `user_id="patient:7"`。（保留 contextvars 版本会被评审否决，按此修正后实现。）

- [ ] **Step 4: 跑测试通过**（含循环防护、401、截断三个断言）
- [ ] **Step 5: Commit**：`feat(clinic): 12 business tools with loop-guard and idempotency header`

---

### Task 5: agent 装配（总+业务子）与工具门控探针

**Files:**
- Create: `_agent/clinic_agent_service/prompts.py`、`_agent/clinic_agent_service/factory.py`、`_agent/clinic_agent_service/bootstrap.py`

**Interfaces:**
- Consumes: Task 3 main.py 的 `registry`；Task 4 `build_tools(registry, user_id)`
- Produces: `LEADER_PROMPT`、`WORKER_PROMPT`；`clinic_tool_factory(user_id, agent_id, session_id) -> list[ToolBase]`（门控：仅业务子 agent 拿业务工具）；`bootstrap.py` 提供按患者 find-or-create 总agent+session 的函数 `ensure_patient_agent(user_id) -> (agent_id, session_id)`（调 :8001 REST）。

- [ ] **Step 1: prompts.py**（完整两段提示词，含纠偏/确认/超纲约束——从设计文档 4.2/4.3 逐条落进文字）

```python
LEADER_PROMPT = """你是诊所智能助手「小诊」，面向患者提供服务。
职责：闲聊寒暄、理解患者意图、把业务任务派发给子agent「业务专员」、把结果清楚转述给患者。
规则：
1. 涉及挂号/排班/药单/病历/就诊人的任何请求，创建 type=clinic-business 的子agent并交付任务；交付语必须包含完整上下文（患者原话+已确认信息）。
2. 子agent回报后，用简体中文友好转述，关键信息（时间/医生/就诊人/排队号）分行列出。
3. 你自己没有业务工具，禁止编造任何业务数据。子agent失败时，引导患者使用小程序对应页面。
4. 患者纠正 = 最高优先级指令：立即通知子agent停止当前方向或重新派发。
5. 一期外业务（支付/会员/咨询医生）如实告知"暂未开放"，不得编造。"""

WORKER_PROMPT = """你是{member_name}，诊所业务专员，属于团队'{team_name}'。
团队目标：{team_description}；你的职责：{member_description}
可用工具：查科室/医生/排班、查与创建取消挂号、查药单/病历、查就诊人。
规则：
1. 动手前先核对患者最新意图；参数拿不准先问（经TeamSay告知leader转问患者）。
2. 创建/取消挂号前，必须已向患者复述：医生、日期时间、就诊人，并获得明确同意。
3. 用户纠正 = 立即放弃当前方向，不得继续旧工具链未完成步骤。
4. 工具失败时向leader如实报告原因，不要编造结果。
5. 完成后用TeamSay汇报结论（结构化要点），汇报即结束，不要等待反问。
6. 查询结果只报患者相关内容，简洁分条。"""
```

（若 SubAgentTemplate 的模板变量不含上述占位符，按 `agentscope/app/_types.py` SubAgentTemplate.system_prompt_template 实际支持的变量调整——探针步骤会打印。）

- [ ] **Step 2: factory.py + 门控探针**。先写探针脚本 `probe_factory.py`：以 patient:test 用户建 leader+session（直接用 :8001 REST，参考 `_agent/src/agentscope/app/_router/_agent.py` 与 `_session.py` 的创建 schema 字段名），发一条"查口腔科排班"，在 factory 里 print 每次调用的 `(user_id, agent_id, session_id)`。观察 leader 与 worker 是否都经过 factory：
  - **若 worker 也经过 factory**：门控 = factory 查 Redis `agentscope:user:{uid}:agent:{agent_id}` 记录的 name，worker 名由 leader 按 LEADER_PROMPT 固定命名「业务专员」（在提示词交付语里写明"将子agent命名为：业务专员"），name 含"业务专员"或 agent 记录 type 为 worker → 返回业务工具；否则返回 []。
  - **若 worker 不经过 factory**（worker 工具须另挂）：改用 `custom_agent_cls` 不动，将业务工具同时经 `extra_agent_tools` 给 leader（leader 提示词第1条改为"直接使用业务工具办理"），并在设计文档追加偏差说明（记录决策）。
- [ ] **Step 3: 按探针结论实装 clinic_tool_factory**：

```python
from tools import build_tools
from token_registry import TokenRegistry

_registry = TokenRegistry()

async def clinic_tool_factory(user_id: str, agent_id: str, session_id: str):
    if _is_business_agent(user_id, agent_id):      # 门控函数：按Step 2结论实现
        return build_tools(_registry, user_id)
    return []

def _is_business_agent(user_id: str, agent_id: str) -> bool:
    ...  # 探针结论的两种实现之一，需通过 Step 4 冒烟验证
```

- [ ] **Step 4: 冒烟脚本**（真实走 glm-5.2）：`python bootstrap.py` 建患者 agent+session → `POST /chat/` 发"查一下口腔科明天有没有号" → 订阅 SSE 断言出现 `ToolCallStart`（工具=list_schedules）且最终 TextBlock 含排班信息或引导语。此脚本保留为 `clinic_agent_service/smoke_test.py` 供回归。
- [ ] **Step 5: Commit**：`feat(clinic): leader+worker assembly with tool gating`

---

### Task 6: HITL 写操作确认流

**Files:**
- Create: `_agent/clinic_agent_service/smoke_hitl.py`（保留作回归脚本）

- [ ] **Step 1: 脚本**：bootstrap 患者 → 发"帮我约明天上午第一位口腔科医生" → SSE 中捕获 `RequireUserConfirmEvent`（含 tool_call 与参数）→ 打印确认卡内容 → `POST /chat/` body `input` 传 `UserConfirmResultEvent`（字段 `reply_id`、`confirm_results`，构造照 `_agent/src/agentscope/event/_event.py` 的 ConfirmResult）→ 断言后续出现 create_registration 的 ToolCall/ToolResult 且最后回复含"预约成功"或失败原因
- [ ] **Step 2: 若确认未被框架自动触发**（写工具未标记 require-confirm）：在 ClinicHttpTool 上按 `agentscope/permission` 模块给写工具实例设置 `PermissionRule`（探针：读 `_agent/src/agentscope/tool/_base.py` 中 permission 相关注解/属性与 `_builtin/_ask_user.py` 的确认样例），在 build_tools 中对 `create_registration`/`cancel_registration` 两实例设置确认规则后重跑
- [ ] **Step 3: 拒绝分支**：同样脚本点"拒绝" → 断言回复为"已取消本次操作"，且 clinic-api 无新增挂号（my-list 数量不变）
- [ ] **Step 4: Commit**：`feat(clinic): HITL confirmation flow verified end-to-end`

---

### Task 7: clinic-api AgentProxyController（SSE 透传/中断/确认/限流/watchdog）

**Files:**
- Create: `controller/miniapp/AgentProxyController.java`、`service/AgentProxyService.java`（或并入 controller，若项目无 service 层习惯则直接 WebClient 于 controller）
- Modify: `resources/application-dev.yml`（+`clinic.agent.base-url: http://localhost:8001`、`clinic.agent.secret: dev-clinic-secret`）
- Test: `clinic-api/src/test/java/com/clinic/controller/AgentProxyRateLimitTest.java`

**Interfaces:**
- Consumes: :8001 的 `POST /clinic/session-token`、`POST /chat/`、`GET /sessions/{sid}/stream`(SSE)、`POST /sessions/{sid}/interrupt?agent_id=`、agent/session 创建 REST（Task 5 bootstrap 同款）
- Produces（小程序用）: `POST /miniapp/agent/chat`（body `{sessionId?, text}`，响应 `text/event-stream`）、`POST /miniapp/agent/interrupt`、`POST /miniapp/agent/confirm`、`GET /miniapp/agent/history`

- [ ] **Step 1: 失败测试**（限流与并发 409 的纯逻辑单测：抽 `AgentGuard` 类）

```java
class AgentProxyRateLimitTest {
    @Test void sixPerMinuteThen429() {
        AgentGuard g = new AgentGuard();
        for (int i = 0; i < 6; i++) assertTrue(g.tryAcquire("patient:7"));
        assertFalse(g.tryAcquire("patient:7"));
    }
    @Test void activeRunBlocksNewChat() {
        AgentGuard g = new AgentGuard();
        g.markActive("patient:7", "sess-1");
        assertTrue(g.hasActive("patient:7"));
    }
}
```

- [ ] **Step 2: 失败确认 → Step 3 实现 AgentGuard（滑动窗口计数 + activeMap + 120s watchdog 的 ScheduledExecutor，超时自动调 interrupt 并结束 SseEmitter）**
- [ ] **Step 4: 实现 AgentProxyController**：WebClient(reactor) 或 java.net.http.HttpClient。chat 流程：`SecurityUtils.getCurrentUserId()` 判空 → `patient:{id}` → `POST :8001/clinic/session-token`（X-Registry-Key）→ find-or-create agent+session（按用户缓存 id 映射，Map<Long,String>，key=userId）→ `POST /chat/`（input=Msg：`{"name":"patient","role":"user","content":[{"type":"text","text":<text>}]}`，header `X-User-ID: patient:{id}`）→ 返回 `SseEmitter`，异步线程读 :8001 SSE 流逐行 `emitter.send(data)`；结束/超时/错误分别 send 帧 `event:error`+complete。interrupt/confirm 转发同款 header。history 调 `GET /sessions/{sid}/messages`（字段照 `_router/_session.py`）过滤返回。
- [ ] **Step 5: 测试通过 + 手工 curl**（浏览器外 `curl -N`）看 SSE 帧流出
- [ ] **Step 6: Commit**：`feat: 小程序agent代理（SSE透传/中断/确认/限流/watchdog）`

---

### Task 8: 小程序聊天页

**Files:**
- Create: `clinic-miniapp/src/api/agent.js`、`clinic-miniapp/src/pages/agent/chat.vue`
- Modify: `clinic-miniapp/src/pages.json`（注册页面+标题）、首页加"智能助手"入口按钮（`pages/index/index.vue` 或现有首页文件，navigateTo）

**Interfaces:**
- Consumes: Task 7 四个端点（JWT 由 `utils/request.js` 同款 storage token 附加）
- Produces: 页面路由 `/pages/agent/chat`

- [ ] **Step 1: api/agent.js**（mp-weixin 分块 SSE：`uni.request` `enableChunked:true` + `onChunkReceived` 拼 buffer 按 `\n\n` 切帧；H5 用 fetch+ReadableStream，`// #ifdef` 条件编译）

```javascript
// 核心（两平台共用解析）
export function parseSseChunk(buf, onEvent) {
  const frames = buf.split('\n\n'); const rest = frames.pop();
  frames.forEach(f => { const m = f.match(/^data: (.*)$/m); if (m) onEvent(JSON.parse(m[1])); });
  return rest;
}
export function chatStream({ sessionId, text }, onEvent, onDone, onFail) { /* #ifdef MP-WEIXIN 分块实现 / #ifdef H5 fetch 实现 */ }
```

- [ ] **Step 2: chat.vue**：状态机 `idle|sending|confirming`；渲染映射：`text_block_delta`→气泡追加、`thinking_block_delta`→"思考中…"动效（不显示内容）、`tool_call_start`→状态行"正在查询：{tool 中文名映射}"、`require_user_confirm`→确认卡片（展示参数里 doctor/reg_date/time_slot/patient 字段 + [确认][取消]）、`reply_end`→完成置 idle、error 帧→重试按钮（重发 text）。停止按钮 sending 时可见 → `POST /miniapp/agent/interrupt`。进入页面 `GET history` 恢复历史。
- [ ] **Step 3: H5 联调**：`pnpm dev:h5` + clinic-api + clinic-agent 全启动，走通：闲聊/查排班/预约确认/拒绝/停止/断网重试六场景（逐条勾选记录）
- [ ] **Step 4: pages.json 注册 + 首页入口**
- [ ] **Step 5: Commit**：`feat: 小程序智能助手聊天页（SSE流式/确认卡片/停止）`

---

### Task 9: 端到端整合与交付物

**Files:**
- Create: `_agent/clinic_agent_service/README.md`（启动/联调/回归说明）、`E:\workcode\clinic\_agent\start-clinic-agent.bat`（照 start-all.bat 模式：起 :8001）

- [ ] **Step 1: start-clinic-agent.bat**（ASCII 提示；`start "clinic-agent :8001" /D "%~dp0clinic_agent_service" cmd /k %~dp0.venv\Scripts\python.exe main.py`；沿用 Redis 端口检测）
- [ ] **Step 2: 真机/开发者工具验证**（mp-weixin：分块 SSE、确认卡、停止）记录通过截图
- [ ] **Step 3: 回归脚本**：`smoke_test.py` + `smoke_hitl.py` 全绿
- [ ] **Step 4: Commit**（两仓库各自）：`docs(clinic): integration guide` / `chore: add clinic-agent start script`
- [ ] **Step 5: 更新 clinic_app README** 一节"智能助手（agent）架构与启动"

---

## Self-Review 记录

- **Spec 覆盖**：架构(§2→T3/T7)、agent结构(§3.1→T5)、工具(§3.1表→T4)、新接口(§3.2→T1/T2)、确认(§3.1→T6)、小程序(§3.3→T8)、超时中断(§4.1→T4/T7)、纠偏(§4.2→T4循环防护+T5提示词)、重试(§4.3→T4)、兜底(§4.4→T5/T7)、并发409+限流(§4.5→T7)、安全(§4.6→T3/T1)、测试(§6→各Task)。无缺口。
- **占位符**：Task 5 门控存在"探针二选一"——这是对框架不确定性的显式决策点（spec R1），两种分支都给了实现路径与验收（Step 4 冒烟），非占位符。Task 4 内嵌了一处自我修正说明（contextvars→构造器绑定），实现者按修正后版本写。
- **类型一致**：`build_tools(registry)`→Task 5 修正为 `build_tools(registry, user_id)`，Task 5 工厂代码已按两参调用；端点路径前后一致。
