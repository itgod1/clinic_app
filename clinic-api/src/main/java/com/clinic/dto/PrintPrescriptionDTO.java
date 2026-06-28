package com.clinic.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 处方打印数据
 */
@Data
public class PrintPrescriptionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 诊所信息
     */
    private ClinicInfo clinic;

    /**
     * 处方信息
     */
    private PrescriptionInfo prescription;

    /**
     * 患者信息
     */
    private PatientInfo patient;

    /**
     * 医生信息
     */
    private DoctorInfo doctor;

    /**
     * 处方项目列表
     */
    private List<PrescriptionItemInfo> items;

    /**
     * 金额信息
     */
    private AmountInfo amount;

    @Data
    public static class ClinicInfo {
        private String name;
        private String address;
        private String phone;
        private String licenseNo;
    }

    @Data
    public static class PrescriptionInfo {
        private String prescriptionNo;
        private String prescriptionType;
        private String department;
        private LocalDateTime createTime;
        private String diagnosis;
    }

    @Data
    public static class PatientInfo {
        private String name;
        private String phone;
        private String gender;
        private Integer age;
    }

    @Data
    public static class DoctorInfo {
        private String name;
        private String title;
        private String signUrl;
    }

    @Data
    public static class PrescriptionItemInfo {
        private String itemType;
        private String name;
        private String spec;
        private String unit;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private String usage;
        private String frequency;
        private String duration;
    }

    @Data
    public static class AmountInfo {
        private BigDecimal totalAmount;
        private BigDecimal discountAmount;
        private BigDecimal actualAmount;
    }
}
