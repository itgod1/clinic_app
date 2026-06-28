package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("patient")
public class Patient implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private Long userId;
    private String patientName;
    private String patientCode;
    private String phone;
    private Integer gender;
    private Integer age;
    private LocalDate birthday;
    private Integer memberLevel;
    private BigDecimal balance;
    private Integer points;
    private Integer visitCount;
    private LocalDate lastVisitDate;
    private String allergyHistory;
    private String medicalHistory;
    private Integer status;

    @TableField(exist = false)
    private String memberLevelName;

    // 小程序就诊人字段
    private String idCard;
    private String address;
    
    // 微信openid（用于接收模板消息）
    private String openId;

    @TableField(exist = false)
    private Boolean isDefault;

    // 兼容前端字段名
    public String getBirthDate() {
        return this.birthday != null ? this.birthday.toString() : null;
    }

    public void setBirthDate(String birthDate) {
        if (birthDate != null && !birthDate.isEmpty()) {
            this.birthday = LocalDate.parse(birthDate);
        }
    }



    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}