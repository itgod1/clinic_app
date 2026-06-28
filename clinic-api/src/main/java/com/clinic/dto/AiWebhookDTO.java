package com.clinic.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AiWebhookDTO {
    private String event;
    private String timestamp;
    private Map<String, Object> data;
}
