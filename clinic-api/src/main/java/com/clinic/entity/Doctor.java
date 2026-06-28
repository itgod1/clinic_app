package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("doctor")
public class Doctor implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long deptId;
    private String doctorName;
    private String doctorCode;
    private String specialty;
    private String phone;
    private String email;
    private String avatarUrl;
    private String introduction;
    private Integer workStatus;
    private Integer serviceCount;
    private BigDecimal rating;
    private LocalDate joinDate;
    private String position;
    private Integer status;

    @TableField(exist = false)
    private String deptName;

    @TableField(exist = false)
    private Boolean hasSchedule;

    private String titleName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}