package com.clinic.service;

import com.clinic.entity.ToothChartRecord;
import java.util.List;

public interface ToothChartService {

    /**
     * 根据病历ID获取牙位图记录
     */
    List<ToothChartRecord> getByMedicalRecordId(Long medicalRecordId);

    /**
     * 批量保存牙位图记录（先删后插，事务保证）
     */
    void saveToothChart(Long medicalRecordId, Long clinicId, List<ToothChartRecord> records);
}
