package com.clinic.controller.miniapp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.result.Result;
import com.clinic.entity.Doctor;
import com.clinic.entity.Schedule;
import com.clinic.service.DoctorService;
import com.clinic.service.ScheduleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 小程序 - 医生相关接口
 */
@Api(tags = "小程序-医生管理")
@RestController("miniappDoctorController")
@RequestMapping("/miniapp")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final ScheduleService scheduleService;

    /**
     * 获取医生列表（公开接口）
     */
    @ApiOperation("获取医生列表")
    @GetMapping("/doctors")
    public Result<Page<Doctor>> getDoctorList(
            @ApiParam("诊所ID") @RequestParam(required = false) Long clinicId,
            @ApiParam("科室ID") @RequestParam(required = false) Long deptId,
            @ApiParam("搜索关键词") @RequestParam(required = false) String keyword,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {

        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询在职状态的医生
        queryWrapper.eq(Doctor::getStatus, 1);

        // 如果指定了诊所ID，按诊所筛选
        if (clinicId != null) {
            queryWrapper.eq(Doctor::getClinicId, clinicId);
        }

        // 如果指定了科室ID，按科室筛选
        if (deptId != null) {
            queryWrapper.eq(Doctor::getDeptId, deptId);
        }

        // 如果有关键词，按医生姓名搜索（过滤掉 "undefined" 字符串）
        if (keyword != null && !keyword.trim().isEmpty() && !"undefined".equals(keyword.trim())) {
            queryWrapper.like(Doctor::getDoctorName, keyword.trim());
        }

        // 按创建时间倒序，优先展示新加入的医生
        queryWrapper.orderByDesc(Doctor::getCreatedAt);

        Page<Doctor> pageParam = new Page<>(page, pageSize);
        Page<Doctor> resultPage = doctorService.list(clinicId, deptId, keyword, 1, page, pageSize);

        // 补充科室名称和排班状态
        List<Doctor> records = resultPage.getRecords();
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String startDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = today.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE);
        
        for (Doctor doctor : records) {
            if (doctor.getDeptId() != null && doctor.getDeptName() == null) {
                doctor.setDeptName("科室" + doctor.getDeptId());
            }
            
            // 查询是否有可预约排班（今天及未来7天，剩余号源>0，且时段未过）
            // 优先使用请求参数中的 clinicId，如果没有则使用医生自身的 clinicId
            Long queryClinicId = clinicId != null ? clinicId : doctor.getClinicId();
            
            boolean hasAvailableSchedule = false;
            if (queryClinicId != null) {
                List<Schedule> schedules = scheduleService.list(
                        queryClinicId, 
                        doctor.getId(), 
                        startDate, 
                        endDate
                );
                hasAvailableSchedule = schedules.stream()
                        .anyMatch(s -> {
                            // 基本状态检查
                            if (s.getRemainingQuota() == null || s.getRemainingQuota() <= 0 
                                    || s.getStatus() == null || s.getStatus() != 1) {
                                return false;
                            }
                            
                            // 检查时段是否已过（只检查今天的排班）
                            if (s.getScheduleDate() != null && s.getTimeSlotEnd() != null) {
                                LocalDate scheduleDate = s.getScheduleDate();
                                if (scheduleDate.equals(today)) {
                                    // 今天的排班，检查结束时间是否已过
                                    LocalTime endTime = s.getTimeSlotEnd();
                                    if (now.toLocalTime().isAfter(endTime)) {
                                        return false; // 时段已过
                                    }
                                }
                            }
                            return true;
                        });
            }
            doctor.setHasSchedule(hasAvailableSchedule);
        }

        return Result.success(resultPage);
    }

    /**
     * 获取医生详情（公开接口）
     */
    @ApiOperation("获取医生详情")
    @GetMapping("/doctors/{id}")
    public Result<Doctor> getDoctorDetail(@PathVariable Long id) {
        Doctor doctor = doctorService.getById(id);
        if (doctor == null || doctor.getStatus() != 1) {
            return Result.error("医生不存在或已停用");
        }
        return Result.success(doctor);
    }
}
