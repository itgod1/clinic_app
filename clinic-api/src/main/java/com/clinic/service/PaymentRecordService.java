package com.clinic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clinic.entity.PaymentRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 支付流水服务接口
 */
public interface PaymentRecordService extends IService<PaymentRecord> {

    /**
     * 创建支付记录
     *
     * @param record 支付记录
     * @return 创建后的记录
     */
    PaymentRecord createPaymentRecord(PaymentRecord record);

    /**
     * 根据处方ID查询支付记录
     *
     * @param prescriptionId 处方ID
     * @return 支付记录列表
     */
    List<PaymentRecord> getByPrescriptionId(Long prescriptionId);

    /**
     * 根据订单号查询支付记录
     *
     * @param orderNo 订单号
     * @return 支付记录
     */
    PaymentRecord getByOrderNo(String orderNo);

    /**
     * 支付
     *
     * @param orderNo       订单号
     * @param paymentMethod 支付方式
     * @param operatorId    操作员ID
     * @param operatorName  操作员姓名
     * @return 是否成功
     */
    boolean pay(String orderNo, Integer paymentMethod, Long operatorId, String operatorName);

    /**
     * 退款
     *
     * @param orderNo      订单号
     * @param refundAmount 退款金额
     * @param reason       退款原因
     * @param operatorId   操作员ID
     * @param operatorName 操作员姓名
     * @return 是否成功
     */
    boolean refund(String orderNo, BigDecimal refundAmount, String reason, Long operatorId, String operatorName);

    /**
     * 生成订单号
     *
     * @return 订单号
     */
    String generateOrderNo();

    /**
     * 查询今日支付方式统计
     *
     * @param clinicId 诊所ID
     * @return 统计结果
     */
    Map<Integer, BigDecimal> getDailyPaymentMethodStats(Long clinicId);

    /**
     * 查询今日收入明细
     *
     * @param clinicId 诊所ID
     * @return 收入明细
     */
    List<Map<String, Object>> getDailyRevenueDetails(Long clinicId);
}
