# LangGraph AI 回访工作流

基于 LangGraph 的口腔诊所 AI 回访服务，已完全替代 Dify 工作流。

## 项目结构

```
return_visit_workflow/
├── __init__.py          # 包初始化
├── state.py             # 状态定义
├── tools.py             # 工具函数
├── nodes.py             # 工作流节点
├── graph.py             # LangGraph 工作流
├── api.py               # FastAPI 接口
├── integration.py       # Java 后端集成
├── requirements.txt     # 依赖
├── .env.example         # 环境变量示例
└── README.md            # 本文档
```

## 核心功能

- **智能对话管理**：多轮对话，上下文记忆
- **意图识别**：自动识别用户意图（问候、咨询、投诉等8种）
- **知识库问答**：基于治疗类型的专业知识回答（拔牙、种植牙、根管治疗、正畸、洗牙、补牙）
- **满意度收集**：自动收集患者满意度评分（1-5分）
- **人工转接**：检测投诉/医疗紧急情况自动转人工
- **状态持久化**：对话状态保存，支持断点续传
- **流式响应**：支持 SSE 流式输出

## 快速开始

### 1. 安装依赖

```bash
cd src/main/python/return_visit_workflow
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
# 编辑 .env 文件，填入你的配置
```

### 3. 启动服务

```bash
python api.py
```

服务将启动在 http://localhost:8000

API 文档：http://localhost:8000/docs

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/health` | 健康检查 |
| POST | `/fastapi/v1/chat` | 统一对话接口（推荐Java代理使用） |
| POST | `/fastapi/v1/chat/stream` | 流式对话（SSE） |
| POST | `/fastapi/v1/conversation/start` | 开始新对话 |
| POST | `/fastapi/v1/conversation/continue` | 继续已有对话 |
| POST | `/fastapi/v1/conversation/{id}/end` | 强制结束对话 |
| GET | `/fastapi/v1/conversation/{id}/state` | 获取对话状态 |
| GET | `/fastapi/v1/conversation/{id}/messages` | 获取对话消息历史 |
| POST | `/fastapi/v1/webhook/dify` | Dify兼容Webhook（过渡期） |
| POST | `/fastapi/v1/batch/process-pending` | 批量处理待回访任务 |

### 统一对话接口（推荐）

```http
POST /fastapi/v1/chat
Content-Type: application/json

{
  "query": "我感觉恢复得不错",
  "conversation_id": "LG-20250531-ABC12345",
  "inputs": {
    "patient_info": {
      "patient_id": 123,
      "name": "张三",
      "phone": "138****8000",
      "age": 30,
      "gender": "男",
      "medical_history": [],
      "allergies": [],
      "last_visit_date": "2025-05-24",
      "last_treatment": "拔牙"
    },
    "visit_context": {
      "plan_id": 456,
      "plan_type": "术后回访",
      "related_treatment": "拔牙",
      "scheduled_date": "2025-05-31",
      "doctor_name": "李医生",
      "department": "口腔外科"
    }
  },
  "user": "patient-123"
}
```

首次对话时 `conversation_id` 留空，服务会自动生成。

## 工作流流程

```
router_node（路由）
├── greeting_node（问候）→ END（返回用户）
└── intent_analysis_node（意图分析）
    └── generate_response_node（生成回复）
        └── check_completion_node（完成检查）
            ├── END（等待用户输入）
            ├── collect_satisfaction_node（收集满意度）
            │   └── end_node（结束）
            ├── escalate_node（转人工）
            │   └── end_node（结束）
            └── end_node（结束）
```

## 与 Java 后端集成

### 请求流程

```
微信小程序 → Java DifyProxyController (/ai/dify/chat)
    → LangGraph Service (/fastapi/v1/chat)
    → 返回 AI 回复
```

### 数据同步

LangGraph 服务在对话结束时回调 Java 后端保存记录：
- `POST /ai/return-visit/conversation/{id}/complete` — 完成对话并保存

### Java 配置

```yaml
langgraph:
  api-url: http://localhost:8000
  api-key: sk-clinic-ai-2025-a1b2c3d4e5f6
```

## 配置说明

### OpenAI 配置

```env
OPENAI_API_KEY=your-api-key
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o-mini
```

### 服务配置

```env
HOST=0.0.0.0
PORT=8000
```

### Java 后端配置

```env
CLINIC_API_URL=http://localhost:8080
CLINIC_API_KEY=sk-clinic-ai-2025-a1b2c3d4e5f6
```

## 开发指南

### 添加新的治疗类型知识

在 `tools.py` 的 `KnowledgeBase` 类中添加：

```python
self.treatment_guidelines["新治疗"] = {
    "术后注意事项": [...],
    "常见问题": {...}
}
```

### 扩展意图分类

在 `tools.py` 的 `IntentClassifier.KEYWORDS` 中添加新的意图类型和关键词。

### 添加新的工作流节点

1. 在 `nodes.py` 中实现节点函数
2. 在 `graph.py` 中注册节点
3. 配置路由逻辑

## 许可证

MIT
