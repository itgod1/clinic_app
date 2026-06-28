"""
FastAPI 接口服务
提供 HTTP API 供 Java 后端代理调用
"""
import os
import json
import uuid
from typing import Optional, List
from datetime import datetime
from contextlib import asynccontextmanager

# 优先加载 .env 文件
try:
    from dotenv import load_dotenv
    _env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
    load_dotenv(_env_path)
except ImportError:
    pass

from fastapi import FastAPI, APIRouter, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from graph import get_workflow, start_return_visit, continue_return_visit


# ==================== 请求/响应模型 ====================

class PatientInfoRequest(BaseModel):
    patient_id: int
    name: str
    phone: Optional[str] = None
    age: Optional[int] = None
    gender: Optional[str] = None
    medical_history: List[str] = Field(default_factory=list)
    allergies: List[str] = Field(default_factory=list)
    last_visit_date: Optional[str] = None
    last_treatment: Optional[str] = None


class VisitContextRequest(BaseModel):
    plan_id: int
    plan_type: str
    related_treatment: Optional[str] = None
    scheduled_date: Optional[str] = None
    doctor_name: Optional[str] = None
    department: Optional[str] = None


class StartRequest(BaseModel):
    patient_info: PatientInfoRequest
    visit_context: VisitContextRequest
    conversation_id: Optional[str] = None


class ContinueRequest(BaseModel):
    conversation_id: str
    user_input: str


class ChatRequest(BaseModel):
    """统一对话请求（前端兼容格式）"""
    query: str
    conversation_id: Optional[str] = ""
    inputs: dict = Field(default_factory=dict)
    user: str = "anonymous"


class ConversationResponse(BaseModel):
    conversation_id: str
    status: str
    response: str
    should_escalate: bool = False
    is_completed: bool = False
    satisfaction_score: Optional[int] = None
    messages: list = Field(default_factory=list)


class ConversationStateResponse(BaseModel):
    conversation_id: str
    status: str = ""
    step: Optional[str] = None
    message_count: int = 0
    satisfaction_score: Optional[int] = None


class HealthResponse(BaseModel):
    status: str
    version: str
    timestamp: str


# ==================== 路由定义 ====================

# API v1 路由 — 所有 AI 回访接口统一前缀 /api/v1
router = APIRouter(prefix="/fastapi/v1", tags=["AI回访"])


# ---- 对话 ----

@router.post("/chat", response_model=ConversationResponse)
async def unified_chat(request: ChatRequest):
    """
    统一对话接口（推荐）
    自动判断首次对话/继续对话
    """
    try:
        conversation_id = request.conversation_id

        if not conversation_id:
            patient_info = request.inputs.get("patient_info", {})
            visit_context = request.inputs.get("visit_context", {})

            if not patient_info or not visit_context:
                raise HTTPException(status_code=400, detail="新对话需要提供 inputs.patient_info 和 inputs.visit_context")

            conversation_id = _new_conversation_id()
            print(f"\n{'='*40}")
            print(f"[新对话] id={conversation_id}")
            print(f"[患者] {patient_info.get('name', '?')} | 治疗: {visit_context.get('related_treatment', '?')}")

            # 先生成问候语
            greeting_result = start_return_visit(
                conversation_id=conversation_id,
                patient_info=patient_info,
                visit_context=visit_context
            )
            print(f"[问候] {greeting_result['response']}...")

            # 如果用户带了实质查询，紧接处理
            user_query = request.query.strip()
            if user_query and user_query != "开始回访":
                print(f"[用户输入] {user_query}")
                result = continue_return_visit(
                    conversation_id=conversation_id,
                    user_input=user_query
                )
                print(f"[AI回复] {result['response']}...")
                # 拼接问候+回复
                result["response"] = greeting_result["response"] + "\n" + result["response"]
                result["messages"] = greeting_result["messages"] + result["messages"]
            else:
                result = greeting_result
        else:
            print(f"\n[继续对话] id={conversation_id} | 用户: {request.query}")
            result = continue_return_visit(
                conversation_id=conversation_id,
                user_input=request.query
            )
            print(f"[AI回复] {result['response']}...")
            print(f"[状态] {result['status']} | 完成: {result.get('is_completed')} | 转人工: {result.get('should_escalate')}")

        return ConversationResponse(
            conversation_id=result["conversation_id"],
            status=result["status"],
            response=result["response"],
            should_escalate=result.get("should_escalate", False),
            is_completed=result.get("is_completed", False),
            satisfaction_score=result.get("satisfaction_score"),
            messages=result["messages"]
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"对话处理失败: {str(e)}")


@router.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    """流式对话（SSE）"""
    async def generate():
        try:
            conversation_id = request.conversation_id

            if not conversation_id:
                patient_info = request.inputs.get("patient_info", {})
                visit_context = request.inputs.get("visit_context", {})

                if not patient_info or not visit_context:
                    yield f"data: {json.dumps({'error': '缺少患者信息或回访上下文'})}\n\n"
                    return

                conversation_id = _new_conversation_id()
                print(f"\n[新对话流] id={conversation_id} | 患者={patient_info.get('name', '?')}")

                # 第一步：生成问候语
                result = start_return_visit(
                    conversation_id=conversation_id,
                    patient_info=patient_info,
                    visit_context=visit_context
                )
                print(f"[问候] {result['response']}...")

                # 输出问候语
                greeting_text = result["response"]
                for i in range(0, len(greeting_text), 3):
                    chunk = greeting_text[i:i+3]
                    yield f"data: {json.dumps({'event': 'message', 'answer': chunk, 'conversation_id': conversation_id})}\n\n"

                # 第二步：如果用户带了实质查询，继续处理
                user_query = request.query.strip()
                if user_query and user_query != "开始回访":
                    print(f"[用户输入] {user_query}")
                    result = continue_return_visit(
                        conversation_id=conversation_id,
                        user_input=user_query
                    )
                    print(f"[AI回复] {result['response']}...")

                    response_text = result["response"]
                    for i in range(0, len(response_text), 3):
                        chunk = response_text[i:i+3]
                        yield f"data: {json.dumps({'event': 'message', 'answer': chunk, 'conversation_id': conversation_id})}\n\n"

                yield f"data: {json.dumps({'event': 'message_end', 'conversation_id': conversation_id, 'metadata': {'status': result['status'], 'is_completed': result.get('is_completed', False), 'satisfaction_score': result.get('satisfaction_score')}})}\n\n"

            else:
                print(f"\n[继续对话流] id={conversation_id} | 用户: {request.query}")
                result = continue_return_visit(
                    conversation_id=conversation_id,
                    user_input=request.query
                )
                print(f"[AI回复] {result['response']}...")

                response_text = result["response"]
                for i in range(0, len(response_text), 3):
                    chunk = response_text[i:i+3]
                    yield f"data: {json.dumps({'event': 'message', 'answer': chunk, 'conversation_id': conversation_id})}\n\n"

                yield f"data: {json.dumps({'event': 'message_end', 'conversation_id': conversation_id, 'metadata': {'status': result['status'], 'is_completed': result.get('is_completed', False), 'satisfaction_score': result.get('satisfaction_score')}})}\n\n"

        except Exception as e:
            yield f"data: {json.dumps({'error': str(e)})}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "Connection": "keep-alive", "X-Accel-Buffering": "no"}
    )


@router.post("/conversation/start", response_model=ConversationResponse)
async def start_conversation(request: StartRequest):
    """开始新对话，生成问候语"""
    try:
        conversation_id = request.conversation_id or _new_conversation_id()

        result = start_return_visit(
            conversation_id=conversation_id,
            patient_info=request.patient_info.model_dump(),
            visit_context=request.visit_context.model_dump()
        )

        return ConversationResponse(
            conversation_id=result["conversation_id"],
            status=result["status"],
            response=result["response"],
            messages=result["messages"]
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"启动对话失败: {str(e)}")


@router.post("/conversation/continue", response_model=ConversationResponse)
async def continue_conversation(request: ContinueRequest):
    """继续已有对话"""
    try:
        result = continue_return_visit(
            conversation_id=request.conversation_id,
            user_input=request.user_input
        )

        return ConversationResponse(
            conversation_id=result["conversation_id"],
            status=result["status"],
            response=result["response"],
            should_escalate=result.get("should_escalate", False),
            is_completed=result.get("is_completed", False),
            satisfaction_score=result.get("satisfaction_score"),
            messages=result["messages"]
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"继续对话失败: {str(e)}")


# ---- 对话管理 ----

@router.get("/conversation/{conversation_id}/state", response_model=ConversationStateResponse)
async def get_conversation_state(conversation_id: str):
    """获取对话状态"""
    try:
        workflow = get_workflow()
        state = workflow.get_conversation_state(conversation_id)

        if "error" in state:
            raise HTTPException(status_code=404, detail=state["error"])

        return ConversationStateResponse(
            conversation_id=state["conversation_id"],
            status=str(state.get("status", "")),
            step=state.get("step"),
            message_count=state.get("message_count", 0),
            satisfaction_score=state.get("satisfaction_score")
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取状态失败: {str(e)}")


@router.get("/conversation/{conversation_id}/messages")
async def get_conversation_messages(conversation_id: str):
    """获取对话消息历史"""
    try:
        workflow = get_workflow()
        config = {"configurable": {"thread_id": conversation_id}}
        full_state = workflow.app.get_state(config)

        messages = []
        if full_state and full_state.values:
            for m in full_state.values.get("messages", []):
                messages.append({
                    "role": m.role,
                    "content": m.content,
                    "timestamp": m.timestamp.isoformat()
                })

        return {"conversation_id": conversation_id, "messages": messages, "count": len(messages)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取消息失败: {str(e)}")


@router.post("/conversation/{conversation_id}/end")
async def end_conversation(conversation_id: str):
    """强制结束对话"""
    try:
        continue_return_visit(conversation_id=conversation_id, user_input="结束对话")
        return {"conversation_id": conversation_id, "status": "ended", "message": "对话已结束"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"结束对话失败: {str(e)}")


# ---- Webhook & 批量 ----

@router.post("/webhook/dify")
async def dify_webhook(data: dict):
    """兼容 Dify 的 Webhook 接口（过渡期）"""
    try:
        event = data.get("event")

        if event == "conversation_started":
            result = start_return_visit(
                conversation_id=data.get("conversation_id"),
                patient_info=data.get("patient_info", {}),
                visit_context=data.get("visit_context", {})
            )
            return {"event": "conversation_started", "conversation_id": data.get("conversation_id"), "response": result["response"]}

        elif event == "message_received":
            result = continue_return_visit(
                conversation_id=data.get("conversation_id"),
                user_input=data.get("message", "")
            )
            return {
                "event": "message_received",
                "conversation_id": data.get("conversation_id"),
                "response": result["response"],
                "should_escalate": result.get("should_escalate", False),
                "is_completed": result.get("is_completed", False)
            }

        return {"status": "ignored", "event": event}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Webhook 处理失败: {str(e)}")


@router.post("/batch/process-pending")
async def process_pending_tasks(background_tasks: BackgroundTasks):
    """批量处理待回访任务（定时任务调用）"""
    background_tasks.add_task(_process_pending_tasks)
    return {"status": "processing", "message": "批量处理任务已启动"}


# ==================== 应用实例 ====================

@asynccontextmanager
async def lifespan(app: FastAPI):
    print("=" * 50)
    print("LangGraph AI 回访服务启动")
    print("API 文档: http://localhost:8000/docs")
    print("=" * 50)
    get_workflow()
    yield
    print("服务关闭")


app = FastAPI(
    title="AI 回访服务",
    description="基于 LangGraph 的口腔诊所 AI 回访工作流服务",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(router)


# ---- 健康检查（不带 prefix，挂载在根路径）----

@app.get("/health", response_model=HealthResponse)
async def health_check():
    return HealthResponse(status="healthy", version="1.0.0", timestamp=datetime.now().isoformat())


# ==================== 内部函数 ====================

def _new_conversation_id() -> str:
    return f"LG-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8].upper()}"


async def _process_pending_tasks():
    """异步处理待回访任务"""
    from tools import api_client as client
    try:
        tasks = client.get_pending_tasks(limit=50)
        workflow = get_workflow()

        for task in tasks:
            try:
                patient_id = task.get("patientId")
                plan_id = task.get("planId")
                if not patient_id:
                    continue

                profile = client.get_patient_profile(patient_id)
                plan = client.get_plan_detail(plan_id) if plan_id else {}

                patient_info = {
                    "patient_id": patient_id,
                    "name": profile.get("patientName", ""),
                    "phone": profile.get("phone"),
                    "age": profile.get("age"),
                    "gender": profile.get("gender"),
                    "medical_history": profile.get("medicalHistory", []),
                    "allergies": profile.get("allergies", []),
                    "last_visit_date": profile.get("lastVisitDate"),
                    "last_treatment": profile.get("lastTreatment")
                }

                visit_context = {
                    "plan_id": plan_id or 0,
                    "plan_type": plan.get("visitType", "术后回访"),
                    "related_treatment": plan.get("visitItem"),
                    "scheduled_date": plan.get("planDate"),
                    "doctor_name": plan.get("doctorName"),
                    "department": plan.get("department")
                }

                conversation_id = _new_conversation_id()
                workflow.start_conversation(conversation_id, patient_info, visit_context)
                client.trigger_task(task.get("taskId"))

            except Exception as e:
                print(f"处理任务失败: {task.get('taskId')}, 错误: {e}")

    except Exception as e:
        print(f"批量处理失败: {e}")


# ==================== 启动入口 ====================

if __name__ == "__main__":
    host = os.getenv("HOST", "0.0.0.0")
    port = int(os.getenv("PORT", 8000))
    import uvicorn

    print(f"LangGraph 回访服务启动中 → http://{host}:{port}")
    print(f"API 文档 → http://{host}:{port}/docs")
    print("按 Ctrl+C 停止服务")
    uvicorn.run(app, host=host, port=port)
