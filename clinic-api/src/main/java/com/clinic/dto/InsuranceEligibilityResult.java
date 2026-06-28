package com.clinic.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class InsuranceEligibilityResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean eligible;
    private String insuranceType;
    private String insuranceTypeName;
    private String insuranceCardNo;
    private String holderName;
    private BigDecimal balance;
    private String message;
}
