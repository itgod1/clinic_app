package com.clinic.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ScheduleBatchDTO {

    @NotNull(message = "诊所ID不能为空")
    private Long clinicId;

    @NotNull(message = "医生ID不能为空")
    private Long doctorId;

    private List<ScheduleItemDTO> schedules;

    @Data
    public static class ScheduleItemDTO {
        private String scheduleDate;
        private Boolean amIsWork;
        private String amStartTime;
        private String amEndTime;
        private Boolean pmIsWork;
        private String pmStartTime;
        private String pmEndTime;
        private Integer limitNum;
    }
}
