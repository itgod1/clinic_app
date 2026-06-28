package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水记录表
 */
@Data
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 诊所ID
     */
    private Long clinicId;

    /**
     * 处方ID
     */
    private Long prescriptionId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 应收金额
     */
    private BigDecimal amount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 实收金额
     */
    private BigDecimal actualAmount;

    /**
     * 支付方式：1现金 2微信 3支付宝 4银行卡 5会员卡 6医保
     */
    private Integer paymentMethod;

    /**
     * 支付状态：0未支付 1已支付 2已退款 3部分退款
     */
    private Integer paymentStatus;

    /**
     * 第三方交易流水号（微信/支付宝）
     */
    private String transactionNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 操作员ID
     */
    private Long operatorId;

    /**
     * 操作员姓名
     */
    private String operatorName;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 备注
     */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    /**
     * 非数据库字段：支付方式名称
     */
    @TableField(exist = false)
    private String paymentMethodName;

    /**
     * 非数据库字段：支付状态名称
     */
    @TableField(exist = false)
    private String paymentStatusName;
}
