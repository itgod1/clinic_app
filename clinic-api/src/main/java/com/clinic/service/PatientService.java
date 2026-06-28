package com.clinic.service;

import com.clinic.entity.Patient;

import java.math.BigDecimal;
import java.util.Map;

public interface PatientService {

    /**
     * 获取患者列表（带会员等级名称）
     */
    Map<String, Object> list(Long clinicId, String keyword, Integer memberLevel, String startDate, String endDate, Integer pageNum, Integer pageSize);

    /**
     * 获取患者详情（带会员等级名称）
     */
    Patient getById(Long id);

    /**
     * 创建患者
     */
    void create(Patient patient);

    /**
     * 更新患者
     */
    void update(Patient patient);

    /**
     * 更新患者状态
     */
    void updateStatus(Patient patient);

    /**
     * 删除患者
     */
    void delete(Long patientId);

    /**
     * 检查手机号是否已存在
     */
    boolean existsByPhone(String phone, Long clinicId);

    /**
     * 校验手机号唯一性
     */
    void validatePhoneUnique(String phone, Long clinicId);

    /**
     * 校验患者信息
     */
    void validatePatient(Patient patient);

    /**
     * 生成患者编码
     */
    String generatePatientCode();

    /**
     * 会员充值
     */
    void recharge(Long patientId, BigDecimal amount, BigDecimal giftAmount, Integer paymentMethod, String remark);
}
