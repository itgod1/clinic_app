package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("medical_record")
public class MedicalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private String recordNo;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long registrationId;
    private Long doctorId;
    private String doctorName;
    private Long deptId;
    private String deptName;
    private Integer visitType;
    private String chiefComplaint;
    private String diagnosis;
    private String treatment;
    private String medicalAdvice;
    //private String attachUrls;
    //private LocalDateTime createTime;
    //private LocalDateTime updateTime;
    private LocalDate visitDate;

    @TableField(exist = false)
    private String visitTypeName;

    @TableField(exist = false)
    private List<ToothChartRecord> toothChartRecords;

    @TableField(exist = false)
    private String toothChartSummary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}