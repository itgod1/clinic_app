package com.clinic.service;

import com.clinic.entity.MedicalRecord;

import java.util.List;
import java.util.Map;

public interface MedicalRecordService {

    /**
     * 获取病历列表（带枚举名称）
     */
    Map<String, Object> list(Long clinicId, Long patientId, Long doctorId, String startDate, String endDate, String keyword, Integer pageNum, Integer pageSize);

    /**
     * 获取病历详情（带枚举名称）
     */
    MedicalRecord getById(Long id);

    /**
     * 创建病历
     */
    void create(MedicalRecord medicalRecord);

    /**
     * 更新病历
     */
    void update(MedicalRecord medicalRecord);

    /**
     * 删除病历
     */
    void delete(Long recordId);

    /**
     * 生成病历号
     */
    String generateRecordNo();

    /**
     * 校验病历数据
     */
    void validateMedicalRecord(MedicalRecord medicalRecord);

    /**
     * 根据患者ID列表查询病历
     * @param clinicId 诊所ID
     * @param patientIds 患者ID列表
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 病历列表和总数
     */
    Map<String, Object> listByPatientIds(Long clinicId, List<Long> patientIds, Integer pageNum, Integer pageSize);


}
