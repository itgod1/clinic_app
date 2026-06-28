package com.clinic.service.impl;

import com.clinic.controller.dto.ScheduleBatchDTO;
import com.clinic.entity.Schedule;
import com.clinic.mapper.ScheduleMapper;
import com.clinic.service.NotificationService;
import com.clinic.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final NotificationService notificationService;

    @Override
    public List<Schedule> list(Long clinicId, Long doctorId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        if (doctorId != null) {
            return scheduleMapper.selectByDoctorAndDateRange(clinicId, doctorId, start, end);
        } else {
            return scheduleMapper.selectByDateRange(clinicId, start, end);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSet(ScheduleBatchDTO data) {
        Long clinicId = data.getClinicId();
        Long doctorId = data.getDoctorId();
        List<ScheduleBatchDTO.ScheduleItemDTO> schedules = data.getSchedules();

        if (schedules == null || schedules.isEmpty()) {
            return;
        }

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        boolean hasNewSchedule = false;

        for (ScheduleBatchDTO.ScheduleItemDTO item : schedules) {
            LocalDate scheduleDate = LocalDate.parse(item.getScheduleDate());

            // 处理上午排班
            if (Boolean.TRUE.equals(item.getAmIsWork())) {
                if (item.getLimitNum() == null  || item.getLimitNum() <= 0) {
                    item.setLimitNum(20);
                }
                LocalTime startTime = LocalTime.parse(item.getAmStartTime(), timeFormatter);
                LocalTime endTime = LocalTime.parse(item.getAmEndTime(), timeFormatter);
                saveOrUpdateSchedule(clinicId, doctorId, scheduleDate, startTime, endTime, item.getLimitNum());
                hasNewSchedule = true;
            } else {
                // 取消上午排班
                if (item.getAmStartTime() != null) {
                    LocalTime startTime = LocalTime.parse(item.getAmStartTime(), timeFormatter);
                    cancelSchedule(clinicId, doctorId, scheduleDate, startTime);
                }
            }

            // 处理下午排班
            if (Boolean.TRUE.equals(item.getPmIsWork())) {
                if (item.getLimitNum() == null  || item.getLimitNum() <= 0) {
                    item.setLimitNum(20);
                }
                LocalTime startTime = LocalTime.parse(item.getPmStartTime(), timeFormatter);
                LocalTime endTime = LocalTime.parse(item.getPmEndTime(), timeFormatter);
                saveOrUpdateSchedule(clinicId, doctorId, scheduleDate, startTime, endTime, item.getLimitNum());
                hasNewSchedule = true;
            } else {
                // 取消下午排班
                if (item.getPmStartTime() != null) {
                    LocalTime startTime = LocalTime.parse(item.getPmStartTime(), timeFormatter);
                    cancelSchedule(clinicId, doctorId, scheduleDate, startTime);
                }
            }
        }

        // 发送排班通知
        if (hasNewSchedule && !schedules.isEmpty()) {
            String scheduleDate = schedules.get(0).getScheduleDate();
            notificationService.createScheduleNotification(clinicId, doctorId, scheduleDate);
        }
    }

    private void saveOrUpdateSchedule(Long clinicId, Long doctorId, LocalDate scheduleDate,
                                       LocalTime startTime, LocalTime endTime, Integer limitNum) {
        // 查询是否已存在该时段的排班（不限制状态，允许重新激活）
        Schedule existSchedule = scheduleMapper.selectBySlotIgnoreStatus(
                clinicId, doctorId, scheduleDate, startTime);

        if (existSchedule != null) {
            // 更新现有排班
            existSchedule.setTimeSlotEnd(endTime);
            existSchedule.setTotalQuota(limitNum);
            existSchedule.setRemainingQuota(limitNum);
            existSchedule.setOnlineQuota(limitNum / 2);
            existSchedule.setStatus(1); // 正常
            existSchedule.setUpdatedAt(LocalDateTime.now());
            scheduleMapper.updateById(existSchedule);
        } else {
            // 新增排班
            Schedule schedule = new Schedule();
            schedule.setClinicId(clinicId);
            schedule.setDoctorId(doctorId);
            schedule.setScheduleDate(scheduleDate);
            schedule.setTimeSlotStart(startTime);
            schedule.setTimeSlotEnd(endTime);
            schedule.setTotalQuota(limitNum);
            schedule.setRemainingQuota(limitNum);
            schedule.setOnlineQuota(limitNum / 2);
            schedule.setStatus(1); // 正常
            schedule.setCreatedAt(LocalDateTime.now());
            scheduleMapper.insert(schedule);
        }
    }

    private void cancelSchedule(Long clinicId, Long doctorId, LocalDate scheduleDate,
                                 LocalTime startTime) {
        // 查询是否已存在该时段的排班（不限制状态）
        Schedule existSchedule = scheduleMapper.selectBySlotIgnoreStatus(
                clinicId, doctorId, scheduleDate, startTime);

        if (existSchedule != null) {
            // 取消排班：将状态设为停用（0）或删除
            existSchedule.setStatus(0); // 停用
            existSchedule.setUpdatedAt(LocalDateTime.now());
            scheduleMapper.updateById(existSchedule);
        }
    }
}
