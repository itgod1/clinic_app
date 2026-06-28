package com.clinic.service;

import com.clinic.entity.FollowUpPlan;

import java.util.List;
import java.util.Map;

public interface FollowUpPlanService {

    Map<String, Object> listPlans(Long clinicId, String status, String startDate, String endDate,
                                   String keyword, Long doctorId, Integer pageNum, Integer pageSize);

    FollowUpPlan getPlanById(Long id);

    void createPlan(FollowUpPlan plan) throws Exception;

    void updatePlan(FollowUpPlan plan) throws Exception;

    void cancelPlan(Long id) throws Exception;

    List<FollowUpPlan> getTodayDuePlans(Long clinicId);

    List<FollowUpPlan> getPendingPlans(Long clinicId);

    List<FollowUpPlan> getRemindedPlans(Long clinicId);

    List<FollowUpPlan> getAppointedPlans(Long clinicId);

    Map<String, Object> getStatistics(Long clinicId);

    void markAsReminded(Long planId, Long doctorId) throws Exception;

    void markAsAppointed(Long planId, Long doctorId, String doctorName, String note) throws Exception;

    void markAsCompleted(Long planId, Long doctorId, String note) throws Exception;

    List<FollowUpPlan> getPatientPlans(Long clinicId, Long patientId);

    void createPlanFromMedicalRecord(Long medicalRecordId, String followUpItem, String followUpDesc,
                                      Integer recoveryDays, Integer remindDaysBefore) throws Exception;

    void checkAndRemindDuePlans();
}
