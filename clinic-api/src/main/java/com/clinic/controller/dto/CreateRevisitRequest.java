package com.clinic.controller.dto;

import lombok.Data;

@Data
public class CreateRevisitRequest {
    private Long medicalRecordId;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private String originalTreatment;
    private String followUpItem;
    private String followUpDesc;
    private Integer recoveryDays;
    private String planDate;
}
