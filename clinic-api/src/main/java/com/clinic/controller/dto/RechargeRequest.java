package com.clinic.controller.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RechargeRequest {
    private Long patientId;
    private BigDecimal amount;
    private BigDecimal giftAmount;
    private Integer paymentMethod;
    private String remark;
}
