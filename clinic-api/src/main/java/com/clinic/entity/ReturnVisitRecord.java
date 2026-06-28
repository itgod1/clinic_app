package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("return_visit_record")
public class ReturnVisitRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;
    private Long clinicId;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long registrationId;
    private Long medicalRecordId;
    private Long doctorId;
    private String doctorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime visitDate;
    private String visitType;
    private String content;
    private String result;
    private Integer satisfaction;
    private Long nextPlanId;
    private String recordUrl;
    private String remark;

    @TableField(exist = false)
    private String visitTypeName;

    @TableField(exist = false)
    private String resultName;

    @TableField(exist = false)
    private String createdAtStr;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
