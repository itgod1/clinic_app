"""
LangGraph AI 回访工作流

提供基于 LangGraph 的 AI 回访功能，用于替代/增强现有的 Dify 工作流。

主要功能：
- 智能对话管理
- 意图识别
- 知识库问答
- 满意度收集
- 人工转接

使用方法：
    from return_visit_workflow import get_workflow
    
    workflow = get_workflow()
    result = workflow.start_conversation(
        conversation_id="conv-123",
        patient_info=patient_info,
        visit_context=visit_context
    )
"""

from .graph import ReturnVisitWorkflow, get_workflow, start_return_visit, continue_return_visit
from .state import (
    ReturnVisitState,
    PatientInfo,
    VisitContext,
    Message,
    ConversationStatus,
    IntentType,
    create_initial_state
)

__version__ = "1.0.0"
__all__ = [
    "ReturnVisitWorkflow",
    "get_workflow",
    "start_return_visit",
    "continue_return_visit",
    "ReturnVisitState",
    "PatientInfo",
    "VisitContext",
    "Message",
    "ConversationStatus",
    "IntentType",
    "create_initial_state",
]
