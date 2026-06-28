package com.clinic.controller.ai;

import com.clinic.common.result.Result;
import com.clinic.entity.Patient;
import com.clinic.entity.ReturnVisitPlan;
import com.clinic.mapper.PatientMapper;
import com.clinic.mapper.ReturnVisitPlanMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/**
 * LangGraph AI回访代理控制器（原 Dify 代理，已迁移到 LangGraph）
 * 用于转发小程序端的 AI 回访请求到 LangGraph 服务
 */
@Slf4j
@Api(tags = "AI回访代理（LangGraph）")
@RestController
@RequestMapping("/ai/dify")
@RequiredArgsConstructor
public class DifyProxyController {

    @Value("${langgraph.api-url:http://localhost:8000}")
    private String langgraphApiUrl;

    @Value("${langgraph.api-key:}")
    private String langgraphApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PatientMapper patientMapper;
    private final ReturnVisitPlanMapper returnVisitPlanMapper;

    /**
     * 发送聊天消息（非流式）
     * 前端兼容 Dify 格式，后端转发到 LangGraph 服务
     */
    @ApiOperation("发送聊天消息")
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> requestBody) {
        log.info("LangGraph chat 请求: {}", requestBody);

        try {
            String query = (String) requestBody.get("query");
            String conversationId = (String) requestBody.getOrDefault("conversation_id", "");
            @SuppressWarnings("unchecked")
            Map<String, Object> inputs = (Map<String, Object>) requestBody.getOrDefault("inputs", Map.of());

            // 构建 LangGraph 请求
            Map<String, Object> langgraphRequest = new HashMap<>();
            langgraphRequest.put("query", query);
            langgraphRequest.put("conversation_id", conversationId);
            langgraphRequest.put("user", requestBody.getOrDefault("user", "anonymous"));

            // 如果是新对话，从数据库获取患者和回访信息
            if (conversationId == null || conversationId.isEmpty()) {
                Map<String, Object> patientInfo = buildPatientInfo(inputs);
                Map<String, Object> visitContext = buildVisitContext(inputs);

                Map<String, Object> enrichedInputs = new HashMap<>();
                enrichedInputs.put("patient_info", patientInfo);
                enrichedInputs.put("visit_context", visitContext);
                langgraphRequest.put("inputs", enrichedInputs);
            } else {
                langgraphRequest.put("inputs", inputs);
            }

            // 调用 LangGraph 服务
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", langgraphApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(langgraphRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                langgraphApiUrl + "/fastapi/v1/chat",
                HttpMethod.POST,
                entity,
                String.class
            );

            String responseBody = response.getBody();
            log.info("LangGraph 响应: {}", responseBody != null && responseBody.length() > 300
                ? responseBody.substring(0, 300) + "..." : responseBody);

            // 将 LangGraph 响应转换为 Dify 兼容格式
            if (responseBody != null && !responseBody.isEmpty()) {
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                String answer = jsonNode.has("response") ? jsonNode.get("response").asText() : "";
                String lgConversationId = jsonNode.has("conversation_id") ? jsonNode.get("conversation_id").asText() : "";

                Map<String, Object> result = new HashMap<>();
                result.put("answer", answer);
                result.put("conversation_id", lgConversationId);

                // 附加信息
                if (jsonNode.has("is_completed")) {
                    result.put("is_completed", jsonNode.get("is_completed").asBoolean());
                }
                if (jsonNode.has("satisfaction_score") && !jsonNode.get("satisfaction_score").isNull()) {
                    result.put("satisfaction_score", jsonNode.get("satisfaction_score").asInt());
                }

                return ResponseEntity.ok(Result.success(result));
            }

            return ResponseEntity.ok(Result.success(Map.of(
                "answer", "服务返回空响应",
                "conversation_id", ""
            )));

        } catch (Exception e) {
            log.error("LangGraph chat 请求失败", e);
            return ResponseEntity.ok(Result.error("AI服务暂时不可用，请稍后重试"));
        }
    }

    /**
     * 发送聊天消息（流式 SSE）
     */
    @ApiOperation("发送聊天消息（流式）")
    @PostMapping("/chat-stream")
    public SseEmitter chatStream(@RequestBody Map<String, Object> requestBody) {
        log.info("LangGraph chat-stream 请求: {}", requestBody);

        SseEmitter emitter = new SseEmitter(0L);

        new Thread(() -> {
            try {
                String query = (String) requestBody.get("query");
                String conversationId = (String) requestBody.getOrDefault("conversation_id", "");
                @SuppressWarnings("unchecked")
                Map<String, Object> inputs = (Map<String, Object>) requestBody.getOrDefault("inputs", Map.of());

                Map<String, Object> langgraphRequest = new HashMap<>();
                langgraphRequest.put("query", query);
                langgraphRequest.put("conversation_id", conversationId);
                langgraphRequest.put("user", requestBody.getOrDefault("user", "anonymous"));

                if (conversationId == null || conversationId.isEmpty()) {
                    Map<String, Object> enrichedInputs = new HashMap<>();
                    enrichedInputs.put("patient_info", buildPatientInfo(inputs));
                    enrichedInputs.put("visit_context", buildVisitContext(inputs));
                    langgraphRequest.put("inputs", enrichedInputs);
                } else {
                    langgraphRequest.put("inputs", inputs);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-API-Key", langgraphApiKey);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(langgraphRequest, headers);

                org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                    new org.springframework.http.client.SimpleClientHttpRequestFactory();
                factory.setBufferRequestBody(false);

                RestTemplate streamingRestTemplate = new RestTemplate(factory);

                // 使用 execute + ResponseExtractor 直接读取流，避免类型转换问题
                streamingRestTemplate.execute(
                    langgraphApiUrl + "/fastapi/v1/chat/stream",
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        request.getHeaders().set("X-API-Key", langgraphApiKey);
                        new ObjectMapper().writeValue(request.getBody(), langgraphRequest);
                    },
                    clientHttpResponse -> {
                        try (java.io.InputStream inputStream = clientHttpResponse.getBody();
                             BufferedReader reader = new BufferedReader(
                                 new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(line));
                                }
                            }
                        }
                        emitter.complete();
                        return null;
                    }
                );

            } catch (Exception e) {
                log.error("LangGraph chat-stream 请求失败", e);
                try {
                    emitter.send(SseEmitter.event().data("data: {\"error\": \"" + e.getMessage() + "\"}"));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();

        return emitter;
    }

    /**
     * 获取会话列表
     * 从本地数据库查询（LangGraph 服务不存储历史对话列表）
     */
    @ApiOperation("获取会话列表")
    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(
            @RequestParam String user,
            @RequestParam(defaultValue = "20") Integer limit) {
        log.info("获取会话列表: user={}, limit={}", user);
        // 会话列表由前端自行管理，此处返回空列表保持兼容
        return ResponseEntity.ok(Result.success(Map.of("data", new ArrayList<>())));
    }

    /**
     * 获取会话消息列表
     * 从 LangGraph 服务获取对话历史
     */
    @ApiOperation("获取会话消息列表")
    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(
            @RequestParam String user,
            @RequestParam String conversation_id,
            @RequestParam(defaultValue = "50") Integer limit) {
        log.info("获取消息列表: user={}, conversation_id={}", user, conversation_id);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", langgraphApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                langgraphApiUrl + "/fastapi/v1/conversation/" + conversation_id + "/messages",
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return ResponseEntity.ok(Result.success(jsonNode));
            }

            return ResponseEntity.ok(Result.success(Map.of("data", new ArrayList<>())));

        } catch (Exception e) {
            log.error("获取消息失败", e);
            return ResponseEntity.ok(Result.success(Map.of("data", new ArrayList<>())));
        }
    }

    /**
     * 获取会话历史（兼容接口）
     */
    @ApiOperation("获取会话历史")
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestParam String user,
            @RequestParam(required = false) String conversation_id,
            @RequestParam(defaultValue = "50") Integer limit) {
        if (conversation_id == null || conversation_id.isEmpty()) {
            return ResponseEntity.ok(Result.success(Map.of("data", new ArrayList<>())));
        }
        return getMessages(user, conversation_id, limit);
    }

    // ==================== 私有方法 ====================

    /**
     * 从 inputs 构建患者信息，优先查数据库
     */
    private Map<String, Object> buildPatientInfo(Map<String, Object> inputs) {
        Map<String, Object> info = new HashMap<>();

        Object patientIdObj = inputs.get("patient_id");
        Long patientId = parseLong(patientIdObj);

        if (patientId != null) {
            Patient patient = patientMapper.selectById(patientId);
            if (patient != null) {
                info.put("patient_id", patient.getId());
                info.put("name", patient.getPatientName() != null ? patient.getPatientName() : "");
                info.put("phone", patient.getPhone() != null ? maskPhone(patient.getPhone()) : "");
                info.put("age", patient.getAge() != null ? patient.getAge() : 0);
                info.put("gender", patient.getGender() != null && patient.getGender() == 1 ? "男" : "女");
                info.put("medical_history", patient.getMedicalHistory() != null
                    ? List.of(patient.getMedicalHistory().split("[,，]")) : List.of());
                info.put("allergies", patient.getAllergyHistory() != null
                    ? List.of(patient.getAllergyHistory().split("[,，]")) : List.of());
                info.put("last_visit_date", patient.getLastVisitDate() != null
                    ? patient.getLastVisitDate().toString() : "");
                info.put("last_treatment", "");
                return info;
            }
        }

        // 数据库查不到则用 inputs 中的值
        info.put("patient_id", patientId != null ? patientId : 0);
        info.put("name", inputs.getOrDefault("patient_name", ""));
        info.put("phone", "");
        info.put("age", 0);
        info.put("gender", "");
        info.put("medical_history", List.of());
        info.put("allergies", List.of());
        info.put("last_visit_date", "");
        info.put("last_treatment", "");
        return info;
    }

    /**
     * 从 inputs 构建回访上下文，优先查数据库
     */
    private Map<String, Object> buildVisitContext(Map<String, Object> inputs) {
        Map<String, Object> context = new HashMap<>();

        Object planIdObj = inputs.get("plan_id");
        Long planId = parseLong(planIdObj);

        if (planId != null) {
            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(planId);
            if (plan != null) {
                context.put("plan_id", plan.getId());
                context.put("plan_type", "术后回访");
                context.put("related_treatment", plan.getVisitItem() != null ? plan.getVisitItem() : "");
                context.put("scheduled_date", plan.getPlanDate() != null ? plan.getPlanDate().toString() : "");
                context.put("doctor_name", "");
                context.put("department", "");
                return context;
            }
        }

        // 数据库查不到则用 inputs 中的值
        context.put("plan_id", planId != null ? planId : 0);
        context.put("plan_type", "术后回访");
        context.put("related_treatment", inputs.getOrDefault("visit_item", ""));
        context.put("scheduled_date", LocalDate.now().toString());
        context.put("doctor_name", "");
        context.put("department", "");
        return context;
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        try {
            String str = value.toString();
            if (str.contains(".")) {
                str = str.split("\\.")[0];
            }
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
