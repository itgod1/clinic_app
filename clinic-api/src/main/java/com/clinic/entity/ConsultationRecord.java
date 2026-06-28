package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("consultation_record")
public class ConsultationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private String recordNo;
    private Integer recordType;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long doctorId;
    private String doctorName;
    private Long deptId;
    private String deptName;
    private LocalDate visitDate;
    private String consultationContent;
    private String designRequirements;
    private String suggestion;
    private Integer status;
    private String attachUrls;
    private String notes;
    private Integer remindCount;
    private LocalDateTime remindedAt;
    private LocalDate nextVisitDate;

    @TableField(exist = false)
    private String recordTypeName;

    @TableField(exist = false)
    private String statusName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
