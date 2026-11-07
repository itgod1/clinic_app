"""
LangGraph 工作流定义
构建 AI 回访的完整工作流图
"""
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from typing import Dict, Any

from state import ReturnVisitState, create_initial_state, PatientInfo, VisitContext
from nodes import (
    router_node,
    greeting_node,
    intent_analysis_node,
    generate_response_node,
    check_completion_node,
    collect_satisfaction_node,
    escalate_node,
    end_node,
    route_by_step,
    route_after_check
)


def create_return_visit_graph() -> StateGraph:
    """
    创建 AI 回访工作流图

    工作流流程：
    1. router_node: 根据 step 路由到 greeting 或 intent_analysis
    2. greeting_node: 生成问候语 → END（返回用户）
    3. intent_analysis_node → generate_response_node → check_completion_node
    4. check_completion_node → waiting_for_response(END) / collect_satisfaction / escalate / end
    """
    workflow = StateGraph(ReturnVisitState)

    # 注册节点
    workflow.add_node("router_node", router_node)
    workflow.add_node("greeting_node", greeting_node)
    workflow.add_node("intent_analysis_node", intent_analysis_node)
    workflow.add_node("generate_response_node", generate_response_node)
    workflow.add_node("check_completion_node", check_completion_node)
    workflow.add_node("collect_satisfaction_node", collect_satisfaction_node)
    workflow.add_node("escalate_node", escalate_node)
    workflow.add_node("end_node", end_node)

    # 入口：router_node
    workflow.set_entry_point("router_node")

    # router_node → 条件路由
    workflow.add_conditional_edges(
        "router_node",
        route_by_step,
        {
            "greeting_node": "greeting_node",
            "intent_analysis_node": "intent_analysis_node",
            "end_node": "end_node",
            END: END
        }
    )

    # greeting_node → END（返回问候语给用户，等待下一轮）
    workflow.add_edge("greeting_node", END)

    # intent_analysis → generate_response → check_completion
    workflow.add_edge("intent_analysis_node", "generate_response_node")
    workflow.add_edge("generate_response_node", "check_completion_node")

    # check_completion_node → 条件路由
    workflow.add_conditional_edges(
        "check_completion_node",
        route_after_check,
        {
            "waiting_for_response": END,
            "waiting_for_satisfaction": "collect_satisfaction_node",
            "collect_satisfaction": "collect_satisfaction_node",
            "escalate": "escalate_node",
            "end": "end_node"
        }
    )

    # collect_satisfaction → end_node
    workflow.add_edge("collect_satisfaction_node", "end_node")

    # escalate_node → end_node
    workflow.add_edge("escalate_node", "end_node")

    # end_node → END
    workflow.add_edge("end_node", END)

    return workflow


def create_workflow_with_memory() -> StateGraph:
    """创建带记忆功能的工作流（多轮对话状态持久化）"""
    workflow = create_return_visit_graph()
    memory = MemorySaver()
    return workflow.compile(checkpointer=memory)


class ReturnVisitWorkflow:
    """AI 回访工作流管理器"""

    def __init__(self):
        self.app = create_workflow_with_memory()

    def start_conversation(
        self,
        conversation_id: str,
        patient_info: PatientInfo,
        visit_context: VisitContext
    ) -> Dict[str, Any]:
        """开始新回访对话，生成问候语"""
        initial_state = create_initial_state(
            conversation_id=conversation_id,
            patient_info=patient_info,
            visit_context=visit_context
        )
        initial_state["step"] = "start"

        config = {"configurable": {"thread_id": conversation_id}}
        result = self.app.invoke(initial_state, config)

        return {
            "conversation_id": conversation_id,
            "status": result["status"].value,
            "response": result.get("current_response", ""),
            "messages": [
                {"role": m.role, "content": m.content}
                for m in result["messages"]
            ]
        }

    def continue_conversation(
        self,
        conversation_id: str,
        user_input: str
    ) -> Dict[str, Any]:
        """继续对话，处理用户输入并生成回复"""
        config = {"configurable": {"thread_id": conversation_id}}

        state_update = {
            "current_input": user_input,
            "step": "intent_analysis"
        }

        result = self.app.invoke(state_update, config)
        status_value = result["status"].value

        return {
            "conversation_id": conversation_id,
            "status": status_value,
            "response": result.get("current_response", ""),
            "should_escalate": result.get("should_escalate", False),
            "is_completed": status_value in ("completed", "escalated", "failed"),
            "satisfaction_score": result.get("satisfaction_score"),
            "messages": [
                {"role": m.role, "content": m.content}
                for m in result["messages"][-2:]
            ]
        }

    def get_conversation_state(self, conversation_id: str) -> Dict[str, Any]:
        """获取对话状态（从 checkpoint 读取）"""
        config = {"configurable": {"thread_id": conversation_id}}

        try:
            state = self.app.get_state(config)
            if state.values:
                return {
                    "conversation_id": conversation_id,
                    "status": str(state.values.get("status", "")),
                    "step": state.values.get("step"),
                    "message_count": len(state.values.get("messages", [])),
                    "satisfaction_score": state.values.get("satisfaction_score")
                }
            return {"conversation_id": conversation_id, "error": "对话不存在"}
        except Exception as e:
            return {"conversation_id": conversation_id, "error": str(e)}


# 全局单例
_workflow_instance = None


def get_workflow() -> ReturnVisitWorkflow:
    """获取工作流实例（单例模式）"""
    global _workflow_instance
    if _workflow_instance is None:
        _workflow_instance = ReturnVisitWorkflow()
    return _workflow_instance


# 便捷函数
def start_return_visit(
    conversation_id: str,
    patient_info: Dict[str, Any],
    visit_context: Dict[str, Any]
) -> Dict[str, Any]:
    """便捷函数：开始回访"""
    from datetime import datetime

    patient = PatientInfo(
        patient_id=patient_info["patient_id"],
        name=patient_info.get("name", patient_info.get("patient_name", "")),
        phone=patient_info.get("phone"),
        age=patient_info.get("age"),
        gender=patient_info.get("gender"),
        medical_history=patient_info.get("medical_history", []),
        allergies=patient_info.get("allergies", []),
        last_visit_date=datetime.fromisoformat(patient_info["last_visit_date"]) if patient_info.get("last_visit_date") else None,
        last_treatment=patient_info.get("last_treatment")
    )

    context = VisitContext(
        plan_id=visit_context["plan_id"],
        plan_type=visit_context.get("plan_type", "术后回访"),
        related_treatment=visit_context.get("related_treatment"),
        scheduled_date=datetime.fromisoformat(visit_context["scheduled_date"]) if visit_context.get("scheduled_date") else None,
        doctor_name=visit_context.get("doctor_name"),
        department=visit_context.get("department")
    )

    workflow = get_workflow()
    return workflow.start_conversation(conversation_id, patient, context)


def continue_return_visit(conversation_id: str, user_input: str) -> Dict[str, Any]:
    """便捷函数：继续回访"""
    workflow = get_workflow()
    return workflow.continue_conversation(conversation_id, user_input)
