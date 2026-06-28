package com.clinic.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_return_visit_conversation")
public class AiReturnVisitConversation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String conversationId;
    private Long planId;
    private Long patientId;
    private String difyConversationId;
    private String channel;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationSeconds;
    private Integer messageCount;
    private String summary;
    private String patientStatus;
    private Integer satisfaction;
    private Boolean needsFollowUp;
    private String followUpReason;
    private String fullDialogue;
    private String extractedInfo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
