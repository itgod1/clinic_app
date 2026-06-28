package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("return_visit_plan")
public class ReturnVisitPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long patientId;

    private Long doctorId;

    private String doctorName;
    private String patientName;
    private String patientPhone;
    private Long registrationId;
    private Long medicalRecordId;
    
    // 计划类型: FOLLOW_UP回访/REVISIT复诊提醒
    private String planType;
    
    // 回访相关
    private String visitItem;
    private LocalDate planDate;
    private String planTime;
    private String visitType;
    private String contentTemplate;
    private Long assigneeId;
    private String assigneeName;
    
    // 复诊提醒相关
    private Integer recoveryDays;
    private String originalTreatment;
    private LocalDateTime remindedAt;
    private Long remindedTo;
    private Integer remindCount;
    private LocalDateTime appointedAt;
    private Long appointedBy;
    private String appointedByName;
    private String appointmentNote;
    private LocalDate actualDate;
    
    // 状态: PENDING待处理/REMINDED已提醒/APPOINTED已预约/EXECUTED已执行/CANCELLED已取消/OVERDUE已逾期
    private String status;
    private Integer priority;

    @TableField(exist = false)
    private String visitTypeName;

    @TableField(exist = false)
    private String statusName;

    @TableField(exist = false)
    private String createdAtStr;

    @TableField(exist = false)
    private Integer daysUntilDue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
