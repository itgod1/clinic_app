package com.clinic.controller.ai;

import com.clinic.common.result.Result;
import com.clinic.dto.*;
import com.clinic.service.AiReturnVisitService;
import com.clinic.vo.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "AI智能回访-API接口")
@RestController
@RequestMapping("/ai/return-visit")
@RequiredArgsConstructor
public class AiReturnVisitController {

    private final AiReturnVisitService aiReturnVisitService;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.webhook-secret}")
    private String webhookSecret;

    private boolean validateApiKey(String requestKey) {
        return apiKey.equals(requestKey);
    }

    private boolean validateWebhookSecret(String requestSecret) {
        return webhookSecret.equals(requestSecret);
    }

    /**
     * 解析 patientId，兼容带后缀的格式
     * 例如："1777739727007" → 1777739727007L
     *      "1777739727007.patient_id" → 1777739727007L
     */
    private Long parsePatientId(String patientId) {
        if (patientId == null || patientId.trim().isEmpty()) {
            return null;
        }
        try {
            // 如果包含点号，取点号前的部分
            if (patientId.contains(".")) {
                patientId = patientId.split("\\.")[0];
            }
            return Long.parseLong(patientId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @ApiOperation("获取患者完整档案")
    @GetMapping("/patient/{patientId}/profile")
    public Result<PatientProfileVO> getPatientProfile(
            @PathVariable String patientId,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        Long parsedPatientId = parsePatientId(patientId);
        if (parsedPatientId == null) {
            return Result.badRequest("患者ID格式错误: " + patientId);
        }
        PatientProfileVO profile = aiReturnVisitService.getPatientProfile(parsedPatientId);
        return Result.success(profile);
    }

    @ApiOperation("获取患者就诊历史")
    @GetMapping("/patient/{patientId}/history")
    public Result<PatientVisitHistoryVO> getPatientHistory(
            @PathVariable String patientId,
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        Long parsedPatientId = parsePatientId(patientId);
        if (parsedPatientId == null) {
            return Result.badRequest("患者ID格式错误: " + patientId);
        }
        PatientVisitHistoryVO history = aiReturnVisitService.getPatientHistory(parsedPatientId, limit);
        return Result.success(history);
    }

    @ApiOperation("获取今日AI回访任务列表")
    @GetMapping("/plan/today")
    public Result<List<AiReturnVisitPlanVO>> getTodayPlans(
            @RequestParam Long clinicId,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        List<AiReturnVisitPlanVO> plans = aiReturnVisitService.getTodayPlans(clinicId);
        return Result.success(plans);
    }

    @ApiOperation("获取回访任务详情")
    @GetMapping("/plan/{planId}")
    public Result<AiReturnVisitPlanVO> getPlanDetail(
            @PathVariable Long planId,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        AiReturnVisitPlanVO plan = aiReturnVisitService.getPlanDetail(planId);
        return Result.success(plan);
    }

    @ApiOperation("开始AI回访对话")
    @PostMapping("/conversation/start")
    public Result<AiConversationVO> startConversation(
            @RequestBody AiConversationStartDTO dto,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        AiConversationVO conversation = aiReturnVisitService.startConversation(dto);
        return Result.success(conversation);
    }

    @ApiOperation("提交对话消息")
    @PostMapping("/conversation/{conversationId}/message")
    public Result<Map<String, Object>> saveMessage(
            @PathVariable String conversationId,
            @RequestBody AiConversationMessageDTO dto,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        Map<String, Object> result = aiReturnVisitService.saveMessage(conversationId, dto);
        return Result.success(result);
    }

    @ApiOperation("完成回访对话")
    @PostMapping("/conversation/{conversationId}/complete")
    public Result<Map<String, Object>> completeConversation(
            @PathVariable String conversationId,
            @RequestBody AiConversationCompleteDTO dto,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        Map<String, Object> result = aiReturnVisitService.completeConversation(conversationId, dto);
        return Result.success(result);
    }

    @ApiOperation("保存AI回访记录")
    @PostMapping("/record")
    public Result<Map<String, Object>> saveRecord(
            @RequestBody AiReturnVisitRecordDTO dto,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        Map<String, Object> result = aiReturnVisitService.saveRecord(dto);
        return Result.success(result);
    }

    @ApiOperation("获取AI回访记录列表")
    @GetMapping("/record/list")
    public Result<Map<String, Object>> listRecords(
            @RequestParam(required = false) Long clinicId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        Map<String, Object> result = aiReturnVisitService.listRecords(clinicId, patientId, status, pageNum, pageSize);
        return Result.success(result);
    }

    @ApiOperation("获取回访记录详情")
    @GetMapping("/record/{recordId}")
    public Result<AiReturnVisitRecordVO> getRecordDetail(
            @PathVariable Long recordId,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        AiReturnVisitRecordVO record = aiReturnVisitService.getRecordDetail(recordId);
        return Result.success(record);
    }

    @ApiOperation("AI回访 Webhook回调（兼容Dify/LangGraph）")
    @PostMapping("/webhook")
    public Result<String> handleWebhook(
            @RequestBody AiWebhookDTO dto,
            @RequestHeader("X-Webhook-Secret") String requestWebhookSecret) {
        if (!validateWebhookSecret(requestWebhookSecret)) {
            return Result.error(401, "Webhook Secret 无效");
        }
        aiReturnVisitService.handleWebhook(dto);
        return Result.success("OK");
    }

    @ApiOperation("触发AI回访任务执行")
    @PostMapping("/task/trigger")
    public Result<String> triggerTask(
            @RequestParam Long taskId,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        aiReturnVisitService.triggerTask(taskId);
        return Result.success("任务已触发");
    }

    @ApiOperation("保存LangGraph原生格式对话记录")
    @PostMapping("/conversation/save")
    public Result<Map<String, Object>> saveLangGraphConversation(
            @RequestBody Map<String, Object> langgraphData,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        Map<String, Object> result = aiReturnVisitService.saveLangGraphConversation(langgraphData);
        return Result.success(result);
    }

    @ApiOperation("获取待执行的AI回访任务")
    @GetMapping("/task/pending")
    public Result<List<Map<String, Object>>> getPendingTasks(
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestHeader("X-API-Key") String requestApiKey) {
        if (!validateApiKey(requestApiKey)) {
            return Result.error(401, "API Key 无效");
        }
        List<Map<String, Object>> tasks = aiReturnVisitService.getPendingTasks(limit);
        return Result.success(tasks);
    }
}
