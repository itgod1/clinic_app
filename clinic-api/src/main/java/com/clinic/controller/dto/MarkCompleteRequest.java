package com.clinic.controller.dto;

import lombok.Data;

@Data
public class MarkCompleteRequest {
    private Long doctorId;
    private String note;
}
