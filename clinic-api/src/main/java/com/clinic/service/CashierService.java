package com.clinic.service;

import com.clinic.entity.Prescription;

import java.math.BigDecimal;
import java.util.Map;

public interface CashierService {

    /**
     * 获取待缴费列表（带枚举名称）
     */
    Map<String, Object> unpaidList(Long clinicId, Integer pageNum, Integer pageSize);

    /**
     * 收费
     * @return 支付记录ID
     */
    Long pay(String orderId, Integer paymentMethod);

    /**
     * 退费
     */
    void refund(Long orderId);

    /**
     * 优惠
     */
    void discount(Long orderId, BigDecimal discountAmount);

    /**
     * 校验是否可以收费
     */
    void validateCanPay(Long orderId);

    /**
     * 校验是否可以退费
     */
    void validateCanRefund(Long orderId);

    /**
     * 校验优惠金额是否合法
     */
    void validateDiscount(Long orderId, BigDecimal discountAmount);
}
