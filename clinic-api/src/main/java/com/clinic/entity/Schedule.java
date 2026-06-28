package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("schedule")
public class Schedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;

    private Long doctorId;

    private LocalDate scheduleDate;

    private LocalTime timeSlotStart;

    private LocalTime timeSlotEnd;

    private Integer totalQuota;

    private Integer remainingQuota;

    private Integer onlineQuota;

    private Integer status;

    private String stopReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
