package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.enums.BusinessCode;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.*;
import com.clinic.mapper.*;
import com.clinic.service.ReturnVisitService;
import com.clinic.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReturnVisitServiceImpl implements ReturnVisitService {

    private final ReturnVisitPlanMapper planMapper;
    private final ReturnVisitRecordMapper recordMapper;
    private final ReturnVisitTemplateMapper templateMapper;
    private final ReturnVisitRuleMapper ruleMapper;
    private final RegistrationMapper registrationMapper;
    private final PatientMapper patientMapper;
    private final MedicalRecordMapper medicalRecordMapper;

    private static final Map<String, String> VISIT_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> PLAN_STATUS_MAP = new HashMap<>();
    private static final Map<String, String> RECORD_RESULT_MAP = new HashMap<>();
    private static final Map<String, String> PLAN_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> REVISIT_STATUS_MAP = new HashMap<>();

    static {
        VISIT_TYPE_MAP.put("PHONE", "电话");
        VISIT_TYPE_MAP.put("SMS", "短信");
        VISIT_TYPE_MAP.put("WECHAT", "微信");
        VISIT_TYPE_MAP.put("STORE", "到店");

        PLAN_STATUS_MAP.put("PENDING", "待执行");
        PLAN_STATUS_MAP.put("EXECUTED", "已执行");
        PLAN_STATUS_MAP.put("CANCELLED", "已取消");
        PLAN_STATUS_MAP.put("OVERDUE", "已逾期");

        RECORD_RESULT_MAP.put("CONTACTED", "已联系");
        RECORD_RESULT_MAP.put("NO_ANSWER", "未接通");
        RECORD_RESULT_MAP.put("REFUSED", "拒访");
        RECORD_RESULT_MAP.put("INVALID", "无效");

        PLAN_TYPE_MAP.put("FOLLOW_UP", "回访");
        PLAN_TYPE_MAP.put("REVISIT", "复诊提醒");

        REVISIT_STATUS_MAP.put("PENDING", "待提醒");
        REVISIT_STATUS_MAP.put("REMINDED", "已提醒");
        REVISIT_STATUS_MAP.put("APPOINTED", "已预约");
        REVISIT_STATUS_MAP.put("EXECUTED", "已完成");
        REVISIT_STATUS_MAP.put("CANCELLED", "已取消");
        REVISIT_STATUS_MAP.put("OVERDUE", "已逾期");
    }

    private void enrichPlanNames(ReturnVisitPlan plan) {
        if (plan == null) return;
        plan.setVisitTypeName(VISIT_TYPE_MAP.get(plan.getVisitType()));
        // 根据计划类型显示不同的状态名称
        if ("REVISIT".equals(plan.getPlanType())) {
            plan.setStatusName(REVISIT_STATUS_MAP.get(plan.getStatus()));
        } else {
            plan.setStatusName(PLAN_STATUS_MAP.get(plan.getStatus()));
        }
        if (plan.getCreatedAt() != null) {
            plan.setCreatedAtStr(plan.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }

    private void enrichRecordNames(ReturnVisitRecord record) {
        if (record == null) return;
        record.setVisitTypeName(VISIT_TYPE_MAP.get(record.getVisitType()));
        record.setResultName(RECORD_RESULT_MAP.get(record.getResult()));
        if (record.getCreatedAt() != null) {
            record.setCreatedAtStr(record.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
    }

    @Override
    public Map<String, Object> listPlans(Long clinicId, String planType, String status, String startDate, String endDate,
                                          String keyword, Long assigneeId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ReturnVisitPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitPlan::getClinicId, clinicId);
        wrapper.eq(StringUtils.hasText(planType), ReturnVisitPlan::getPlanType, planType);
        wrapper.eq(StringUtils.hasText(status), ReturnVisitPlan::getStatus, status);
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(ReturnVisitPlan::getPlanDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(ReturnVisitPlan::getPlanDate, LocalDate.parse(endDate));
        }
        wrapper.eq(assigneeId != null, ReturnVisitPlan::getAssigneeId, assigneeId);
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(ReturnVisitPlan::getPatientName, keyword)
                .or()
                .like(ReturnVisitPlan::getPatientPhone, keyword)
                .or()
                .like(ReturnVisitPlan::getVisitItem, keyword));
        wrapper.orderByDesc(ReturnVisitPlan::getCreatedAt);

        IPage<ReturnVisitPlan> page = planMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        for (ReturnVisitPlan plan : page.getRecords()) {
            enrichPlanNames(plan);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return result;
    }

    @Override
    public ReturnVisitPlan getPlanById(Long id) {
        ReturnVisitPlan plan = planMapper.selectById(id);
        enrichPlanNames(plan);
        return plan;
    }

    @Override
    @Transactional
    public void createPlan(ReturnVisitPlan plan) throws Exception {
        if (plan.getClinicId() == null) {
            throw new Exception("诊所ID不能为空");
        }
        if (plan.getPatientId() == null) {
            throw new Exception("患者ID不能为空");
        }
        if (plan.getPlanDate() == null) {
            throw new Exception("计划日期不能为空");
        }
        if (plan.getStatus() == null) {
            plan.setStatus("PENDING");
        }
        if (plan.getPriority() == null) {
            plan.setPriority(0);
        }
        planMapper.insert(plan);
    }

    @Override
    public void updatePlan(ReturnVisitPlan plan) throws Exception {
        if (plan.getId() == null) {
            throw new Exception("ID不能为空");
        }
        planMapper.updateById(plan);
    }

    @Override
    public void cancelPlan(Long id) throws Exception {
        ReturnVisitPlan plan = planMapper.selectById(id);
        if (plan == null) {
            throw new Exception("回访计划不存在");
        }
        plan.setStatus("CANCELLED");
        planMapper.updateById(plan);
    }

    @Override
    public List<ReturnVisitPlan> getTodayPlans(Long clinicId) {
        List<ReturnVisitPlan> plans = planMapper.selectTodayPlans(clinicId, LocalDate.now());
        plans.forEach(this::enrichPlanNames);
        return plans;
    }

    @Override
    public List<ReturnVisitPlan> getOverduePlans(Long clinicId) {
        List<ReturnVisitPlan> plans = planMapper.selectOverduePlans(clinicId);
        plans.forEach(this::enrichPlanNames);
        return plans;
    }

    @Override
    public Map<String, Object> getPlanStatistics(Long clinicId) {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("pending", planMapper.countByStatus(clinicId, "PENDING"));
        statistics.put("executed", planMapper.countByStatus(clinicId, "EXECUTED"));
        statistics.put("cancelled", planMapper.countByStatus(clinicId, "CANCELLED"));

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<ReturnVisitPlan> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(ReturnVisitPlan::getClinicId, clinicId)
                .eq(ReturnVisitPlan::getPlanDate, today)
                .eq(ReturnVisitPlan::getStatus, "PENDING");
        statistics.put("todayTotal", planMapper.selectCount(todayWrapper));

        LambdaQueryWrapper<ReturnVisitPlan> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(ReturnVisitPlan::getClinicId, clinicId)
                .lt(ReturnVisitPlan::getPlanDate, today)
                .eq(ReturnVisitPlan::getStatus, "PENDING");
        statistics.put("overdue", planMapper.selectCount(overdueWrapper));

        return statistics;
    }

    @Override
    public Map<String, Object> listRecords(Long clinicId, Long patientId, Long doctorId,
                                            String startDate, String endDate, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ReturnVisitRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitRecord::getClinicId, clinicId);
        wrapper.eq(patientId != null, ReturnVisitRecord::getPatientId, patientId);
        wrapper.eq(doctorId != null, ReturnVisitRecord::getDoctorId, doctorId);
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(ReturnVisitRecord::getVisitDate, LocalDateTime.parse(startDate + " 00:00:00"));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(ReturnVisitRecord::getVisitDate, LocalDateTime.parse(endDate + " 23:59:59"));
        }
        wrapper.orderByDesc(ReturnVisitRecord::getVisitDate);

        IPage<ReturnVisitRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        for (ReturnVisitRecord record : page.getRecords()) {
            enrichRecordNames(record);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return result;
    }

    @Override
    public ReturnVisitRecord getRecordById(Long id) {
        ReturnVisitRecord record = recordMapper.selectById(id);
        enrichRecordNames(record);
        return record;
    }

    @Override
    @Transactional
    public void createRecord(ReturnVisitRecord record) throws Exception {
        if (record.getClinicId() == null) {
            throw new Exception("诊所ID不能为空");
        }
        if (record.getPatientId() == null) {
            throw new Exception("患者ID不能为空");
        }
        if (record.getVisitDate() == null) {
            record.setVisitDate(LocalDateTime.now());
        }
        recordMapper.insert(record);

        if (record.getPlanId() != null) {
            linkPlanWithRecord(record.getPlanId(), record.getId());
        }
    }

    @Override
    @Transactional
    public void linkPlanWithRecord(Long planId, Long recordId) {
        ReturnVisitPlan plan = planMapper.selectById(planId);
        if (plan != null) {
            plan.setStatus("EXECUTED");
            planMapper.updateById(plan);
        }
    }

    @Override
    public List<ReturnVisitTemplate> listTemplates(Long clinicId) {
        LambdaQueryWrapper<ReturnVisitTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(clinicId != null, ReturnVisitTemplate::getClinicId, clinicId)
                .or()
                .isNull(ReturnVisitTemplate::getClinicId);
        wrapper.eq(ReturnVisitTemplate::getStatus, "ENABLED");
        wrapper.orderByAsc(ReturnVisitTemplate::getSortOrder);
        return templateMapper.selectList(wrapper);
    }

    @Override
    public void createTemplate(ReturnVisitTemplate template) throws Exception {
        if (!StringUtils.hasText(template.getName())) {
            throw new Exception("模板名称不能为空");
        }
        if (template.getStatus() == null) {
            template.setStatus("ENABLED");
        }
        templateMapper.insert(template);
    }

    @Override
    public void updateTemplate(ReturnVisitTemplate template) throws Exception {
        if (template.getId() == null) {
            throw new Exception("ID不能为空");
        }
        templateMapper.updateById(template);
    }

    @Override
    public void deleteTemplate(Long id) {
        templateMapper.deleteById(id);
    }

    @Override
    public List<ReturnVisitRule> listRules(Long clinicId) {
        LambdaQueryWrapper<ReturnVisitRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitRule::getClinicId, clinicId);
        wrapper.orderByDesc(ReturnVisitRule::getPriority);
        return ruleMapper.selectList(wrapper);
    }

    @Override
    public void createRule(ReturnVisitRule rule) throws Exception {
        if (rule.getClinicId() == null) {
            throw new Exception("诊所ID不能为空");
        }
        if (!StringUtils.hasText(rule.getRuleName())) {
            throw new Exception("规则名称不能为空");
        }
        if (rule.getStatus() == null) {
            rule.setStatus("ENABLED");
        }
        ruleMapper.insert(rule);
    }

    @Override
    public void updateRule(ReturnVisitRule rule) throws Exception {
        if (rule.getId() == null) {
            throw new Exception("ID不能为空");
        }
        ruleMapper.updateById(rule);
    }

    @Override
    public void deleteRule(Long id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public void toggleRule(Long id) throws Exception {
        ReturnVisitRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new Exception("规则不存在");
        }
        rule.setStatus("ENABLED".equals(rule.getStatus()) ? "DISABLED" : "ENABLED");
        ruleMapper.updateById(rule);
    }

    @Override
    @Transactional
    public void applyRulesToRegistration(Long registrationId, Long patientId, String patientName, String visitItem) {
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) return;

        LambdaQueryWrapper<ReturnVisitRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitRule::getClinicId, registration.getClinicId())
                .eq(ReturnVisitRule::getStatus, "ENABLED")
                .eq(ReturnVisitRule::getAutoCreate, 1);

        List<ReturnVisitRule> rules = ruleMapper.selectList(wrapper);
        for (ReturnVisitRule rule : rules) {
            boolean match = false;
            if ("ALL".equals(rule.getConditionType())) {
                match = true;
            } else if ("VISIT_ITEM".equals(rule.getConditionType()) && visitItem != null
                    && visitItem.contains(rule.getConditionValue())) {
                match = true;
            } else if ("DOCTOR".equals(rule.getConditionType()) && rule.getConditionValue() != null
                    && rule.getConditionValue().equals(String.valueOf(registration.getDoctorId()))) {
                match = true;
            } else if ("DEPT".equals(rule.getConditionType()) && rule.getConditionValue() != null
                    && rule.getConditionValue().equals(String.valueOf(registration.getDeptId()))) {
                match = true;
            }

            if (match && rule.getAutoCreate() == 1) {
                ReturnVisitPlan plan = new ReturnVisitPlan();
                plan.setClinicId(registration.getClinicId());
                plan.setPatientId(patientId);
                plan.setPatientName(patientName);
                plan.setRegistrationId(registrationId);
                plan.setVisitItem(visitItem);
                plan.setPlanDate(LocalDate.now().plusDays(7));
                plan.setVisitType("PHONE");
                plan.setAssigneeId(rule.getAssigneeId());
                plan.setAssigneeName(rule.getAssigneeName());
                plan.setStatus("PENDING");
                plan.setPriority(rule.getPriority());
                plan.setPlanType("FOLLOW_UP");
                planMapper.insert(plan);
            }
        }
    }

    // ========== 复诊提醒相关方法实现 ==========

    @Override
    public Map<String, Object> listRevisitPlans(Long clinicId, String status, String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ReturnVisitPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitPlan::getClinicId, clinicId);
        wrapper.eq(ReturnVisitPlan::getPlanType, "REVISIT");
        wrapper.eq(StringUtils.hasText(status), ReturnVisitPlan::getStatus, status);
        wrapper.and(StringUtils.hasText(keyword), w -> w
                .like(ReturnVisitPlan::getPatientName, keyword)
                .or()
                .like(ReturnVisitPlan::getPatientPhone, keyword)
                .or()
                .like(ReturnVisitPlan::getVisitItem, keyword)
                .or()
                .like(ReturnVisitPlan::getOriginalTreatment, keyword));
        wrapper.orderByAsc(ReturnVisitPlan::getPlanDate);

        IPage<ReturnVisitPlan> page = planMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        for (ReturnVisitPlan plan : page.getRecords()) {
            enrichPlanNames(plan);
            // 计算距离到期天数
            if (plan.getPlanDate() != null) {
                plan.setDaysUntilDue((int) ChronoUnit.DAYS.between(LocalDate.now(), plan.getPlanDate()));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return result;
    }

    @Override
    public List<ReturnVisitPlan> getDueRevisitPlans(Long clinicId) {
        List<ReturnVisitPlan> plans = planMapper.selectDueRevisitPlans(clinicId, LocalDate.now());
        plans.forEach(this::enrichPlanNames);
        return plans;
    }

    @Override
    public List<ReturnVisitPlan> getPendingRevisitPlans(Long clinicId) {
        // 查询所有待处理的复诊提醒（PENDING状态，按到期日期排序）
        LambdaQueryWrapper<ReturnVisitPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReturnVisitPlan::getClinicId, clinicId)
                .eq(ReturnVisitPlan::getPlanType, "REVISIT")
                .eq(ReturnVisitPlan::getStatus, "PENDING")
                .orderByAsc(ReturnVisitPlan::getPlanDate)
                .last("LIMIT 10");
        List<ReturnVisitPlan> plans = planMapper.selectList(wrapper);
        plans.forEach(plan -> {
            enrichPlanNames(plan);
            // 计算距离到期天数
            if (plan.getPlanDate() != null) {
                plan.setDaysUntilDue((int) ChronoUnit.DAYS.between(LocalDate.now(), plan.getPlanDate()));
            }
        });
        return plans;
    }

    @Override
    public List<ReturnVisitPlan> getRemindedRevisitPlans(Long clinicId) {
        List<ReturnVisitPlan> plans = planMapper.selectRemindedRevisitPlans(clinicId);
        plans.forEach(this::enrichPlanNames);
        return plans;
    }

    @Override
    public List<ReturnVisitPlan> getAppointedRevisitPlans(Long clinicId) {
        List<ReturnVisitPlan> plans = planMapper.selectAppointedRevisitPlans(clinicId);
        plans.forEach(this::enrichPlanNames);
        return plans;
    }

    @Override
    public Map<String, Object> getRevisitStatistics(Long clinicId) {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("pending", planMapper.countByTypeAndStatus(clinicId, "REVISIT", "PENDING"));
        statistics.put("reminded", planMapper.countByTypeAndStatus(clinicId, "REVISIT", "REMINDED"));
        statistics.put("appointed", planMapper.countByTypeAndStatus(clinicId, "REVISIT", "APPOINTED"));
        statistics.put("executed", planMapper.countByTypeAndStatus(clinicId, "REVISIT", "EXECUTED"));
        statistics.put("cancelled", planMapper.countByTypeAndStatus(clinicId, "REVISIT", "CANCELLED"));

        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<ReturnVisitPlan> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(ReturnVisitPlan::getClinicId, clinicId)
                .eq(ReturnVisitPlan::getPlanType, "REVISIT")
                .eq(ReturnVisitPlan::getPlanDate, today)
                .eq(ReturnVisitPlan::getStatus, "PENDING");
        statistics.put("todayDue", planMapper.selectCount(todayWrapper));

        LambdaQueryWrapper<ReturnVisitPlan> overdueWrapper = new LambdaQueryWrapper<>();
        overdueWrapper.eq(ReturnVisitPlan::getClinicId, clinicId)
                .eq(ReturnVisitPlan::getPlanType, "REVISIT")
                .lt(ReturnVisitPlan::getPlanDate, today)
                .eq(ReturnVisitPlan::getStatus, "PENDING");
        statistics.put("overdue", planMapper.selectCount(overdueWrapper));

        return statistics;
    }

    @Override
    @Transactional
    public void markRevisitAsReminded(Long planId, Long doctorId) throws Exception {
        ReturnVisitPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new Exception("复诊计划不存在");
        }
        if (!"REVISIT".equals(plan.getPlanType())) {
            throw new Exception("该计划不是复诊提醒类型");
        }
        plan.setStatus("REMINDED");
        plan.setRemindedTo(doctorId);
        plan.setRemindedAt(LocalDateTime.now());
        plan.setRemindCount(plan.getRemindCount() != null ? plan.getRemindCount() + 1 : 1);
        planMapper.updateById(plan);
    }

    @Override
    @Transactional
    public void markRevisitAsAppointed(Long planId, Long doctorId, String doctorName, String note) throws Exception {
        ReturnVisitPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new Exception("复诊计划不存在");
        }
        if (!"REVISIT".equals(plan.getPlanType())) {
            throw new Exception("该计划不是复诊提醒类型");
        }
        plan.setStatus("APPOINTED");
        plan.setAppointedBy(doctorId);
        plan.setAppointedByName(doctorName);
        plan.setAppointedAt(LocalDateTime.now());
        plan.setAppointmentNote(note);
        planMapper.updateById(plan);
    }

    @Override
    @Transactional
    public void markRevisitAsCompleted(Long planId, Long doctorId, String note) throws Exception {
        ReturnVisitPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new Exception("复诊计划不存在");
        }
        if (!"REVISIT".equals(plan.getPlanType())) {
            throw new Exception("该计划不是复诊提醒类型");
        }
        plan.setStatus("EXECUTED");
        /*plan.setCompletedBy(doctorId);
        plan.setCompletedAt(LocalDateTime.now());
        plan.setCompletionNote(note);*/
        plan.setActualDate(LocalDate.now());
        planMapper.updateById(plan);
    }

    @Override
    public List<ReturnVisitPlan> getPatientRevisitPlans(Long clinicId, Long patientId) {
        List<ReturnVisitPlan> plans = planMapper.selectActiveRevisitPlansByPatient(clinicId, patientId);
        plans.forEach(this::enrichPlanNames);
        return plans;
    }

    @Override
    @Transactional
    public void createRevisitPlan(Long medicalRecordId, Long patientId, String patientName, String patientPhone,
                                   String originalTreatment, String followUpItem, String followUpDesc,
                                   Integer recoveryDays, LocalDate planDate) throws Exception {
        MedicalRecord medicalRecord = medicalRecordMapper.selectById(medicalRecordId);
        if (medicalRecord == null) {
            throw new Exception("病历不存在");
        }

        ReturnVisitPlan plan = new ReturnVisitPlan();
        plan.setClinicId(medicalRecord.getClinicId());
        plan.setPatientId(patientId);
        plan.setPatientName(patientName);
        plan.setPatientPhone(patientPhone);
        plan.setMedicalRecordId(medicalRecordId);
        plan.setDoctorId(medicalRecord.getDoctorId());
        plan.setDoctorName(medicalRecord.getDoctorName());
        plan.setPlanType("REVISIT");
        plan.setOriginalTreatment(originalTreatment);
        plan.setVisitItem(followUpItem);
        plan.setContentTemplate(followUpDesc);
        plan.setRecoveryDays(recoveryDays);
        plan.setPlanDate(planDate);
        plan.setStatus("PENDING");
        plan.setPriority(0);

        planMapper.insert(plan);
    }

    @Override
    public void checkAndRemindDueRevisits() {
        LocalDate today = LocalDate.now();
        List<ReturnVisitPlan> duePlans = planMapper.selectAllDueRevisitPlans(today);

        for (ReturnVisitPlan plan : duePlans) {
            // 更新状态为已提醒，增加提醒次数
            planMapper.incrementRemindCount(plan.getId());

            // 可以在这里添加发送通知的逻辑
            // 例如：发送站内通知、短信、微信等
        }
    }
}
