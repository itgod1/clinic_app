package com.clinic.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 病历打印数据
 */
@Data
public class PrintMedicalRecordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 诊所信息
     */
    private ClinicInfo clinic;

    /**
     * 病历信息
     */
    private RecordInfo record;

    /**
     * 患者信息
     */
    private PatientInfo patient;

    /**
     * 医生信息
     */
    private DoctorInfo doctor;

    @Data
    public static class ClinicInfo {
        private String name;
        private String address;
        private String phone;
    }

    @Data
    public static class RecordInfo {
        private String recordNo;
        private LocalDateTime visitTime;
        private String visitType;
        private String department;
        private String chiefComplaint;
        private String presentIllness;
        private String pastHistory;
        private String physicalExam;
        private String auxiliaryExam;
        private String diagnosis;
        private String treatment;
        private String medicalAdvice;
    }

    @Data
    public static class PatientInfo {
        private String name;
        private String gender;
        private Integer age;
        private String phone;
        private String address;
        private String idCard;
    }

    @Data
    public static class DoctorInfo {
        private String name;
        private String title;
        private String signUrl;
    }
}
