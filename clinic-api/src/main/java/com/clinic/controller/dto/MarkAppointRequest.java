package com.clinic.controller.dto;

import lombok.Data;

@Data
public class MarkAppointRequest {
    private Long doctorId;
    private String doctorName;
    private String note;
}
