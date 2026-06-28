package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("follow_up_plan")
public class FollowUpPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long registrationId;
    private Long medicalRecordId;
    private Long doctorId;
    private String doctorName;

    private String followUpItem;
    private String followUpDesc;
    private String originalTreatment;

    private Integer recoveryDays;
    private LocalDate planDate;
    private Integer remindDaysBefore;
    private LocalDate actualDate;

    private String status;
    private Integer priority;

    private LocalDateTime remindedAt;
    private Long remindedTo;
    private Integer remindCount;

    private LocalDateTime appointedAt;
    private Long appointedBy;
    private String appointedByName;
    private String appointmentNote;

    private LocalDateTime completedAt;
    private Long completedBy;
    private String completionNote;

    private Long followUpRegistrationId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String statusName;

    @TableField(exist = false)
    private String createdAtStr;

    @TableField(exist = false)
    private Integer daysUntilDue;
}
