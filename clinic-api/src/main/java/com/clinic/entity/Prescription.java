package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("prescription")
public class Prescription implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long clinicId;
    private String prescriptionNo;
    private Integer prescriptionType;
    private Long registrationId;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private Long deptId;
    private String departmentName;
    private String diagnosis;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal actualAmount;
    private Integer paymentStatus;
    //private String chiefComplaint;
    //private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    private Integer deleted;

    /**
     * 缴费方式：1微信 2支付宝 3现金 4医保 5会员卡
     */
    private Integer paymentMethod;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String prescriptionTypeName;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String statusName;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private java.util.List<PrescriptionItem> items;
}

