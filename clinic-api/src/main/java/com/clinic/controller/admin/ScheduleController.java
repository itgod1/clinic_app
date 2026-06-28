package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.controller.dto.ScheduleBatchDTO;
import com.clinic.entity.Schedule;
import com.clinic.service.ScheduleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "医生管理-排班管理")
@RestController
@RequestMapping("/admin/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @ApiOperation("获取排班列表")
    @GetMapping("/list")
    public Result<List<Schedule>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        List<Schedule> list = scheduleService.list(clinicId, doctorId, startDate, endDate);
        return Result.success(list);
    }

    @ApiOperation("批量设置排班")
    @OperationLog(module = "排班管理", type = OperationType.UPDATE, desc = "批量设置医生排班")
    @PostMapping("/batchSet")
    public Result<String> batchSet(@Valid @RequestBody ScheduleBatchDTO data) {
        scheduleService.batchSet(data);
        return Result.success("排班设置成功");
    }
}
