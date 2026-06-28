package com.clinic.dto;

import lombok.Data;

/**
 * 支付处方请求DTO
 */
@Data
public class PayPrescriptionDTO {

    /**
     * 处方ID
     */
    private Long prescriptionId;

    /**
     * 诊所ID
     */
    private Long clinicId;

    /**
     * 支付方式：1-现金 2-微信支付 3-支付宝 4-银行卡 5-会员卡
     */
    private Integer paymentMethod;
}
