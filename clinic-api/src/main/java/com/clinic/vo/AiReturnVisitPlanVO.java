package com.clinic.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

@Data
public class AiReturnVisitPlanVO {
    private Long planId;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private LocalDate planDate;
    private String planTime;
    private String visitType;
    private String visitItem;
    private Integer priority;
    private String status;
    private String templateContent;
    private Map<String, Object> context;
}
