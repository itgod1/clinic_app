"""
工作流节点定义
实现 LangGraph 的各个节点逻辑
"""
import os
from typing import Dict, Any
from datetime import datetime
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

from state import ReturnVisitState, Message, ConversationStatus, IntentType
from tools import (
    api_client,
    knowledge_base,
    intent_classifier,
    satisfaction_collector
)


# LLM 实例（延迟加载，避免导入时报错）
_llm = None


def get_llm() -> ChatOpenAI:
    global _llm
    if _llm is None:
        _llm = ChatOpenAI(
            model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
            temperature=0.7,
            api_key=os.getenv("OPENAI_API_KEY"),
            base_url=os.getenv("OPENAI_BASE_URL")
        )
    return _llm


# ==================== 路由节点 ====================

def router_node(state: ReturnVisitState) -> Dict[str, Any]:
    """
    路由节点：根据当前 step 决定走哪个分支
    - "start" → 走 greeting 流程
    - "intent_analysis" → 走意图分析流程（继续对话）
    - 其他 → END
    此节点本身不修改状态，只做路由
    """
    return {}


# ==================== 问候节点 ====================

def greeting_node(state: ReturnVisitState) -> Dict[str, Any]:
    """问候节点：生成初始问候语"""
    patient_name = state["patient_info"].name
    treatment = state["visit_context"].related_treatment or "治疗"
    doctor_name = state["visit_context"].doctor_name or ""

    greeting_prompt = f"""请为口腔诊所的患者 {patient_name} 生成一段术后回访问候语。

背景信息：
- 治疗项目：{treatment}
- 主治医生：{doctor_name}
- 回访类型：术后恢复情况回访

要求：
1. 友好亲切，称呼患者姓名
2. 表明自己是诊所的AI回访助手
3. 说明回访目的是了解术后恢复情况
4. 询问患者目前的感受（如有无疼痛、肿胀、出血等）
5. 控制在80字以内
6. 以问句结尾，引导患者回复

请直接输出问候语，不要包含引号或其他标记。"""

    messages_payload = [
        SystemMessage(content="你是口腔诊所的AI回访助手，语气亲切专业。"),
        HumanMessage(content=greeting_prompt)
    ]

    response = get_llm().invoke(messages_payload)
    greeting = response.content.strip().strip('"').strip("'")
    print(f"[LLM问候] {greeting}...")

    return {
        "messages": [Message(role="assistant", content=greeting)],
        "current_response": greeting,
        "status": ConversationStatus.IN_PROGRESS,
        "step": "waiting_for_response"
    }


# ==================== 意图分析节点 ====================

def intent_analysis_node(state: ReturnVisitState) -> Dict[str, Any]:
    """意图分析节点：分类用户输入意图"""
    user_input = state.get("current_input", "")

    if not user_input:
        return {"current_intent": IntentType.OTHER, "step": "generate_response"}

    intent = intent_classifier.classify(user_input)
    print(f"[意图分析] 用户: {user_input}→ {intent}")
    user_message = Message(role="user", content=user_input)

    return {
        "messages": [user_message],
        "current_intent": intent,
        "step": "generate_response"
    }


# ==================== 响应生成节点 ====================

def generate_response_node(state: ReturnVisitState) -> Dict[str, Any]:
    """响应生成节点：根据意图和上下文生成AI回复"""
    intent = state.get("current_intent", IntentType.OTHER)
    user_input = state.get("current_input", "")
    patient_info = state["patient_info"]
    visit_context = state["visit_context"]
    messages = state["messages"]

    treatment = visit_context.related_treatment

    # 从 Java 后端查数据库获取真实患者档案（首次调用后缓存）
    collected = state.get("collected_info", {})
    profile_not_found = False
    if "profile" not in collected:
        pid = patient_info.patient_id
        print(f"[查数据库] 调用 GET /api/ai/return-visit/patient/{pid}/profile ...")
        profile = api_client.get_patient_profile(pid)
        if profile and profile.get("patientName"):
            collected["profile"] = profile
            print(f"[查数据库] 获取到真实数据: {profile.get('patientName')}")
        else:
            collected["profile"] = None
            profile_not_found = True
            print(f"[查数据库] 数据库中未找到患者 {pid}")

    profile = collected.get("profile")
    # 如果数据库查不到，不 fallback 静态数据，直接告知查不到
    real_name = profile.get("patientName") if profile else None
    real_age = profile.get("age") if profile else None
    real_gender = profile.get("genderText") or (profile.get("gender") if profile else None)
    real_allergies = [profile.get("allergyHistory")] if profile and profile.get("allergyHistory") else None
    real_history = [profile.get("medicalHistory")] if profile and profile.get("medicalHistory") else None
    real_last_visit = profile.get("lastVisitDate") if profile else None
    real_phone = profile.get("phone") if profile else None
    real_visit_count = profile.get("visitCount") if profile else None

    guidelines_text = ""
    faqs_text = ""

    if treatment:
        guidelines = knowledge_base.get_treatment_guidelines(treatment)
        if guidelines:
            if "术后注意事项" in guidelines:
                guidelines_text = "术后注意事项：\n" + "\n".join(
                    f"- {item}" for item in guidelines["术后注意事项"]
                )
            elif "注意事项" in guidelines:
                guidelines_text = "注意事项：\n" + "\n".join(
                    f"- {item}" for item in guidelines["注意事项"]
                )
        faqs = knowledge_base.search_faq(user_input, treatment)
        if faqs:
            faqs_text = "相关FAQ：\n" + "\n".join(
                f"Q: {faq['question']}\nA: {faq['answer']}" for faq in faqs
            )

    # 确定回复策略
    intent_strategies = {
        "greeting": "用户在进行问候，友好回应并引导到回访主题",
        "satisfied": "用户表示满意/恢复良好，给予肯定并询问是否还有其他需要",
        "dissatisfied": "用户表示不适/不满意，关切询问具体情况，给出护理建议",
        "complaint": "用户投诉，表示歉意并记录问题，承诺转达给相关人员",
        "appointment": "用户想预约，告知会转达预约需求给诊所前台",
        "medical": "用户咨询医疗问题，根据知识库给出专业建议，严重时建议就诊",
        "end": "用户表示对话可以结束，给予温暖的告别语和健康祝福",
        "inquiry": "用户咨询，根据知识库给出准确回答",
        "other": "通用回复，引导用户继续讨论回访相关话题"
    }

    strategy = intent_strategies.get(intent, intent_strategies["other"])

    # 安全格式化日期
    def _fmt_date(d) -> str:
        if not d:
            return '未知'
        if isinstance(d, str):
            return d[:10]
        return d.strftime('%Y-%m-%d')

    # 构建患者档案信息（来自数据库真实查询）
    if profile:
        patient_profile = f"""
患者档案（数据库查询结果）：
- 姓名：{real_name}
- 年龄：{real_age or '未知'}岁
- 性别：{real_gender or '未知'}
- 电话：{real_phone or '未知'}
- 就诊次数：{real_visit_count or 0}次
- 过敏史：{', '.join(real_allergies) if real_allergies else '无'}
- 病史：{', '.join(real_history) if real_history else '无'}
- 末次就诊日期：{_fmt_date(real_last_visit)}
- 末次治疗：{patient_info.last_treatment or treatment or '未知'}
- 主治医生：{visit_context.doctor_name or '未知'}
- 回访类型：{visit_context.plan_type}
- 回访计划日期：{_fmt_date(visit_context.scheduled_date)}
"""
    else:
        patient_profile = """
患者档案：数据库查询失败，未找到该患者记录。请如实告诉用户系统暂时查不到数据，建议联系诊所前台确认。
"""

    system_prompt = f"""你是口腔诊所的AI回访助手。你可以根据以下患者档案回答患者的问题。

{patient_profile}

当前意图：{intent}
回复策略：{strategy}

{guidelines_text}
{faqs_text}

回复要求：
1. 语气亲切专业，直接回应患者的问题
2. 如果问就诊时间、治疗方案等，直接用患者档案中的数据回答
3. 引用专业知识时确保准确
4. 如有严重症状（持续出血、剧烈疼痛、高烧等），建议及时就诊
5. 控制在150字以内"""

    chat_messages = [SystemMessage(content=system_prompt)]

    # 添加最近10条历史消息
    for msg in messages[-10:]:
        if msg.role == "user":
            chat_messages.append(HumanMessage(content=msg.content))
        elif msg.role == "assistant":
            chat_messages.append(AIMessage(content=msg.content))

    chat_messages.append(HumanMessage(content=user_input))

    response = get_llm().invoke(chat_messages)
    reply = response.content.strip().strip('"').strip("'")
    print(f"[LLM回复] 意图={intent} | {reply}...")

    # 判断是否需要转人工
    escalate_keywords = ["转人工", "找医生", "找客服", "投诉", "退款", "太差了"]
    medical_emergency = ["出血不止", "疼得受不了", "肿得很厉害", "发烧", "化脓"]
    should_escalate = (
        intent in ("complaint",) or
        any(kw in user_input for kw in escalate_keywords) or
        any(kw in user_input for kw in medical_emergency)
    )

    return {
        "messages": [Message(role="assistant", content=reply)],
        "current_response": reply,
        "should_escalate": should_escalate,
        "collected_info": collected,
        "step": "check_completion"
    }


# ==================== 完成检查节点 ====================

def check_completion_node(state: ReturnVisitState) -> Dict[str, Any]:
    """检查是否应该结束对话"""
    messages = state["messages"]
    should_escalate = state.get("should_escalate", False)
    current_intent = state.get("current_intent", "")

    if should_escalate:
        print("[检查完成] → 转人工")
        return {
            "status": ConversationStatus.ESCALATED,
            "step": "escalate",
            "ended_at": datetime.now()
        }

    message_count = len([m for m in messages if m.role in ("user", "assistant")])
    satisfaction_score = state.get("satisfaction_score")
    print(f"[检查完成] 轮次={message_count} | 满意度={satisfaction_score} | 意图={current_intent}")

    # 用户明确表示结束 → 直接结束（不用收集满意度）
    if current_intent == "end" and message_count >= 2:
        print("[检查完成] 用户表示结束 → 直接结束")
        return {
            "step": "end",
            "status": ConversationStatus.COMPLETED,
            "ended_at": datetime.now()
        }

    # 对话超过8轮且未收集满意度 → 收集满意度
    if message_count >= 8 and satisfaction_score is None:
        print("[检查完成] ≥8轮无满意度 → 收集满意度")
        return {"step": "collect_satisfaction"}

    # 对话超过12轮 → 强制结束
    if message_count >= 12:
        print("[检查完成] ≥12轮 → 强制结束")
        return {
            "step": "end",
            "status": ConversationStatus.COMPLETED,
            "ended_at": datetime.now()
        }

    # 检查用户是否表达了结束意图
    last_user_msgs = [m for m in messages if m.role == "user"]
    if last_user_msgs:
        last_msg = last_user_msgs[-1].content.lower()
        end_keywords = ["谢谢", "好的", "没有了", "没问题", "再见", "拜拜", "行了", "可以了"]
        if any(kw in last_msg for kw in end_keywords) and message_count >= 3:
            print("[检查完成] 检测到结束关键词 → 结束")
            return {
                "step": "end",
                "status": ConversationStatus.COMPLETED,
                "ended_at": datetime.now()
            }

    # 继续对话
    print("[检查完成] → 等待用户回复")
    return {"step": "waiting_for_response"}


# ==================== 满意度收集节点 ====================

def collect_satisfaction_node(state: ReturnVisitState) -> Dict[str, Any]:
    """收集用户满意度评分"""
    user_input = state.get("current_input", "")

    # 尝试从用户输入中提取评分
    score = satisfaction_collector.extract_score(user_input)

    if score is not None:
        if score >= 4:
            closing = "感谢您的认可！祝您身体健康，如有任何问题随时联系我们。"
        elif score >= 3:
            closing = "感谢您的反馈，我们会继续改进服务。祝您身体健康！"
        else:
            closing = "感谢您的反馈，我们会认真改进。如有任何问题，可以随时联系我们。祝您身体健康！"

        return {
            "messages": [Message(role="assistant", content=closing)],
            "current_response": closing,
            "satisfaction_score": score,
            "step": "end",
            "status": ConversationStatus.COMPLETED,
            "ended_at": datetime.now()
        }
    else:
        ask_satisfaction = "感谢您的配合！最后想请您对本次治疗和回访服务打个分（1-5分，5分最满意），您的反馈对我们很重要~"

        return {
            "messages": [Message(role="assistant", content=ask_satisfaction)],
            "current_response": ask_satisfaction,
            "step": "waiting_for_satisfaction"
        }


# ==================== 转人工节点 ====================

def escalate_node(state: ReturnVisitState) -> Dict[str, Any]:
    """转人工节点"""
    patient_name = state["patient_info"].name

    escalate_message = (
        f"{patient_name}您好，我已经记录了您的问题。"
        f"稍后会有专业医生或客服人员主动与您联系，请保持电话畅通。感谢您的耐心！"
    )

    # 通知 Java 后端转人工
    try:
        api_client.save_conversation({
            "conversation_id": state["conversation_id"],
            "patient_id": state["patient_info"].patient_id,
            "plan_id": state["visit_context"].plan_id,
            "status": "escalated",
            "messages": [{"role": m.role, "content": m.content} for m in state["messages"]],
            "escalate_reason": "用户要求转人工或检测到需要人工介入",
            "ended_at": datetime.now().isoformat()
        })
    except Exception as e:
        print(f"通知转人工失败: {e}")

    return {
        "messages": [Message(role="assistant", content=escalate_message)],
        "current_response": escalate_message,
        "status": ConversationStatus.ESCALATED,
        "ended_at": datetime.now()
    }


# ==================== 结束节点 ====================

def end_node(state: ReturnVisitState) -> Dict[str, Any]:
    """结束节点：保存对话记录到后端"""
    try:
        conversation_data = {
            "conversation_id": state["conversation_id"],
            "patient_id": state["patient_info"].patient_id,
            "plan_id": state["visit_context"].plan_id,
            "status": state["status"].value,
            "satisfaction_score": state.get("satisfaction_score"),
            "messages": [
                {
                    "role": m.role,
                    "content": m.content,
                    "timestamp": m.timestamp.isoformat()
                }
                for m in state["messages"]
            ],
            "started_at": state["started_at"].isoformat(),
            "ended_at": state.get("ended_at").isoformat() if state.get("ended_at") else datetime.now().isoformat(),
            "collected_info": state.get("collected_info", {})
        }

        api_client.save_conversation(conversation_data)
    except Exception as e:
        print(f"保存对话记录失败: {e}")

    return {"step": "finished"}


# ==================== 路由函数 ====================

def route_by_step(state: ReturnVisitState) -> str:
    """根据 step 决定路由目标"""
    step = state.get("step", "start")
    END=""
    if step == "start":
        return "greeting_node"
    elif step == "intent_analysis":
        return "intent_analysis_node"
    elif step == "end":
        return "end_node"
    else:
        return END


def route_after_check(state: ReturnVisitState) -> str:
    """check_completion 之后的路由"""
    step = state.get("step", "waiting_for_response")
    return step
