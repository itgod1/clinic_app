package com.clinic.service;

import com.clinic.controller.dto.ScheduleBatchDTO;
import com.clinic.entity.Schedule;

import java.util.List;

public interface ScheduleService {

    List<Schedule> list(Long clinicId, Long doctorId, String startDate, String endDate);

    void batchSet(ScheduleBatchDTO data);
}
