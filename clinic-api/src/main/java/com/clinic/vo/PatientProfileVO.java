package com.clinic.vo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PatientProfileVO {
    private Long patientId;
    private String patientName;
    private String gender;
    private String genderText;
    private Integer age;
    private String phone;
    private String memberLevel;
    private String memberLevelText;
    private Integer visitCount;
    private LocalDate lastVisitDate;
    private String allergyHistory;
    private String medicalHistory;
    private String address;
}
