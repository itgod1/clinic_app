package com.clinic.service;

import com.clinic.entity.Prescription;

import java.util.Map;

public interface PrescriptionService {

    /**
     * 获取处方列表（带枚举名称和明细）
     */
    Map<String, Object> list(Long clinicId, Long doctorId, Integer prescriptionType, Integer paymentStatus, String startDate, String endDate, Integer pageNum, Integer pageSize);

    /**
     * 获取处方详情（带枚举名称和明细）
     */
    Prescription getById(Long id);

    /**
     * 创建处方（含明细和库存扣减）
     */
    Map<String, Object> create(Prescription prescription);

    /**
     * 处方退费（含库存回退）
     */
    void refund(Long prescriptionId, String reason);

    /**
     * 确认发药
     */
    void dispense(Long prescriptionId);

    /**
     * 校验处方是否可以退费
     */
    void validateCanRefund(Long prescriptionId);

    /**
     * 校验处方是否可以发药
     */
    void validateCanDispense(Long prescriptionId);

    /**
     * 支付处方
     * @param prescriptionId 处方ID
     * @param paymentMethod 支付方式：1-现金 2-微信支付 3-支付宝 4-银行卡 5-会员卡
     * @param payAmount 支付金额
     */
    void pay(Long prescriptionId, Integer paymentMethod, java.math.BigDecimal payAmount);
}
