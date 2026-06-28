package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("registration")
public class Registration implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private String regNo;
    private LocalDate regDate;
    private String regTime;
    private Integer queueNo;
    private Long patientId;

    private String patientName;
    private String phone;
    private Long doctorId;

    private String doctorName;
    private Long deptId;

    private String deptName;
    private Integer visitType;
    private String chiefComplaint;
    private String cancelReason;
    private Integer status;

    @TableField(exist = false)
    private String visitTypeName;

    @TableField(exist = false)
    private String statusName;

    // 实时排队信息（非数据库字段，查询时动态计算）
    @TableField(exist = false)
    private Integer currentNumber;      // 当前叫号

    @TableField(exist = false)
    private Integer aheadCount;         // 前方等待人数

    @TableField(exist = false)
    private Integer expectedWaitTime;   // 预计等待时间(分钟)

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}