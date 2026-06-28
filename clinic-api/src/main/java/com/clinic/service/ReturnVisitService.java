package com.clinic.service;

import com.clinic.entity.ReturnVisitPlan;
import com.clinic.entity.ReturnVisitRecord;
import com.clinic.entity.ReturnVisitTemplate;
import com.clinic.entity.ReturnVisitRule;
import com.clinic.common.result.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReturnVisitService {

    Map<String, Object> listPlans(Long clinicId, String planType, String status, String startDate, String endDate,
                                   String keyword, Long assigneeId, Integer pageNum, Integer pageSize);

    ReturnVisitPlan getPlanById(Long id);

    void createPlan(ReturnVisitPlan plan) throws Exception;

    void updatePlan(ReturnVisitPlan plan) throws Exception;

    void cancelPlan(Long id) throws Exception;

    List<ReturnVisitPlan> getTodayPlans(Long clinicId);

    List<ReturnVisitPlan> getOverduePlans(Long clinicId);

    Map<String, Object> getPlanStatistics(Long clinicId);

    // ========== 复诊提醒相关方法 ==========

    Map<String, Object> listRevisitPlans(Long clinicId, String status, String keyword, Integer pageNum, Integer pageSize);

    List<ReturnVisitPlan> getDueRevisitPlans(Long clinicId);

    List<ReturnVisitPlan> getPendingRevisitPlans(Long clinicId);

    List<ReturnVisitPlan> getRemindedRevisitPlans(Long clinicId);

    List<ReturnVisitPlan> getAppointedRevisitPlans(Long clinicId);

    Map<String, Object> getRevisitStatistics(Long clinicId);

    void markRevisitAsReminded(Long planId, Long doctorId) throws Exception;

    void markRevisitAsAppointed(Long planId, Long doctorId, String doctorName, String note) throws Exception;

    void markRevisitAsCompleted(Long planId, Long doctorId, String note) throws Exception;

    List<ReturnVisitPlan> getPatientRevisitPlans(Long clinicId, Long patientId);

    void createRevisitPlan(Long medicalRecordId, Long patientId, String patientName, String patientPhone,
                           String originalTreatment, String followUpItem, String followUpDesc,
                           Integer recoveryDays, LocalDate planDate) throws Exception;

    void checkAndRemindDueRevisits();

    Map<String, Object> listRecords(Long clinicId, Long patientId, Long doctorId,
                                      String startDate, String endDate, Integer pageNum, Integer pageSize);

    ReturnVisitRecord getRecordById(Long id);

    void createRecord(ReturnVisitRecord record) throws Exception;

    void linkPlanWithRecord(Long planId, Long recordId);

    List<ReturnVisitTemplate> listTemplates(Long clinicId);

    void createTemplate(ReturnVisitTemplate template) throws Exception;

    void updateTemplate(ReturnVisitTemplate template) throws Exception;

    void deleteTemplate(Long id);

    List<ReturnVisitRule> listRules(Long clinicId);

    void createRule(ReturnVisitRule rule) throws Exception;

    void updateRule(ReturnVisitRule rule) throws Exception;

    void deleteRule(Long id);

    void toggleRule(Long id) throws Exception;

    void applyRulesToRegistration(Long registrationId, Long patientId, String patientName, String visitItem);
}
