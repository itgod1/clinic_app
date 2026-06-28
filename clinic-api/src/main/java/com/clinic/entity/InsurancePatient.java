package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insurance_patient")
public class InsurancePatient implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long patientId;
    private String insuranceCardNo;
    private String insuranceType;
    private String insuranceCity;
    private String insuranceStatus;
    private String holderName;
    private String holderIdCard;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String bankName;
    private String remark;

    @TableField(exist = false)
    private String insuranceTypeName;

    @TableField(exist = false)
    private String insuranceStatusName;

    @TableField(exist = false)
    private String patientName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
