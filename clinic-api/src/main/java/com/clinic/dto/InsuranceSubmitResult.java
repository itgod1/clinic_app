package com.clinic.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class InsuranceSubmitResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String claimNo;
    private BigDecimal insurancePay;
    private BigDecimal selfPay;
    private BigDecimal accountPay;
    private BigDecimal cashPay;
    private String message;
}
