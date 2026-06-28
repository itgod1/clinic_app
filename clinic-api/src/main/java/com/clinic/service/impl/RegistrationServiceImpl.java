package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.constant.DictConstants;
import com.clinic.common.exception.BusinessException;
import com.clinic.entity.Department;
import com.clinic.entity.Doctor;
import com.clinic.entity.Patient;
import com.clinic.entity.Registration;
import com.clinic.mapper.DepartmentMapper;
import com.clinic.mapper.PatientMapper;
import com.clinic.mapper.RegistrationMapper;
import com.clinic.mapper.ScheduleMapper;
import com.clinic.service.DoctorService;
import com.clinic.service.RegistrationService;
import com.clinic.entity.Schedule;
import com.clinic.vo.QueueInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationMapper registrationMapper;
    private final DoctorService doctorService;
    private final DepartmentMapper departmentMapper;
    private final PatientMapper patientMapper;
    private final ScheduleMapper scheduleMapper;

    @Override
    public Map<String, Object> list(Long clinicId, String keyword, String regDate, String startDate, String endDate, Long doctorId, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Registration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Registration::getClinicId, clinicId);
        queryWrapper.eq(Registration::getDeleted, 0);

        // 关键词搜索（患者姓名、手机号、挂号编号）
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(qw -> qw.like(Registration::getPatientName, keyword)
                    .or().like(Registration::getPhone, keyword)
                    .or().like(Registration::getRegNo, keyword));
        }

        if (doctorId != null) {
            queryWrapper.eq(Registration::getDoctorId, doctorId);
        }

        if (status != null) {
            queryWrapper.eq(Registration::getStatus, status);
        }

        // 单个日期
        if (StringUtils.hasText(regDate)) {
            queryWrapper.eq(Registration::getRegDate, regDate);
        }

        // 日期范围
        if (StringUtils.hasText(startDate)) {
            queryWrapper.ge(Registration::getRegDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            queryWrapper.le(Registration::getRegDate, LocalDate.parse(endDate));
        }

        queryWrapper.orderByDesc(Registration::getCreatedAt);

        Page<Registration> page = new Page<>(pageNum, pageSize);
        Page<Registration> resultPage = registrationMapper.selectPage(page, queryWrapper);

        resultPage.getRecords().forEach(this::enrichRegistration);

        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public Registration getById(Long id) {
        Registration registration = registrationMapper.selectById(id);
        if (registration != null) {
            enrichRegistration(registration);
        }
        return registration;
    }

    @Override
    public void create(Registration registration) {
        // 查询医生名称
        if (registration.getDoctorId() != null) {
            Doctor doctor = doctorService.getById(registration.getDoctorId());
            if (doctor != null) {
                registration.setDoctorName(doctor.getDoctorName());
            }
        }

        // 查询科室名称
        if (registration.getDeptId() != null) {
            Department department = departmentMapper.selectById(registration.getDeptId());
            if (department != null) {
                registration.setDeptName(department.getDeptName());
            }
        }

        // 减少排班号源
        reduceScheduleQuota(registration);

        String regNo = generateRegNo();
        registration.setRegNo(regNo);
        registration.setPhone(registration.getPhone());
        registration.setChiefComplaint(registration.getChiefComplaint());
        registration.setQueueNo(generateQueueNo(registration.getDoctorId(), registration.getRegDate()));
        registration.setCreatedAt(LocalDateTime.now());
        registration.setUpdatedAt(LocalDateTime.now());
        registration.setDeleted(0);
        registrationMapper.insert(registration);
    }

    /**
     * 减少排班号源
     */
    private void reduceScheduleQuota(Registration registration) {
        // 解析时间段
        String timeSlot = registration.getRegTime();
        String startTimeStr = timeSlot.split("-")[0];
        LocalTime timeSlotStart = LocalTime.parse(startTimeStr);

        // 查询排班
        Schedule schedule = scheduleMapper.selectByDoctorAndDateAndSlot(
                registration.getClinicId(),
                registration.getDoctorId(),
                registration.getRegDate(),
                timeSlotStart
        );

        if (schedule != null && schedule.getRemainingQuota() != null && schedule.getRemainingQuota() > 0) {
            // 减少剩余号源
            schedule.setRemainingQuota(schedule.getRemainingQuota() - 1);
            schedule.setUpdatedAt(LocalDateTime.now());
            // 更新排班表
            scheduleMapper.updateById(schedule);
        }
    }

    @Override
    public void cancel(Long Id, String reason) {
        Registration registration = registrationMapper.selectById(Id);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        // 恢复排班号源
        restoreScheduleQuota(registration);

        registration.setCancelReason(reason);
        registration.setUpdatedAt(LocalDateTime.now());
        registrationMapper.deleteById(registration);
    }

    /**
     * 恢复排班号源
     */
    private void restoreScheduleQuota(Registration registration) {
        // 只有排队中的挂号才恢复号源
        if (registration.getStatus() == null || registration.getStatus() != 1) {
            return;
        }

        // 解析时间段
        String timeSlot = registration.getRegTime();
        if (!timeSlot.contains("-")) {
            return;
        }
        String startTimeStr = timeSlot.split("-")[0];
        LocalTime timeSlotStart;
        try {
            timeSlotStart = LocalTime.parse(startTimeStr);
        } catch (Exception e) {
            return;
        }

        // 查询排班
        Schedule schedule = scheduleMapper.selectByDoctorAndDateAndSlot(
                registration.getClinicId(),
                registration.getDoctorId(),
                registration.getRegDate(),
                timeSlotStart
        );

        if (schedule != null && schedule.getTotalQuota() != null) {
            // 恢复剩余号源（不能超过总号源）
            int newRemaining = schedule.getRemainingQuota() + 1;
            if (newRemaining > schedule.getTotalQuota()) {
                newRemaining = schedule.getTotalQuota();
            }
            schedule.setRemainingQuota(newRemaining);
            schedule.setUpdatedAt(LocalDateTime.now());
            // 更新排班表
            scheduleMapper.updateById(schedule);
        }
    }

    @Override
    public void updateStatus(Registration registration) {
        registration.setUpdatedAt(LocalDateTime.now());
        registrationMapper.updateById(registration);
    }

    @Override
    public String generateRegNo() {
        return "REG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    @Override
    public Integer generateQueueNo(Long doctorId, LocalDate regDate) {
        LambdaQueryWrapper<Registration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Registration::getDoctorId, doctorId);
        queryWrapper.eq(Registration::getRegDate, regDate);
        queryWrapper.orderByDesc(Registration::getQueueNo);
        queryWrapper.last("LIMIT 1");
        Registration lastRegistration = registrationMapper.selectOne(queryWrapper);
        
        if (lastRegistration != null && lastRegistration.getQueueNo() != null) {
            return lastRegistration.getQueueNo() + 1;
        }
        return 1; // 第一个号从1开始
    }

    @Override
    public void validateRegistration(Registration registration) {
        if (registration.getPatientId() == null && !StringUtils.hasText(registration.getPatientName())) {
            throw new BusinessException("患者姓名不能为空");
        }
        if (registration.getDoctorId() == null) {
            throw new BusinessException("医生不能为空");
        }
        if (registration.getRegDate() == null) {
            throw new BusinessException("就诊日期不能为空");
        }
        if (!StringUtils.hasText(registration.getRegTime())) {
            throw new BusinessException("就诊时间不能为空");
        }

        // 验证医生排班
        validateDoctorSchedule(registration);
    }

    /**
     * 验证医生排班
     */
    private void validateDoctorSchedule(Registration registration) {
        // 解析时间段，格式为 "14:00-14:30"
        String timeSlot = registration.getRegTime();
        String startTimeStr;
        String endTimeStr;
        if (timeSlot.contains("-")) {
            String[] parts = timeSlot.split("-");
            startTimeStr = parts[0];
            endTimeStr = parts[1];
        } else {
            throw new BusinessException("就诊时间格式不正确");
        }

        LocalTime timeSlotStart;
        LocalTime timeSlotEnd;
        try {
            timeSlotStart = LocalTime.parse(startTimeStr);
            timeSlotEnd = LocalTime.parse(endTimeStr);
        } catch (Exception e) {
            throw new BusinessException("就诊时间格式不正确");
        }

        // 查询医生当天的所有排班
        List<Schedule> schedules = scheduleMapper.selectByDoctorAndDateRange(
                registration.getClinicId(),
                registration.getDoctorId(),
                registration.getRegDate(),
                registration.getRegDate()
        );

        if (schedules == null || schedules.isEmpty()) {
            throw new BusinessException("该医生在选定日期没有排班，请选择其他日期");
        }

        // 查找包含该时段的排班
        Schedule matchedSchedule = schedules.stream()
                .filter(s -> {
                    // 检查时段是否在排班范围内
                    LocalTime scheduleStart = s.getTimeSlotStart();
                    LocalTime scheduleEnd = s.getTimeSlotEnd();
                    return !timeSlotStart.isBefore(scheduleStart) && 
                           !timeSlotEnd.isAfter(scheduleEnd);
                })
                .findFirst()
                .orElse(null);

        if (matchedSchedule == null) {
            throw new BusinessException("该医生在选定时间段没有排班，请选择其他时间");
        }

        // 检查是否停诊
        if (matchedSchedule.getStatus() != null && matchedSchedule.getStatus() == 0) {
            throw new BusinessException("该医生在选定时间段已停诊，请选择其他时间");
        }

        // 检查号源是否已满
        if (matchedSchedule.getRemainingQuota() != null && matchedSchedule.getRemainingQuota() <= 0) {
            throw new BusinessException("该时间段号源已满，请选择其他时间");
        }
    }

    /**
     * 为Registration设置显示名称
     */
    private void enrichRegistration(Registration registration) {
        registration.setVisitTypeName(DictConstants.getVisitTypeName(registration.getVisitType()));
        registration.setStatusName(getStatusName(registration.getStatus()));
    }

    /**
     * 获取状态名称
     * 1已挂号 2已签到 3就诊中 4已完成 5已取消 6已退号 7过号
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "已挂号";
            case 2: return "已签到";
            case 3: return "就诊中";
            case 4: return "已完成";
            case 5: return "已取消";
            case 6: return "已退号";
            case 7: return "过号";
            default: return "未知";
        }
    }

    @Override
    public Map<String, Object> listByUserId(Long userId, Long clinicId, Integer status, Integer pageNum, Integer pageSize) {
        // 1. 查询该用户的所有就诊人ID
        LambdaQueryWrapper<Patient> patientQuery = new LambdaQueryWrapper<>();
        patientQuery.eq(Patient::getUserId, userId);
        patientQuery.eq(Patient::getDeleted, 0);
        List<Patient> patients = patientMapper.selectList(patientQuery);
        
        if (patients.isEmpty()) {
            Map<String, Object> emptyData = new HashMap<>();
            emptyData.put("list", new ArrayList<>());
            emptyData.put("total", 0);
            return emptyData;
        }
        
        List<Long> patientIds = patients.stream()
                .map(Patient::getId)
                .collect(Collectors.toList());
        
        // 2. 查询这些就诊人的挂号记录
        LambdaQueryWrapper<Registration> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Registration::getPatientId, patientIds);
        queryWrapper.eq(Registration::getDeleted, 0);
        
        if (clinicId != null) {
            queryWrapper.eq(Registration::getClinicId, clinicId);
        }
        
        if (status != null) {
            queryWrapper.eq(Registration::getStatus, status);
        }
        
        queryWrapper.orderByDesc(Registration::getCreatedAt);
        
        Page<Registration> page = new Page<>(pageNum, pageSize);
        Page<Registration> resultPage = registrationMapper.selectPage(page, queryWrapper);
        
        resultPage.getRecords().forEach(this::enrichRegistration);
        
        // 3. 为排队中的挂号记录查询实时排队信息
        resultPage.getRecords().forEach(reg -> {
            if (reg.getStatus() != null && reg.getStatus() == 1) { // 1=已挂号（排队中）
                // 查询该医生当前叫号
                Integer currentNumber = registrationMapper.selectCurrentNumber(
                        reg.getDoctorId(), reg.getRegDate());
                reg.setCurrentNumber(currentNumber != null ? currentNumber : 0);
                
                // 查询前方等待人数
                Integer aheadCount = registrationMapper.countAheadInQueue(
                        reg.getDoctorId(), reg.getRegDate(), reg.getQueueNo());
                reg.setAheadCount(aheadCount != null ? aheadCount : 0);
                
                // 计算预计等待时间（按每人10分钟计算）
                reg.setExpectedWaitTime(reg.getAheadCount() * 10);
            }
        });
        
        Map<String, Object> data = new HashMap<>();
        data.put("list", resultPage.getRecords());
        data.put("total", resultPage.getTotal());
        return data;
    }

    @Override
    public QueueInfoVO getQueueInfo(Long registrationId) {
        // 查询挂号信息
        Registration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException("挂号记录不存在");
        }

        QueueInfoVO queueInfo = new QueueInfoVO();
        queueInfo.setQueueNumber(registration.getQueueNo());
        queueInfo.setDoctorName(registration.getDoctorName());
        queueInfo.setDeptName(registration.getDeptName());
        queueInfo.setRegDate(registration.getRegDate().toString());
        queueInfo.setRegTime(registration.getRegTime());
        queueInfo.setStatus(registration.getStatus());

        // 查询当前叫号（该医生今天就诊中或已完成的最后一个号）
        Integer currentNumber = registrationMapper.selectCurrentNumber(
                registration.getDoctorId(), registration.getRegDate());
        queueInfo.setCurrentNumber(currentNumber);

        // 如果当前号大于等于我的号，说明已经过号或正在就诊
        if (currentNumber >= registration.getQueueNo()) {
            queueInfo.setAheadCount(0);
            queueInfo.setEstimatedWaitTime(0);
            // 更新状态为就诊中或已完成
            if (currentNumber.equals(registration.getQueueNo())) {
                queueInfo.setStatus(3); // 就诊中
            } else {
                queueInfo.setStatus(4); // 已完成
            }
        } else {
            // 统计前面还有多少人在排队
            Integer aheadCount = registrationMapper.countAheadInQueue(
                    registration.getDoctorId(), 
                    registration.getRegDate(), 
                    registration.getQueueNo());
            queueInfo.setAheadCount(aheadCount);

            // 估算等待时间：假设每人平均10分钟
            int estimatedMinutes = aheadCount * 10;
            queueInfo.setEstimatedWaitTime(estimatedMinutes);
        }

        return queueInfo;
    }
}
