package com.clinic.service;

import java.util.List;
import java.util.Map;

public interface ReportService {

    Map<String, Object> dashboard(Long clinicId);

    Map<String, Object> daily(Long clinicId, String date);

    Map<String, Object> monthly(Long clinicId, Integer year, Integer month);

    List<Map<String, Object>> deptRanking(Long clinicId, String startDate, String endDate);

    List<Map<String, Object>> doctorPerformance(Long clinicId, String startDate, String endDate, Long doctorId);

    List<Map<String, Object>> medicineConsume(Long clinicId, String startDate, String endDate);

    /**
     * 调试：查询今日处方统计
     */
    Map<String, Object> debugTodayPrescription(Long clinicId);
}
