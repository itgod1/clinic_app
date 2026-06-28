package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("clinic")
public class Clinic implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String clinicName;
    private String clinicCode;
    private String address;
    private String province;
    private String city;
    private String district;
    private String contactPerson;
    private String contactPhone;
    private String logoUrl;
    private String licenseNo;
    private String licenseUrl;
    private Integer businessStatus;

    // 数据库是 datetime 类型，但前端只传日期，这里用 LocalDateTime 并设置时间为 00:00:00
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime serviceStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime serviceEnd;

    private String description;
    private Double latitude;
    private Double longitude;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // 设置日期时，自动转换为 LocalDateTime（时间为 00:00:00）
    public void setServiceStart(LocalDate date) {
        this.serviceStart = date != null ? date.atStartOfDay() : null;
    }

    public void setServiceEnd(LocalDate date) {
        this.serviceEnd = date != null ? date.atStartOfDay() : null;
    }
}
