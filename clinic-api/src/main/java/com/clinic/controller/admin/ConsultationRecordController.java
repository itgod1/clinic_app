package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.ConsultationRecord;
import com.clinic.service.ConsultationRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "咨询管理-咨询记录")
@RestController
@RequestMapping("/admin/consultation")
@RequiredArgsConstructor
public class ConsultationRecordController {

    private final ConsultationRecordService consultationRecordService;

    @ApiOperation("获取咨询记录列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) Integer recordType,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = consultationRecordService.list(clinicId, recordType, doctorId, startDate, endDate, keyword, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取咨询记录详情")
    @GetMapping("/{id}")
    public Result<ConsultationRecord> getById(@PathVariable Long id) {
        ConsultationRecord record = consultationRecordService.getById(id);
        if (record == null) {
            return Result.badRequest("记录不存在");
        }
        return Result.success(record);
    }

    @ApiOperation("创建咨询记录")
    @OperationLog(module = "咨询管理-咨询记录", type = OperationType.CREATE, desc = "创建咨询记录/设计单")
    @PostMapping("/create")
    public Result<ConsultationRecord> create(@RequestBody ConsultationRecord record) {
        consultationRecordService.create(record);
        return Result.success(record);
    }

    @ApiOperation("更新咨询记录")
    @OperationLog(module = "咨询管理-咨询记录", type = OperationType.UPDATE, desc = "更新咨询记录/设计单")
    @PostMapping("/update")
    public Result<String> update(@RequestBody ConsultationRecord record) {
        consultationRecordService.update(record);
        return Result.success("更新成功");
    }

    @ApiOperation("删除咨询记录")
    @OperationLog(module = "咨询管理-咨询记录", type = OperationType.DELETE, desc = "删除咨询记录/设计单")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        consultationRecordService.delete(id);
        return Result.success("删除成功");
    }

    @ApiOperation("获取工作台复诊提醒")
    @GetMapping("/upcoming-revisits")
    public Result<List<ConsultationRecord>> upcomingRevisits(
            @RequestParam Long clinicId,
            @RequestParam(required = false) Long doctorId) {
        List<ConsultationRecord> data = consultationRecordService.getUpcomingRevisits(clinicId, doctorId);
        return Result.success(data);
    }

    @ApiOperation("标记咨询记录已提醒")
    @PostMapping("/remind/{id}")
    public Result<String> remind(@PathVariable Long id) {
        consultationRecordService.remind(id);
        return Result.success("标记成功");
    }

    @ApiOperation("标记咨询记录已完成")
    @PostMapping("/complete/{id}")
    public Result<String> complete(@PathVariable Long id) {
        consultationRecordService.complete(id);
        return Result.success("标记成功");
    }
}
