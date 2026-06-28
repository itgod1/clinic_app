package com.clinic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiConversationStartDTO {
    private Long planId;
    private Long patientId;
    private String channel;
    private String difyConversationId;

    /**
     * 兼容带后缀的ID格式，如 "1777739727007.plan_id" → 1777739727007L
     */
    public void setPlanId(Object planId) {
        this.planId = parseId(planId);
    }

    /**
     * 兼容带后缀的ID格式，如 "1777739727007.patient_id" → 1777739727007L
     */
    public void setPatientId(Object patientId) {
        this.patientId = parseId(patientId);
    }

    private Long parseId(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString();
        // 如果包含点号，取点号前的部分
        if (str.contains(".")) {
            str = str.split("\\.")[0];
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
