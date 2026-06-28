package com.clinic.service.miniapp;

import com.clinic.entity.Patient;

import java.util.List;

public interface MiniappPatientService {

    /**
     * 获取当前用户的就诊人列表
     */
    List<Patient> getMyPatients(Long userId, Long clinicId);

    /**
     * 获取当前用户的某个就诊人详情
     */
    Patient getMyPatientById(Long patientId, Long userId);

    /**
     * 添加就诊人
     */
    void createMyPatient(Long userId, Patient patient);

    /**
     * 更新就诊人
     */
    void updateMyPatient(Long userId, Patient patient);

    /**
     * 删除就诊人
     */
    void deleteMyPatient(Long patientId, Long userId);

    /**
     * 设置默认就诊人
     */
    void setDefaultPatient(Long userId, Long patientId, Long clinicId);

    /**
     * 获取默认就诊人
     */
    Patient getDefaultPatient(Long userId, Long clinicId);
}
