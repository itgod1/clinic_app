package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.Prescription;
import com.clinic.service.PrescriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "诊疗管理-处方管理")
@RestController
@RequestMapping("/admin/prescription")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @ApiOperation("获取处方列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Integer prescriptionType,
            @RequestParam(required = false) Integer paymentStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = prescriptionService.list(clinicId, doctorId, prescriptionType, paymentStatus, startDate, endDate, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取处方详情")
    @GetMapping("/{id}")
    public Result<Prescription> getById(@PathVariable Long id) {
        Prescription prescription = prescriptionService.getById(id);
        if (prescription == null) {
            return Result.badRequest("处方不存在");
        }
        return Result.success(prescription);
    }

    @ApiOperation("创建处方")
    @OperationLog(module = "诊疗管理-处方管理", type = OperationType.CREATE, desc = "创建新处方")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Prescription prescription) {
        // 参数校验
        if (prescription.getPatientId() == null && !StringUtils.hasText(prescription.getPatientName())) {
            return Result.badRequest("患者不能为空");
        }
        if (CollectionUtils.isEmpty(prescription.getItems())) {
            return Result.badRequest("处方项目不能为空");
        }

        Map<String, Object> result = prescriptionService.create(prescription);
        return Result.success(result);
    }

    @ApiOperation("处方退费")
    @OperationLog(module = "诊疗管理-处方管理", type = OperationType.REFUND, desc = "处方退费")
    @PostMapping("/refund")
    public Result<String> refund(@RequestParam Long prescriptionId, @RequestParam(required = false) String reason) {
        prescriptionService.refund(prescriptionId, reason);
        return Result.success("退费成功");
    }

    @ApiOperation("确认发药")
    @OperationLog(module = "诊疗管理-处方管理", type = OperationType.UPDATE, desc = "确认发药")
    @PostMapping("/dispense")
    public Result<String> dispense(@RequestParam Long prescriptionId) {
        prescriptionService.dispense(prescriptionId);
        return Result.success("发药成功");
    }
}
