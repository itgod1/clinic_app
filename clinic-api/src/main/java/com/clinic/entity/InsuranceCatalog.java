package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insurance_catalog")
public class InsuranceCatalog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private String catalogType;
    private String itemCode;
    private String itemName;
    private String specification;
    private String manufacturer;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal selfPayRatio;
    private String insuranceCategory;
    private String dosageForm;
    private String icdCode;
    private String hospitalLevel;
    private BigDecimal dailyLimit;
    private Integer status;
    private LocalDate effectiveDate;
    private LocalDate expireDate;

    @TableField(exist = false)
    private String catalogTypeName;

    @TableField(exist = false)
    private String insuranceCategoryName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
