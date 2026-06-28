package com.clinic.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InsuranceQueryResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String settlementNo;
    private String status;
    private BigDecimal insurancePay;
    private BigDecimal selfPay;
    private String claimNo;
    private LocalDateTime completeTime;
    private String message;
}
