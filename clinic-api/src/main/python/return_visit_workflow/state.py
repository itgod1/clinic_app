"""
LangGraph 状态定义
定义 AI 回访工作流的状态结构
"""
from typing import Annotated, List, Optional, Dict, Any
from typing_extensions import TypedDict
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
import operator


class ConversationStatus(str, Enum):
    """对话状态"""
    PENDING = "pending"          # 待开始
    IN_PROGRESS = "in_progress"  # 进行中
    COMPLETED = "completed"      # 已完成
    FAILED = "failed"            # 失败
    ESCALATED = "escalated"      # 已转人工


class IntentType(str, Enum):
    """用户意图类型"""
    GREETING = "greeting"        # 问候
    INQUIRY = "inquiry"          # 咨询
    COMPLAINT = "complaint"      # 投诉
    SATISFIED = "satisfied"      # 满意
    DISSATISFIED = "dissatisfied" # 不满意
    APPOINTMENT = "appointment"  # 预约
    MEDICAL = "medical"          # 医疗问题
    OTHER = "other"              # 其他


@dataclass
class Message:
    """对话消息"""
    role: str                    # user / assistant / system
    content: str
    timestamp: datetime = field(default_factory=datetime.now)
    metadata: Dict[str, Any] = field(default_factory=dict)


@dataclass
class PatientInfo:
    """患者信息"""
    patient_id: int
    name: str
    phone: Optional[str] = None
    age: Optional[int] = None
    gender: Optional[str] = None
    medical_history: List[str] = field(default_factory=list)
    allergies: List[str] = field(default_factory=list)
    last_visit_date: Optional[datetime] = None
    last_treatment: Optional[str] = None


@dataclass
class VisitContext:
    """回访上下文"""
    plan_id: int
    plan_type: str                    # 术后回访 / 定期回访 / 满意度调查
    related_treatment: Optional[str] = None
    scheduled_date: Optional[datetime] = None
    doctor_name: Optional[str] = None
    department: Optional[str] = None


class ReturnVisitState(TypedDict):
    """
    LangGraph 状态定义
    使用 TypedDict 以便 LangGraph 进行状态管理
    """
    # 基础信息
    conversation_id: str
    status: ConversationStatus
    
    # 患者和回访信息
    patient_info: PatientInfo
    visit_context: VisitContext
    
    # 对话历史 - 使用 Annotated 和 operator.add 支持追加操作
    messages: Annotated[List[Message], operator.add]
    
    # 当前轮次信息
    current_input: Optional[str]          # 当前用户输入
    current_intent: Optional[IntentType]  # 当前意图
    current_response: Optional[str]       # 当前回复
    
    # 工作流控制
    step: str                             # 当前步骤
    next_step: Optional[str]              # 下一步
    should_escalate: bool                 # 是否转人工
    
    # 收集的信息
    collected_info: Dict[str, Any]        # 收集到的信息
    satisfaction_score: Optional[int]     # 满意度评分
    
    # 错误和重试
    error_count: int
    error_message: Optional[str]
    retry_count: int
    
    # 元数据
    started_at: datetime
    ended_at: Optional[datetime]
    metadata: Dict[str, Any]


def create_initial_state(
    conversation_id: str,
    patient_info: PatientInfo,
    visit_context: VisitContext
) -> ReturnVisitState:
    """
    创建初始状态
    
    Args:
        conversation_id: 对话ID
        patient_info: 患者信息
        visit_context: 回访上下文
    
    Returns:
        初始状态对象
    """
    return ReturnVisitState(
        conversation_id=conversation_id,
        status=ConversationStatus.PENDING,
        patient_info=patient_info,
        visit_context=visit_context,
        messages=[],
        current_input=None,
        current_intent=None,
        current_response=None,
        step="start",
        next_step=None,
        should_escalate=False,
        collected_info={},
        satisfaction_score=None,
        error_count=0,
        error_message=None,
        retry_count=0,
        started_at=datetime.now(),
        ended_at=None,
        metadata={}
    )
