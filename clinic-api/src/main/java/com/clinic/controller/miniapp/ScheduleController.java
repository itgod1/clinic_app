package com.clinic.controller.miniapp;

import com.clinic.common.result.Result;
import com.clinic.entity.Schedule;
import com.clinic.service.ScheduleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序 - 排班相关接口
 */
@Api(tags = "小程序-排班管理")
@RestController("miniappScheduleController")
@RequestMapping("/miniapp")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 获取医生排班列表（公开接口）
     */
    @ApiOperation("获取医生排班")
    @GetMapping("/schedules")
    public Result<List<Schedule>> getDoctorSchedule(
            @ApiParam("诊所ID") @RequestParam(required = false) Long clinicId,
            @ApiParam("医生ID") @RequestParam Long doctorId,
            @ApiParam("开始日期") @RequestParam String startDate,
            @ApiParam("结束日期") @RequestParam String endDate) {

        List<Schedule> list = scheduleService.list(clinicId, doctorId, startDate, endDate);
        return Result.success(list);
    }
}
