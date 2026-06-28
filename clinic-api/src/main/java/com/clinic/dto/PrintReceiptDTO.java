package com.clinic.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收费票据打印数据
 */
@Data
public class PrintReceiptDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 诊所信息
     */
    private ClinicInfo clinic;

    /**
     * 票据信息
     */
    private ReceiptInfo receipt;

    /**
     * 患者信息
     */
    private PatientInfo patient;

    /**
     * 收费项目列表
     */
    private List<ChargeItemInfo> items;

    /**
     * 支付信息
     */
    private PaymentInfo payment;

    /**
     * 操作员信息
     */
    private OperatorInfo operator;

    @Data
    public static class ClinicInfo {
        private String name;
        private String address;
        private String phone;
    }

    @Data
    public static class ReceiptInfo {
        private String receiptNo;
        private String orderNo;
        private LocalDateTime printTime;
    }

    @Data
    public static class PatientInfo {
        private String name;
        private String phone;
    }

    @Data
    public static class ChargeItemInfo {
        private String itemName;
        private String spec;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    public static class PaymentInfo {
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal actualAmount;
        private String paymentMethod;
        private BigDecimal receivedAmount;
        private BigDecimal changeAmount;
    }

    @Data
    public static class OperatorInfo {
        private String name;
    }
}
