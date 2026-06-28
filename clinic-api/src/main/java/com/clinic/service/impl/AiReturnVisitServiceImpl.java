package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.dto.*;
import com.clinic.entity.*;
import com.clinic.mapper.*;
import com.clinic.service.AiReturnVisitService;
import com.clinic.vo.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReturnVisitServiceImpl implements AiReturnVisitService {

    private final PatientMapper patientMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final RegistrationMapper registrationMapper;
    private final ReturnVisitPlanMapper returnVisitPlanMapper;
    private final AiReturnVisitConversationMapper conversationMapper;
    private final AiReturnVisitTaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PatientProfileVO getPatientProfile(Long patientId) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            return null;
        }

        PatientProfileVO vo = new PatientProfileVO();
        vo.setPatientId(patient.getId());
        vo.setPatientName(patient.getPatientName());
        vo.setGender(patient.getGender() != null && patient.getGender() == 1 ? "MALE" : "FEMALE");
        vo.setGenderText(patient.getGender() != null && patient.getGender() == 1 ? "男" : "女");
        vo.setAge(patient.getAge());
        vo.setPhone(maskPhone(patient.getPhone()));
        vo.setMemberLevel(getMemberLevelCode(patient.getMemberLevel()));
        vo.setMemberLevelText(getMemberLevelName(patient.getMemberLevel()));
        vo.setVisitCount(patient.getVisitCount());
        vo.setLastVisitDate(patient.getLastVisitDate());
        vo.setAllergyHistory(patient.getAllergyHistory());
        vo.setMedicalHistory(patient.getMedicalHistory());
        vo.setAddress(patient.getAddress());

        return vo;
    }

    @Override
    public PatientVisitHistoryVO getPatientHistory(Long patientId, Integer limit) {
        PatientVisitHistoryVO result = new PatientVisitHistoryVO();
        result.setPatientId(patientId);

        LambdaQueryWrapper<MedicalRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalRecord::getPatientId, patientId);
        wrapper.orderByDesc(MedicalRecord::getVisitDate);

        List<MedicalRecord> records = medicalRecordMapper.selectList(wrapper);
        result.setTotal(records.size());

        List<PatientVisitHistoryVO.VisitRecordVO> list = records.stream()
                .limit(limit)
                .map(record -> {
                    PatientVisitHistoryVO.VisitRecordVO vo = new PatientVisitHistoryVO.VisitRecordVO();
                    vo.setRegistrationId(record.getRegistrationId());
                    vo.setMedicalRecordId(record.getId());
                    vo.setVisitDate(record.getVisitDate());
                    vo.setDoctorName(record.getDoctorName());
                    vo.setDeptName(record.getDeptName());
                    vo.setChiefComplaint(record.getChiefComplaint());
                    vo.setDiagnosis(record.getDiagnosis());
                    vo.setTreatment(record.getTreatment());
                    vo.setMedicalAdvice(record.getMedicalAdvice());
                    vo.setVisitType(getVisitTypeName(record.getVisitType()));
                    return vo;
                })
                .collect(Collectors.toList());

        result.setRecords(list);
        return result;
    }

    @Override
    public List<AiReturnVisitPlanVO> getTodayPlans(Long clinicId) {
        LambdaQueryWrapper<ReturnVisitPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitPlan::getClinicId, clinicId);
        wrapper.eq(ReturnVisitPlan::getPlanDate, LocalDate.now());
        wrapper.in(ReturnVisitPlan::getStatus, Arrays.asList("PENDING", "AI_ASSIGNED"));
        wrapper.orderByAsc(ReturnVisitPlan::getPriority);

        List<ReturnVisitPlan> plans = returnVisitPlanMapper.selectList(wrapper);

        return plans.stream().map(plan -> {
            AiReturnVisitPlanVO vo = new AiReturnVisitPlanVO();
            vo.setPlanId(plan.getId());
            vo.setPatientId(plan.getPatientId());
            vo.setPatientName(plan.getPatientName());
            vo.setPatientPhone(maskPhone(plan.getPatientPhone()));
            vo.setPlanDate(plan.getPlanDate());
            vo.setPlanTime(plan.getPlanTime());
            vo.setVisitType(plan.getVisitType());
            vo.setVisitItem(plan.getVisitItem());
            vo.setPriority(plan.getPriority());
            vo.setStatus(plan.getStatus());
            vo.setTemplateContent(plan.getContentTemplate());

            Map<String, Object> context = new HashMap<>();
            context.put("lastVisitDate", plan.getPlanDate().minusDays(7));
            vo.setContext(context);

            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public AiReturnVisitPlanVO getPlanDetail(Long planId) {
        ReturnVisitPlan plan = returnVisitPlanMapper.selectById(planId);
        if (plan == null) {
            return null;
        }

        AiReturnVisitPlanVO vo = new AiReturnVisitPlanVO();
        vo.setPlanId(plan.getId());
        vo.setPatientId(plan.getPatientId());
        vo.setPatientName(plan.getPatientName());
        vo.setPatientPhone(maskPhone(plan.getPatientPhone()));
        vo.setPlanDate(plan.getPlanDate());
        vo.setPlanTime(plan.getPlanTime());
        vo.setVisitType(plan.getVisitType());
        vo.setVisitItem(plan.getVisitItem());
        vo.setPriority(plan.getPriority());
        vo.setStatus(plan.getStatus());
        vo.setTemplateContent(plan.getContentTemplate());

        // 获取更详细的上下文
        Map<String, Object> context = new HashMap<>();

        // 获取患者信息
        Patient patient = patientMapper.selectById(plan.getPatientId());
        if (patient != null) {
            context.put("patientGender", patient.getGender() != null && patient.getGender() == 1 ? "男" : "女");
            context.put("patientAge", patient.getAge());
            context.put("allergyHistory", patient.getAllergyHistory());
            context.put("medicalHistory", patient.getMedicalHistory());
        }

        // 获取病历信息
        if (plan.getMedicalRecordId() != null) {
            MedicalRecord record = medicalRecordMapper.selectById(plan.getMedicalRecordId());
            if (record != null) {
                context.put("diagnosis", record.getDiagnosis());
                context.put("treatment", record.getTreatment());
                context.put("chiefComplaint", record.getChiefComplaint());
                context.put("doctorName", record.getDoctorName());
                context.put("visitDate", record.getVisitDate());
            }
        }

        vo.setContext(context);
        return vo;
    }

    @Override
    @Transactional
    public AiConversationVO startConversation(AiConversationStartDTO dto) {
        String conversationId = generateConversationId();

        AiReturnVisitConversation conversation = new AiReturnVisitConversation();
        conversation.setConversationId(conversationId);
        conversation.setPlanId(dto.getPlanId());
        conversation.setPatientId(dto.getPatientId());
        conversation.setDifyConversationId(dto.getDifyConversationId()); // 保留兼容，实际使用 conversationId
        conversation.setChannel(dto.getChannel());
        conversation.setStatus("IN_PROGRESS");
        conversation.setStartTime(LocalDateTime.now());
        conversation.setMessageCount(0);

        conversationMapper.insert(conversation);

        // 更新回访计划状态
        if (dto.getPlanId() != null) {
            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(dto.getPlanId());
            if (plan != null) {
                plan.setStatus("AI_IN_PROGRESS");
                returnVisitPlanMapper.updateById(plan);
            }
        }

        // 生成问候语
        Patient patient = patientMapper.selectById(dto.getPatientId());
        String greeting = generateGreeting(patient);

        AiConversationVO vo = new AiConversationVO();
        vo.setConversationId(conversationId);
        vo.setPlanId(dto.getPlanId());
        vo.setPatientId(dto.getPatientId());
        vo.setStatus("IN_PROGRESS");
        vo.setStartTime(conversation.getStartTime());
        vo.setGreetingMessage(greeting);

        return vo;
    }

    @Override
    public Map<String, Object> saveMessage(String conversationId, AiConversationMessageDTO dto) {
        // 更新对话的消息数
        LambdaQueryWrapper<AiReturnVisitConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiReturnVisitConversation::getConversationId, conversationId);
        AiReturnVisitConversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation != null) {
            conversation.setMessageCount(conversation.getMessageCount() + 1);
            conversationMapper.updateById(conversation);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", UUID.randomUUID().toString());
        result.put("conversationId", conversationId);
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> completeConversation(String conversationId, AiConversationCompleteDTO dto) {
        LambdaQueryWrapper<AiReturnVisitConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiReturnVisitConversation::getConversationId, conversationId);
        AiReturnVisitConversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            throw new RuntimeException("对话不存在");
        }

        LocalDateTime endTime = LocalDateTime.now();
        conversation.setEndTime(endTime);
        conversation.setStatus("COMPLETED");
        conversation.setSummary(dto.getSummary());
        conversation.setPatientStatus(dto.getPatientStatus());
        conversation.setSatisfaction(dto.getSatisfaction());
        conversation.setNeedsFollowUp(dto.getNeedsFollowUp());
        conversation.setFollowUpReason(dto.getFollowUpReason());

        if (conversation.getStartTime() != null) {
            long duration = java.time.Duration.between(conversation.getStartTime(), endTime).getSeconds();
            conversation.setDurationSeconds((int) duration);
        }

        if (dto.getFullDialogue() != null) {
            try {
                conversation.setFullDialogue(objectMapper.writeValueAsString(dto.getFullDialogue()));
            } catch (JsonProcessingException e) {
                log.error("JSON序列化失败", e);
            }
        }

        conversationMapper.updateById(conversation);

        // 更新回访计划状态
        if (conversation.getPlanId() != null) {
            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(conversation.getPlanId());
            if (plan != null) {
                plan.setStatus("AI_COMPLETED");
                returnVisitPlanMapper.updateById(plan);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversationId);
        result.put("status", "COMPLETED");
        result.put("completeTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        result.put("recordId", conversation.getId());

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> saveRecord(AiReturnVisitRecordDTO dto) {
        AiReturnVisitConversation conversation = new AiReturnVisitConversation();
        conversation.setConversationId(dto.getConversationId());
        conversation.setPlanId(dto.getPlanId());
        conversation.setPatientId(dto.getPatientId());
        conversation.setChannel(dto.getChannel());
        conversation.setStatus("COMPLETED");
        conversation.setSummary(dto.getSummary());
        conversation.setSatisfaction(dto.getSatisfaction());
        conversation.setStartTime(LocalDateTime.now());
        conversation.setEndTime(LocalDateTime.now());

        if (dto.getFullDialogue() != null) {
            conversation.setFullDialogue(dto.getFullDialogue());
        }

        if (dto.getExtractedInfo() != null) {
            try {
                conversation.setExtractedInfo(objectMapper.writeValueAsString(dto.getExtractedInfo()));
            } catch (JsonProcessingException e) {
                log.error("JSON序列化失败", e);
            }
        }

        conversationMapper.insert(conversation);

        // 更新回访计划状态
        if (dto.getPlanId() != null) {
            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(dto.getPlanId());
            if (plan != null) {
                plan.setStatus("COMPLETED");
                returnVisitPlanMapper.updateById(plan);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", conversation.getId());
        result.put("planStatus", "COMPLETED");

        return result;
    }

    @Override
    public Map<String, Object> listRecords(Long clinicId, Long patientId, String status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<AiReturnVisitConversation> wrapper = new LambdaQueryWrapper<>();

        if (patientId != null) {
            wrapper.eq(AiReturnVisitConversation::getPatientId, patientId);
        }

        if (status != null) {
            wrapper.eq(AiReturnVisitConversation::getStatus, status);
        }

        wrapper.orderByDesc(AiReturnVisitConversation::getCreatedAt);

        IPage<AiReturnVisitConversation> page = conversationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<AiReturnVisitRecordVO> records = page.getRecords().stream().map(conv -> {
            AiReturnVisitRecordVO vo = new AiReturnVisitRecordVO();
            vo.setRecordId(conv.getId());
            vo.setConversationId(conv.getConversationId());
            vo.setPlanId(conv.getPlanId());
            vo.setPatientId(conv.getPatientId());
            vo.setChannel(conv.getChannel());
            vo.setSummary(conv.getSummary());
            vo.setSatisfaction(conv.getSatisfaction());
            vo.setPatientStatus(conv.getPatientStatus());
            vo.setNeedsFollowUp(conv.getNeedsFollowUp());
            vo.setFollowUpReason(conv.getFollowUpReason());
            vo.setStartTime(conv.getStartTime());
            vo.setEndTime(conv.getEndTime());
            vo.setDurationSeconds(conv.getDurationSeconds());

            // 获取患者姓名
            Patient patient = patientMapper.selectById(conv.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getPatientName());
            }

            return vo;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());

        return result;
    }

    @Override
    public AiReturnVisitRecordVO getRecordDetail(Long recordId) {
        AiReturnVisitConversation conv = conversationMapper.selectById(recordId);
        if (conv == null) {
            return null;
        }

        AiReturnVisitRecordVO vo = new AiReturnVisitRecordVO();
        vo.setRecordId(conv.getId());
        vo.setConversationId(conv.getConversationId());
        vo.setPlanId(conv.getPlanId());
        vo.setPatientId(conv.getPatientId());
        vo.setChannel(conv.getChannel());
        vo.setSummary(conv.getSummary());
        vo.setSatisfaction(conv.getSatisfaction());
        vo.setPatientStatus(conv.getPatientStatus());
        vo.setNeedsFollowUp(conv.getNeedsFollowUp());
        vo.setFollowUpReason(conv.getFollowUpReason());
        vo.setStartTime(conv.getStartTime());
        vo.setEndTime(conv.getEndTime());
        vo.setDurationSeconds(conv.getDurationSeconds());

        Patient patient = patientMapper.selectById(conv.getPatientId());
        if (patient != null) {
            vo.setPatientName(patient.getPatientName());
        }

        return vo;
    }

    @Override
    public void handleWebhook(AiWebhookDTO dto) {
        // 处理 AI 回访完成回调（兼容 Dify 和 LangGraph）
        if ("conversation.completed".equals(dto.getEvent())) {
            Map<String, Object> data = dto.getData();
            if (data != null) {
                String conversationId = (String) data.getOrDefault("conversation_id",
                    data.get("conversationId"));
                Map<String, Object> outputs = (Map<String, Object>) data.get("outputs");

                if (conversationId != null) {
                    LambdaQueryWrapper<AiReturnVisitConversation> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(AiReturnVisitConversation::getConversationId, conversationId);
                    AiReturnVisitConversation conversation = conversationMapper.selectOne(wrapper);

                    // 如果 conversationId 找不到，尝试用 difyConversationId 查找
                    if (conversation == null) {
                        wrapper = new LambdaQueryWrapper<>();
                        wrapper.eq(AiReturnVisitConversation::getDifyConversationId, conversationId);
                        conversation = conversationMapper.selectOne(wrapper);
                    }

                    if (conversation != null) {
                        conversation.setStatus("COMPLETED");
                        if (outputs != null) {
                            if (outputs.get("summary") != null) {
                                conversation.setSummary((String) outputs.get("summary"));
                            }
                            if (outputs.get("satisfaction") != null) {
                                conversation.setSatisfaction((Integer) outputs.get("satisfaction"));
                            }
                            if (outputs.get("needs_follow_up") != null) {
                                conversation.setNeedsFollowUp((Boolean) outputs.get("needs_follow_up"));
                            }
                            if (outputs.get("patient_status") != null) {
                                conversation.setPatientStatus((String) outputs.get("patient_status"));
                            }
                        }
                        conversation.setEndTime(LocalDateTime.now());
                        conversationMapper.updateById(conversation);

                        // 更新回访计划状态
                        if (conversation.getPlanId() != null) {
                            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(conversation.getPlanId());
                            if (plan != null) {
                                plan.setStatus("AI_COMPLETED");
                                returnVisitPlanMapper.updateById(plan);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void triggerTask(Long taskId) {
        AiReturnVisitTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        task.setStatus("PROCESSING");
        taskMapper.updateById(task);

        // 通过 LangGraph 服务开始对话
        try {
            // 获取患者和计划信息
            Patient patient = patientMapper.selectById(task.getPatientId());
            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(task.getPlanId());

            if (patient != null && plan != null) {
                // 构建请求并调用 LangGraph 服务
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("query", "开始回访");
                requestBody.put("conversation_id", "");
                requestBody.put("user", patient.getPatientName());

                Map<String, Object> inputs = new HashMap<>();
                inputs.put("patient_id", patient.getId());
                inputs.put("plan_id", plan.getId());
                requestBody.put("inputs", inputs);

                // 通过 DifyProxyController 的 chat 接口间接调用 LangGraph
                // 这里直接构建 HTTP 请求到 LangGraph 服务
                org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

                org.springframework.http.HttpEntity<Map<String, Object>> entity =
                    new org.springframework.http.HttpEntity<>(requestBody, headers);

                rt.exchange(
                    "http://localhost:8000/fastapi/v1/chat",
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    String.class
                );

                task.setStatus("COMPLETED");
                taskMapper.updateById(task);
            }
        } catch (Exception e) {
            log.error("调用 LangGraph 服务失败, taskId: {}", taskId, e);
            task.setStatus("FAILED");
            taskMapper.updateById(task);
        }
    }

    @Override
    public List<Map<String, Object>> getPendingTasks(Integer limit) {
        LambdaQueryWrapper<AiReturnVisitTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiReturnVisitTask::getStatus, "PENDING");
        wrapper.le(AiReturnVisitTask::getExecuteTime, LocalDateTime.now());
        wrapper.orderByAsc(AiReturnVisitTask::getPriority);
        wrapper.last("LIMIT " + limit);

        List<AiReturnVisitTask> tasks = taskMapper.selectList(wrapper);

        return tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("planId", task.getPlanId());
            map.put("patientId", task.getPatientId());
            map.put("priority", task.getPriority());
            map.put("executeTime", task.getExecuteTime());

            // 获取患者信息
            Patient patient = patientMapper.selectById(task.getPatientId());
            if (patient != null) {
                map.put("patientName", patient.getPatientName());
                map.put("patientPhone", patient.getPhone());
            }

            // 获取计划信息
            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(task.getPlanId());
            if (plan != null) {
                map.put("visitItem", plan.getVisitItem());
            }

            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Long createTask(Long planId, Long patientId) {
        AiReturnVisitTask task = new AiReturnVisitTask();
        task.setPlanId(planId);
        task.setPatientId(patientId);
        task.setTaskType("SCHEDULED");
        task.setExecuteTime(LocalDateTime.now());
        task.setPriority(0);
        task.setStatus("PENDING");
        task.setRetryCount(0);

        taskMapper.insert(task);

        // 更新回访计划状态
        ReturnVisitPlan plan = returnVisitPlanMapper.selectById(planId);
        if (plan != null) {
            plan.setStatus("AI_ASSIGNED");
            returnVisitPlanMapper.updateById(plan);
        }

        return task.getId();
    }

    @Override
    @Transactional
    public void recordTaskOpen(Long taskId) {
        AiReturnVisitTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            return;
        }
        
        // 更新任务状态为已打开
        task.setStatus("OPENED");
        taskMapper.updateById(task);
        
        log.info("患者已打开回访页面, taskId: {}", taskId);
    }

    @Override
    public AiReturnVisitPlanVO getPlanByPatientId(Long patientId) {
        // 查询该患者最新的待回访计划
        LambdaQueryWrapper<ReturnVisitPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitPlan::getPatientId, patientId);
        wrapper.in(ReturnVisitPlan::getStatus, Arrays.asList("PENDING", "AI_ASSIGNED", "AI_IN_PROGRESS"));
        wrapper.orderByDesc(ReturnVisitPlan::getPlanDate);
        wrapper.last("LIMIT 1");
        
        ReturnVisitPlan plan = returnVisitPlanMapper.selectOne(wrapper);
        if (plan == null) {
            return null;
        }
        
        AiReturnVisitPlanVO vo = new AiReturnVisitPlanVO();
        vo.setPlanId(plan.getId());
        vo.setPatientId(plan.getPatientId());
        vo.setPatientName(plan.getPatientName());
        vo.setPatientPhone(maskPhone(plan.getPatientPhone()));
        vo.setPlanDate(plan.getPlanDate());
        vo.setPlanTime(plan.getPlanTime());
        vo.setVisitType(plan.getVisitType());
        vo.setVisitItem(plan.getVisitItem());
        vo.setPriority(plan.getPriority());
        vo.setStatus(plan.getStatus());
        vo.setTemplateContent(plan.getContentTemplate());
        
        return vo;
    }

    @Override
    @Transactional
    public Map<String, Object> saveLangGraphConversation(Map<String, Object> data) {
        String conversationId = (String) data.get("conversation_id");
        if (conversationId == null || conversationId.isEmpty()) {
            throw new RuntimeException("conversation_id 不能为空");
        }

        // 查找已有记录
        LambdaQueryWrapper<AiReturnVisitConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiReturnVisitConversation::getConversationId, conversationId);
        AiReturnVisitConversation conversation = conversationMapper.selectOne(wrapper);

        boolean isNew = (conversation == null);
        if (isNew) {
            conversation = new AiReturnVisitConversation();
            conversation.setConversationId(conversationId);
            conversation.setStartTime(LocalDateTime.now());
            conversation.setStatus("IN_PROGRESS");
            conversation.setMessageCount(0);
        }

        // 映射 LangGraph 字段
        if (data.get("patient_id") != null) {
            conversation.setPatientId(toLong(data.get("patient_id")));
        }
        if (data.get("plan_id") != null) {
            conversation.setPlanId(toLong(data.get("plan_id")));
        }
        if (data.get("status") != null) {
            conversation.setStatus(((String) data.get("status")).toUpperCase());
        }
        if (data.get("satisfaction_score") != null) {
            conversation.setSatisfaction(toInteger(data.get("satisfaction_score")));
        }

        // 处理消息 → 生成摘要 + 保存完整对话
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) data.get("messages");
        if (messages != null && !messages.isEmpty()) {
            // 生成摘要
            StringBuilder summary = new StringBuilder();
            for (Map<String, Object> msg : messages) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");
                if (content != null) {
                    summary.append(role.equals("user") ? "患者: " : "AI: ")
                           .append(content).append("\n");
                }
            }
            conversation.setSummary(summary.toString().trim());

            // 消息数
            conversation.setMessageCount(messages.size());

            // 序列化完整对话
            try {
                conversation.setFullDialogue(objectMapper.writeValueAsString(messages));
            } catch (JsonProcessingException e) {
                log.error("序列化消息失败", e);
            }
        }

        // escalated 特殊处理
        if ("ESCALATED".equals(conversation.getStatus())) {
            conversation.setNeedsFollowUp(true);
            conversation.setFollowUpReason((String) data.getOrDefault("escalate_reason", "用户要求转人工"));
        }

        // 结束时间
        if (data.get("ended_at") != null) {
            try {
                conversation.setEndTime(LocalDateTime.parse((String) data.get("ended_at"),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } catch (Exception e) {
                conversation.setEndTime(LocalDateTime.now());
            }
        } else if ("COMPLETED".equals(conversation.getStatus()) || "ESCALATED".equals(conversation.getStatus())) {
            conversation.setEndTime(LocalDateTime.now());
        }

        // 计算时长
        if (conversation.getStartTime() != null && conversation.getEndTime() != null) {
            conversation.setDurationSeconds((int) java.time.Duration.between(
                conversation.getStartTime(), conversation.getEndTime()).getSeconds());
        }

        if (isNew) {
            conversationMapper.insert(conversation);
        } else {
            conversationMapper.updateById(conversation);
        }

        // 更新回访计划状态
        if (conversation.getPlanId() != null) {
            ReturnVisitPlan plan = returnVisitPlanMapper.selectById(conversation.getPlanId());
            if (plan != null) {
                if ("COMPLETED".equals(conversation.getStatus())) {
                    plan.setStatus("AI_COMPLETED");
                } else if ("ESCALATED".equals(conversation.getStatus())) {
                    plan.setStatus("AI_ESCALATED");
                } else {
                    plan.setStatus("AI_IN_PROGRESS");
                }
                returnVisitPlanMapper.updateById(plan);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("recordId", conversation.getId());
        result.put("conversationId", conversationId);
        result.put("status", conversation.getStatus());
        return result;
    }

    // ========== 私有方法 ==========

    private String generateConversationId() {
        return "AI-CONV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String getMemberLevelCode(Integer level) {
        if (level == null) return "NORMAL";
        switch (level) {
            case 1: return "SILVER";
            case 2: return "GOLD";
            case 3: return "PLATINUM";
            default: return "NORMAL";
        }
    }

    private String getMemberLevelName(Integer level) {
        if (level == null) return "普通会员";
        switch (level) {
            case 1: return "银卡会员";
            case 2: return "金卡会员";
            case 3: return "白金会员";
            default: return "普通会员";
        }
    }

    private String getVisitTypeName(Integer type) {
        if (type == null) return "初诊";
        switch (type) {
            case 1: return "复诊";
            case 2: return "手术";
            default: return "初诊";
        }
    }

    private String generateGreeting(Patient patient) {
        if (patient == null) {
            return "您好，我是诊所的AI回访专员，想了解一下您最近的恢复情况。";
        }

        String name = patient.getPatientName();
        String gender = patient.getGender() != null && patient.getGender() == 1 ? "先生" : "女士";

        return String.format("%s%s您好，我是诊所的AI回访专员。想耽误您几分钟时间，了解一下您治疗后的恢复情况，您现在方便吗？", name, gender);
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return null; }
    }
}
