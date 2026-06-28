package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.MedicalRecord;
import com.clinic.service.MedicalRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "诊疗管理-病历管理")
@RestController
@RequestMapping("/admin/medicalRecord")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @ApiOperation("获取病历列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = medicalRecordService.list(clinicId, patientId, doctorId, startDate, endDate, keyword, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取病历详情")
    @GetMapping("/{id}")
    public Result<MedicalRecord> getById(@PathVariable Long id) {
        MedicalRecord record = medicalRecordService.getById(id);
        if (record == null) {
            return Result.badRequest("病历不存在");
        }
        return Result.success(record);
    }

    @ApiOperation("创建病历")
    @OperationLog(module = "诊疗管理-病历管理", type = OperationType.CREATE, desc = "创建新病历")
    @PostMapping("/create")
    public Result<MedicalRecord> create(@RequestBody MedicalRecord medicalRecord) {
        // 参数校验
        medicalRecordService.validateMedicalRecord(medicalRecord);

        medicalRecordService.create(medicalRecord);
        return Result.success(medicalRecord);
    }

    @ApiOperation("更新病历")
    @OperationLog(module = "诊疗管理-病历管理", type = OperationType.UPDATE, desc = "更新病历信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody MedicalRecord medicalRecord) {
        medicalRecordService.update(medicalRecord);
        return Result.success("更新成功");
    }

    @ApiOperation("删除病历")
    @OperationLog(module = "诊疗管理-病历管理", type = OperationType.DELETE, desc = "删除病历")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long recordId) {
        medicalRecordService.delete(recordId);
        return Result.success("删除成功");
    }
}
