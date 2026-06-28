package com.clinic.service;

import com.clinic.dto.*;
import com.clinic.vo.*;

import java.util.List;
import java.util.Map;

public interface AiReturnVisitService {

    PatientProfileVO getPatientProfile(Long patientId);

    PatientVisitHistoryVO getPatientHistory(Long patientId, Integer limit);

    List<AiReturnVisitPlanVO> getTodayPlans(Long clinicId);

    AiReturnVisitPlanVO getPlanDetail(Long planId);

    AiConversationVO startConversation(AiConversationStartDTO dto);

    Map<String, Object> saveMessage(String conversationId, AiConversationMessageDTO dto);

    Map<String, Object> completeConversation(String conversationId, AiConversationCompleteDTO dto);

    Map<String, Object> saveRecord(AiReturnVisitRecordDTO dto);

    Map<String, Object> listRecords(Long clinicId, Long patientId, String status, Integer pageNum, Integer pageSize);

    AiReturnVisitRecordVO getRecordDetail(Long recordId);

    void handleWebhook(AiWebhookDTO dto);

    void triggerTask(Long taskId);

    List<Map<String, Object>> getPendingTasks(Integer limit);

    /**
     * 创建AI回访任务
     * @param planId 回访计划ID
     * @param patientId 患者ID
     * @return 任务ID
     */
    Long createTask(Long planId, Long patientId);

    /**
     * 记录患者打开回访页面
     * @param taskId 任务ID
     */
    void recordTaskOpen(Long taskId);

    /**
     * 根据患者ID查询回访计划
     * @param patientId 患者ID
     * @return 回访计划
     */
    AiReturnVisitPlanVO getPlanByPatientId(Long patientId);

    /**
     * 保存 LangGraph 原生格式的对话记录
     * 直接接收 LangGraph 工作流状态，自动映射到实体字段
     * @param langgraphData LangGraph 状态数据
     * @return 保存结果
     */
    Map<String, Object> saveLangGraphConversation(Map<String, Object> langgraphData);
}
