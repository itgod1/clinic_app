package com.clinic.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiConversationCompleteDTO {
    private String result;
    private String summary;
    private String patientStatus;
    private Integer satisfaction;
    private Boolean needsFollowUp;
    private String followUpReason;
    private String nextVisitSuggestion;
    private List<Map<String, Object>> fullDialogue;
}
