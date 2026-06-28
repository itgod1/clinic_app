package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insurance_settlement")
public class InsuranceSettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long orderId;
    private Long patientId;
    private Long insurancePatientId;
    private String settlementNo;
    private BigDecimal totalAmount;
    private BigDecimal insurancePay;
    private BigDecimal selfPay;
    private BigDecimal accountPay;
    private BigDecimal cashPay;
    private String settlementStatus;
    private String insuranceClaimNo;
    private LocalDateTime submitTime;
    private LocalDateTime completeTime;
    private String errorMessage;
    private Long operatorId;
    private String operatorName;
    private String remark;

    @TableField(exist = false)
    private String settlementStatusName;

    @TableField(exist = false)
    private String patientName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
