package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.Patient;
import com.clinic.service.PatientService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "患者管理")
@RestController
@RequestMapping("/admin/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @ApiOperation("获取患者列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer memberLevel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = patientService.list(clinicId, keyword, memberLevel, startDate, endDate, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取患者详情")
    @GetMapping("/{id}")
    public Result<Patient> getById(@PathVariable Long id) {
        Patient patient = patientService.getById(id);
        if (patient == null) {
            return Result.badRequest("患者不存在");
        }
        return Result.success(patient);
    }

    @ApiOperation("创建患者")
    @OperationLog(module = "患者管理", type = OperationType.CREATE, desc = "创建新患者")
    @PostMapping("/create")
    public Result<String> create(@RequestBody Patient patient) {
        // 参数校验
        patientService.validatePatient(patient);

        // 业务校验
        patientService.validatePhoneUnique(patient.getPhone(), patient.getClinicId());

        // 执行业务
        patientService.create(patient);
        return Result.success("创建成功");
    }

    @ApiOperation("更新患者")
    @OperationLog(module = "患者管理", type = OperationType.UPDATE, desc = "更新患者信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody Patient patient) {
        patientService.update(patient);
        return Result.success("更新成功");
    }

    @ApiOperation("更新患者状态")
    @OperationLog(module = "患者管理", type = OperationType.UPDATE, desc = "更新患者状态")
    @PostMapping("/updateStatus")
    public Result<String> updateStatus(@RequestBody Patient patient) {
        patientService.updateStatus(patient);
        return Result.success("状态更新成功");
    }

    @ApiOperation("删除患者")
    @OperationLog(module = "患者管理", type = OperationType.DELETE, desc = "删除患者")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long patientId) {
        patientService.delete(patientId);
        return Result.success("删除成功");
    }
}
