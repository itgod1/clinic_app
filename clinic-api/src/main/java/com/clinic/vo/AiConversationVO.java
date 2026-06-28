package com.clinic.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiConversationVO {
    private String conversationId;
    private Long planId;
    private Long patientId;
    private String status;
    private LocalDateTime startTime;
    private String greetingMessage;
}
