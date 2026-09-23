# 诊所小程序对话 Agent — 设计文档（一期）

日期：2026-09-23
状态：待评审
涉及仓库：`clinic_app`（clinic-api / clinic-miniapp）、`_agent`（AgentScope 框架）

---

## 1. 背景与目标

为诊所小程序增加对话式入口：患者用自然语言完成预约挂号、排班查询、药单/病历查询等业务。底层用 AgentScope 多 agent 架构：总 agent 管理、路由到业务子 agent。

**一期目标**：
- 患者在小程序与 agent 多轮对话，完成：查科室/医生/排班、挂号全流程（查号源→确认→创建/取消/查我的挂号/排队号）、查药单、查病历。
- 写操作（创建/取消挂号）必须经患者确认后执行。
- 患者身份严格隔离：agent 只能访问当前患者 JWT 权限范围内的数据。

**一期明确不做**：
- 管理端（clinic-admin）agent —— 二期。
- 支付、会员、就诊人增删改 —— 二期。
- 旧 Dify AI 模块 —— 不迁移不维护，保留代码但新功能全部走新链路。
- 语音、图片输入。

---

## 2. 总体架构

```
┌──────────────┐      ┌────────────────────┐      ┌────────────────────────┐
│ 小程序 uni-app │      │ clinic-api :8080   │      │ clinic-agent :8001     │
│ 新聊天页      │────▶ │ AgentProxyController│────▶ │ AgentScope create_app  │
│ SSE流式渲染   │ ◀SSE │ · JWT校验→患者身份  │      │                        │
│ 确认卡片/停止 │      │ · 每次对话注册JWT   │      │ 总agent(路由/闲聊)      │
└──────────────┘      │ · 新增4个患者接口    │      │   └─▶ 业务子agent       │
                      └─────────┬──────────┘      │      (12个业务工具)     │
                                ▲                 └───────────┬────────────┘
                                └──── 患者JWT透传（工具回调）────┘
```

### 2.1 部署形态

| 组件 | 位置 | 端口 | 说明 |
|---|---|---|---|
| clinic-api | clinic_app（Spring Boot） | 8080 | 现有，新增代理控制器 + 4 患者接口 |
| clinic-agent | `_agent/clinic_agent_service/`（新目录，复用 `_agent/.venv`） | 8001 | 新服务，与通用 agent 服务 :8000 并行互不影响 |
| Redis | Docker `agentscope-redis` | 6379 | 会话/凭据存储，两个 agent 服务共用 |

### 2.2 一次对话的数据流

1. 小程序 `POST /api/ai/agent/chat`（`Authorization: Bearer <患者JWT>`，body: `{sessionId?, text}`）。
2. clinic-api 校验 JWT → 患者 `userId`；若不带 sessionId 则查找/创建该患者的固定会话。
3. clinic-api 调 clinic-agent `POST /clinic/session-token`（`patient:{userId}` → JWT，TTL 10 分钟滑动续期）。
4. clinic-agent 以 `X-User-ID: patient:{userId}` 调 `POST /chat/` 触发运行（fire-and-forget）。
5. clinic-api 订阅 clinic-agent `GET /sessions/{sid}/stream`（SSE），把 AgentEvent 流式透传给小程序。
6. agent 运行中调用业务工具 → 工具从 token 注册表按 user_id 取 JWT → httpx 回调 clinic-api 业务接口（权限自动对齐）。
7. 写操作 → `RequireUserConfirmEvent` → 小程序确认卡片 → 用户点击 → `POST /chat/` 提交 `UserConfirmResultEvent` → 恢复执行。

**身份安全边界**：患者 JWT 永不进入对话内容、agent 记忆、事件流；仅存于服务侧注册表（短 TTL），工具调用时按 user_id 解析。

---

## 3. 组件设计

### 3.1 clinic-agent 服务（新）

```
_agent/clinic_agent_service/
├─ main.py            # create_app(...)，端口 8001
├─ tools.py           # 12 个业务工具（httpx → clinic-api）
├─ token_registry.py  # 患者 JWT 注册表（Redis，TTL 10min）
└─ prompts.py         # 系统 prompt（总/业务）
```

**agent 结构（总 + 1 业务子 agent）**：

| | 总 agent「诊所助手」 | 业务子 agent「诊所业务专员」 |
|---|---|---|
| 职责 | 闲聊、意图识别、派发任务、转述结果 | 执行全部业务操作 |
| 工具 | 团队工具（spawn 子 agent / TeamSay 汇报），无业务工具 | 12 个业务工具 |
| 入口 | 患者会话的入口 agent | 总 agent 通过 team 机制 spawn |

工具经 `create_app(extra_agent_tools=工具工厂)` 注入；工具工厂签名 `(user_id, agent_id, session_id) → list[ToolBase]`，按 user_id 从注册表取 JWT。业务工具只挂给业务子 agent（工厂按 agent 名单分发；总 agent 的 spawn/TeamSay 由 team 机制提供）。

**业务工具清单**（→ clinic-api 映射）：

| 工具 | clinic-api 接口 | 现状 |
|---|---|---|
| 查科室列表 | GET /miniapp/departments | 已有 |
| 查医生 | GET /miniapp/doctors | 已有 |
| 查排班号源 | GET /miniapp/schedules | 已有 |
| 查我的挂号 | GET /miniapp/registration/my-list | 已有 |
| 查排队号 | GET /miniapp/registration/{id}/queue | 已有 |
| 创建挂号⚠️ | POST /miniapp/registration/create | **新增**（幂等键） |
| 取消挂号⚠️ | POST /miniapp/registration/cancel | **新增** |
| 查药单列表 | GET /miniapp/prescriptions | **新增** |
| 查药单明细 | GET /miniapp/prescriptions/{id} | **新增** |
| 查病历列表 | GET /miniapp/medical/record/list | 已有 |
| 查病历详情 | GET /miniapp/medical/record/{id} | 已有 |
| 查就诊人 | GET /miniapp/patient/list | 已有 |

⚠️ = 需患者确认的写操作。所有工具返回结构化数据（JSON），大结果截断（排班最多 14 天、列表最多 20 条），详情让 agent 按需再查明细。

**写操作确认**：创建/取消挂号工具标记 require-confirm → 框架暂停 run 并发 `RequireUserConfirmEvent`（携带工具名+参数）→ 小程序渲染卡片（医生/时间/就诊人）→ 患者确认 → `UserConfirmResultEvent` 恢复。全程框架原生 HITL，不自造协议。

**会话模型**：每患者一个长期会话（按 `patient:{userId}` + 固定 agent 名定位），历史持久化在 Redis，多轮上下文连续。

**模型**：glm-5.2（已配好的 okflow 中转站凭据，base_url 带 /v1）。

### 3.2 clinic-api 改动

1. **AgentProxyController**（`/api/ai/agent/*`，全部走 JWT 鉴权）：
   - `POST /chat`：注册 token → 触发 agent 运行 → 返回 `{sessionId}`；SSE 响应（`text/event-stream`）透传事件。
   - `POST /interrupt`：转发 `POST /sessions/{sid}/interrupt`。
   - `POST /confirm`：转发 `UserConfirmResultEvent`。
   - `GET /history`：查该患者会话的历史消息（恢复聊天页时用）。
2. **新增患者接口**（上表 4 个"新增"，其中创建挂号带 `clientRequestId` 幂等键：同键重复调用返回同一单，防 agent 重试导致重复挂号）。
3. agent 创建的挂号打 `source=agent` 标记（复用 OperationLogAspect 审计）。

### 3.3 小程序改动（clinic-miniapp）

- 新页面 `pages/agent/chat.vue`：消息流（用户/助手气泡、思考中动效、工具执行状态行）、底部输入框、**停止按钮**、**确认卡片**（写操作）、错误重试按钮。
- SSE 消费：uni.request enableChunked 分块读取（mp-weixin），H5 用 fetch 流。
- 首页入口："智能助手"浮球/按钮。
- 事件渲染映射：`TextBlockDelta`→正文流式；`ThinkingBlockDelta`→"思考中…"（不显示内容）；`ToolCallStart/End`→"正在查询排班…"状态行；`RequireUserConfirmEvent`→确认卡片；`ReplyEndEvent`→完成；错误帧→重试按钮。

---

## 4. Harness：健壮性设计

### 4.1 分层超时与中断

| 层 | 超时/机制 | 触发动作 |
|---|---|---|
| 工具→clinic-api | httpx 连接 3s / 读 10s | 抛 ToolError → agent 得到失败结果，向患者解释（见 4.4） |
| LLM 流式 | openai SDK read timeout 60s | SDK 自动重试（见 4.3） |
| chat run 总时长 | 代理层 watchdog：SSE 120s 无有效事件（心跳不算） | 代理层主动调 interrupt + 下发错误帧，会话可继续 |
| SSE 空闲 | 服务端心跳（框架已有） | 小程序 90s 无事件自动重连（框架 stream 支持缓冲重放，不丢事件） |
| UserConfirm 等待 | 小程序卡片 5 分钟倒计时 | 过期→自动调 interrupt 取消本次操作；患者也可手动点取消 |
| agent 死循环 | 框架 max_iters（默认值按业务调 10） | finished_reason=exceed_max_iters，正常结束并提示 |

**长时间思考的中断**（glm-5.2 是思考模型）：
- 思考过程以 `ThinkingBlockDelta` 事件持续流出 → 前端始终显示"思考中…"动效，不会静默卡死；
- 患者点"停止" → `POST /api/ai/agent/interrupt` → 代理转发框架 `POST /sessions/{sid}/interrupt`（原生支持，可中断运行中或确认等待中的 run）→ 已生成文本保留进历史，UI 标注"已停止"；
- 双保险：watchdog 兜底僵死（见上表）；
- 预防：业务子 agent 系统 prompt 约束"简短思考、快速调工具"；模型 thinking 预算可经参数下调（留配置项）。

### 4.2 走偏检测与纠偏

思考/执行走错路的处理分四层：让用户看得见 → 用户拉回 → 系统自动纠偏 → 事后防重犯。

**A. 实时可观测（检测前提）**：`ToolCallStartEvent` 携带工具名，参数经 `ToolCallDeltaEvent` 流出 → 小程序状态行实时显示"正在查询排班（张医生，明天）"；思考中持续显示动效。用户能第一时间发现方向不对。

**B. 用户手动拉回（主手段）**：点停止（已生成内容保留进历史）→ 发纠正消息（"不对，我要查下个月的"）→ 新一轮 run 带完整上下文（含被打断的尝试）。配套两层提示词硬约束：
- 业务子 agent："用户纠正消息是最高优先级指令——立即放弃当前方向，不得继续旧工具链中未完成的步骤"；
- 会话记忆（AgenticMemory）强化纠正语料，本轮会话内不重犯同类错误。
- 实现备注：框架 inbox 有 mid-run 消息排队迹象，实现首日验证；若支持，"停止+纠正"合并为一步"插话"；不支持则保持两步，体验无本质差异。

**C. 系统自动纠偏（用户未察觉时的兜底）**：
1. **工具循环检测**：工具层包装器对"同名+同参"连续调用计数，第 3 次起不执行、直接返回纠正性结果："你已连续 3 次以相同参数调用此工具，请停止重复，基于已有信息回答用户，或向用户澄清需求" → 模型下一轮自我纠正。零成本，harness 经典手法。
2. **无产出中断即纠偏**：4.1 的 watchdog（120s 无有效事件）触发 interrupt 后，不只报错——自动追加一条系统级引导消息（"刚才的方向没有进展，请重新理解用户需求，基于已获取的信息简洁回答"）并触发新 run。
3. **路由纠偏**（总 agent 派错任务）：业务子 agent 提示词规定——发现任务与自己的工具集无关时，用 TeamSay 告知总 agent"此任务不属于我"，由总 agent 重新处理；不得硬做、不得编造。
4. **max_iters 到顶**：正常结束，但提示词要求结束前必须向用户交代现状+建议下一步，不得沉默退出。

**D. 事后防重犯**：会话摘要压缩时显式保留"用户纠正记录"关键事实，长会话中不重复同类走偏。

### 4.3 重试策略

| 层 | 策略 | 说明 |
|---|---|---|
| LLM 调用 | SDK 内建 max_retries=3，指数退避 | 框架默认，无需代码；429/5xx/连接错误覆盖 |
| 读类工具（GET） | httpx 重试 2 次（0.5s/2s 退避） | 幂等，安全 |
| 写类工具（挂号） | **不自动重试** | 防重复下单；失败由 agent 告知患者；真防重靠 `clientRequestId` 幂等键（clinic-api upsert 语义） |
| 代理层→agent 服务 | 连接类错误重试 1 次 | 仅触发阶段；SSE 建立后断线靠重连+重放 |
| 小程序 | SSE 断线自动重连（最多 3 次） | 之后显示重试按钮 |
| 限流保护 | 代理层 per-user 限流：6 条消息/分钟 | 防刷防重试风暴 |

### 4.4 兜底降级链（每一步失败的患者体验）

1. **clinic-agent 不可达**（启动失败/Redis 挂）→ 代理层捕获连接错误 → 返回降级话术："智能助手暂时不在，您可以在首页使用预约挂号/我的药单功能"，附带静态入口。start-all.bat 启动链已含 Redis 依赖。
2. **模型 API 故障/限流**（重试 3 次仍失败）→ run 以错误结束 → 代理层转友好错误帧 → 小程序显示"回复失败"+重试按钮（= 重发最后一条用户消息）。
3. **业务工具失败**（clinic-api 超时/5xx）→ 工具返回错误描述给 agent（不抛给用户）→ agent 按提示词向患者解释并给替代路径："查询失败了，您也可以在『我的-预约挂号』里查看"。
4. **子 agent 派发失败**（team spawn 异常）→ 总 agent 提示词规定降级行为：道歉 + 引导患者到对应功能页；不重试 spawn。
5. **JWT 过期**（患者长时间挂机后发消息，工具收到 401）→ 工具抛特定错误 → agent 提示"登录已过期，请刷新后重试"；注册表 TTL 滑动续期保证正常对话中不会过期。
6. **上下文超长** → 框架会话记忆压缩（summary）+ 工具侧结果截断（见 3.1）；超长历史自动摘要，不丢关键信息。
7. **agent 回答超纲**（问支付/会员等一期外业务）→ 总 agent 提示词硬约束：明确告知"该功能暂未开放，预计近期上线"，不得编造。

### 4.5 并发模型

- 同一患者存在活跃 run 时再发消息 → 代理层返回 409 + "正在回复中"，小程序置灰发送按钮直至 ReplyEnd/中断（串行化，避免交叉回答）。
- 不同患者之间天然并行（agent 服务多会话隔离）。

### 4.6 安全

- JWT 只存服务侧注册表（TTL 10min），不进对话/日志/事件流；代理层日志脱敏。
- 写操作双保险：HITL 确认 + 幂等键。
- agent 只能通过工具访问业务，无直接 DB 权限；工具即患者权限边界。
- 生产部署：clinic-agent 不暴露公网，仅 clinic-api 内网访问；微信域名/HTTPS 只需覆盖 clinic-api。

---

## 5. 关键时序

### 5.1 写操作确认（创建挂号）

```
患者: 帮我约明天上午张医生
总agent → spawn 业务子agent → 查排班工具 → 回: 9:00/9:30/10:00 可约
患者: 9:30 给张三
业务子agent → 创建挂号工具(确认标记) → RequireUserConfirmEvent
  → 小程序卡片: 张三 / 张医生 / 明天9:30 / [确认] [取消]
患者点确认 → UserConfirmResultEvent → 工具执行 → 回: ✅ 预约成功 排队号12号
```

### 5.2 中断

```
患者发消息 → agent 思考中(ThinkingBlockDelta 流出, UI 显示动效)
患者点[停止] → POST /api/ai/agent/interrupt → 框架 interrupt
  → 部分文本保留, UI 标注"已停止", 会话可继续下一条
```

---

## 6. 测试策略

1. **工具单测**（pytest + httpx MockTransport）：每个工具的正常/超时/401/5xx 分支；幂等键防重。
2. **agent 冒烟脚本**（直连 clinic-agent 服务）：典型 10 轮对话断言工具调用与回复；HITL 流（模拟 confirm）；interrupt 流；max_iters 行为。
3. **代理层集成测**：clinic-api + clinic-agent 双起，mock 模型或用真实 glm-5.2；409 并发；限流；watchdog。
4. **端到端联调**：`uni dev:h5` 浏览器全流程（含确认卡片、停止、断线重连）；mp-weixin 真机最后验证（分块 SSE、卡片交互）。
5. **稳定性脚本**：10 并发患者 × 20 轮，观察 Redis/内存/事件丢失。

---

## 7. 二期展望

- 管理端 agent（按角色：医生 agent / 收银 agent / 运营 agent，复用本架构的代理与工具模式）。
- 小程序端业务扩展（支付、就诊人管理、随访）。
- 主动通知（排队号临近提醒，经 schedule/通道机制）。

## 8. 风险与开放问题

- **R1** SubAgentTemplate 的工具绑定粒度需在实现首日验证：若模板不支持独立工具集，回退方案为工具工厂按 agent 名单分发（3.1 已含）。
- **R2** mp-weixin 分块 SSE 的兼容性需真机早验（H5 先行开发不受阻）。
- **R3** glm-5.2 经中转站的稳定性（偶发慢/断流）已有 watchdog 兜底；若持续可切直连智谱官方 API（凭据另建即可）。
- **R4** team 机制（spawn+TeamSay）链路比单 agent 长，延迟约 +1 次 LLM 调用；用户已知晓并选择此形态。
