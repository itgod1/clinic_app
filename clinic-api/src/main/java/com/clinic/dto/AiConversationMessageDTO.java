package com.clinic.dto;

import lombok.Data;

@Data
public class AiConversationMessageDTO {
    private String role;
    private String content;
    private String intent;
    private String sentiment;
}
