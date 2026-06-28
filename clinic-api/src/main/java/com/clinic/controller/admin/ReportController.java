package com.clinic.controller.admin;

import com.clinic.common.result.Result;
import com.clinic.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Api(tags = "统计报表")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @ApiOperation("获取工作台统计数据")
    @GetMapping("/dashboard")
    public Result<?> dashboard(@RequestParam Long clinicId) {
        Map<String, Object> data = reportService.dashboard(clinicId);
        return Result.success(data);
    }

    @ApiOperation("获取日报表")
    @GetMapping("/daily")
    public Result<?> daily(@RequestParam Long clinicId, @RequestParam String date) {
        Map<String, Object> data = reportService.daily(clinicId, date);
        return Result.success(data);
    }

    @ApiOperation("获取月报表")
    @GetMapping("/monthly")
    public Result<?> monthly(@RequestParam Long clinicId, @RequestParam Integer year, @RequestParam Integer month) {
        Map<String, Object> data = reportService.monthly(clinicId, year, month);
        return Result.success(data);
    }

    @ApiOperation("科室排名")
    @GetMapping("/deptRanking")
    public Result<?> deptRanking(@RequestParam Long clinicId, @RequestParam String startDate, @RequestParam String endDate) {
        List<Map<String, Object>> data = reportService.deptRanking(clinicId, startDate, endDate);
        return Result.success(data);
    }

    @ApiOperation("医生业绩")
    @GetMapping("/doctorPerformance")
    public Result<?> doctorPerformance(
            @RequestParam Long clinicId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long doctorId) {
        List<Map<String, Object>> data = reportService.doctorPerformance(clinicId, startDate, endDate, doctorId);
        return Result.success(data);
    }

    @ApiOperation("药品消耗")
    @GetMapping("/medicineConsume")
    public Result<?> medicineConsume(@RequestParam Long clinicId, @RequestParam String startDate, @RequestParam String endDate) {
        List<Map<String, Object>> data = reportService.medicineConsume(clinicId, startDate, endDate);
        return Result.success(data);
    }

    @ApiOperation("调试：查询今日处方统计")
    @GetMapping("/debug/todayPrescription")
    public Result<?> debugTodayPrescription(@RequestParam(required = false) Long clinicId) {
        Map<String, Object> result = reportService.debugTodayPrescription(clinicId);
        return Result.success(result);
    }
}
