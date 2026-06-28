package com.clinic.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PatientVisitHistoryVO {
    private Long patientId;
    private Integer total;
    private List<VisitRecordVO> records;

    @Data
    public static class VisitRecordVO {
        private Long registrationId;
        private Long medicalRecordId;
        private LocalDate visitDate;
        private String doctorName;
        private String deptName;
        private String chiefComplaint;
        private String diagnosis;
        private String treatment;
        private String medicalAdvice;
        private List<String> medicines;
        private String visitType;
    }
}
