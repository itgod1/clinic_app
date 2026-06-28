package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.InsurancePatient;
import com.clinic.service.InsurancePatientService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "医保患者管理")
@RestController
@RequestMapping("/admin/insurance/patient")
@RequiredArgsConstructor
public class InsurancePatientController {

    private final InsurancePatientService insurancePatientService;

    @ApiOperation("获取医保患者列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String insuranceType,
            @RequestParam(required = false) String insuranceStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = insurancePatientService.list(clinicId, keyword, insuranceType, insuranceStatus, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取医保患者详情")
    @GetMapping("/{id}")
    public Result<InsurancePatient> getById(@PathVariable Long id) {
        InsurancePatient record = insurancePatientService.getById(id);
        if (record == null) {
            return Result.badRequest("记录不存在");
        }
        return Result.success(record);
    }

    @ApiOperation("创建医保患者")
    @OperationLog(module = "医保患者管理", type = OperationType.CREATE, desc = "创建医保患者信息")
    @PostMapping("/create")
    public Result<String> create(@RequestBody InsurancePatient patient) {
        insurancePatientService.validateUnique(patient);
        insurancePatientService.create(patient);
        return Result.success("创建成功");
    }

    @ApiOperation("更新医保患者")
    @OperationLog(module = "医保患者管理", type = OperationType.UPDATE, desc = "更新医保患者信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody InsurancePatient patient) {
        insurancePatientService.update(patient);
        return Result.success("更新成功");
    }

    @ApiOperation("删除医保患者")
    @OperationLog(module = "医保患者管理", type = OperationType.DELETE, desc = "删除医保患者信息")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        insurancePatientService.delete(id);
        return Result.success("删除成功");
    }
}
