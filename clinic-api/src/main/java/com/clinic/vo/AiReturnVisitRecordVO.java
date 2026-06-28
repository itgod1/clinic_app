package com.clinic.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiReturnVisitRecordVO {
    private Long recordId;
    private String conversationId;
    private Long planId;
    private Long patientId;
    private String patientName;
    private String channel;
    private String result;
    private String summary;
    private Integer satisfaction;
    private String patientStatus;
    private Boolean needsFollowUp;
    private String followUpReason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationSeconds;
}
