package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.dto.InsuranceSettlementRequest;
import com.clinic.entity.InsuranceSettlement;
import com.clinic.service.InsuranceSettlementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "医保结算管理")
@RestController
@RequestMapping("/admin/insurance/settlement")
@RequiredArgsConstructor
public class InsuranceSettlementController {

    private final InsuranceSettlementService insuranceSettlementService;

    @ApiOperation("获取医保结算列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String settlementStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = insuranceSettlementService.list(clinicId, settlementStatus, keyword, startDate, endDate, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取结算详情（含明细）")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable Long id) {
        Map<String, Object> data = insuranceSettlementService.getDetail(id);
        return Result.success(data);
    }

    @ApiOperation("创建结算预览（费用计算）")
    @OperationLog(module = "医保结算管理", type = OperationType.QUERY, desc = "医保结算费用预览")
    @PostMapping("/preview")
    public Result<Map<String, Object>> createPreview(@RequestBody InsuranceSettlementRequest request) {
        Map<String, Object> data = insuranceSettlementService.createPreview(request);
        return Result.success(data);
    }

    @ApiOperation("提交医保结算")
    @OperationLog(module = "医保结算管理", type = OperationType.INSURANCE_SETTLE, desc = "提交医保结算")
    @PostMapping("/submit/{id}")
    public Result<InsuranceSettlement> submit(@PathVariable Long id) {
        InsuranceSettlement settlement = insuranceSettlementService.submit(id);
        return Result.success("结算提交成功", settlement);
    }

    @ApiOperation("取消医保结算")
    @OperationLog(module = "医保结算管理", type = OperationType.UPDATE, desc = "取消医保结算")
    @PostMapping("/cancel/{id}")
    public Result<String> cancel(@PathVariable Long id, @RequestParam String reason) {
        insuranceSettlementService.cancel(id, reason);
        return Result.success("取消成功");
    }

    @ApiOperation("从医保平台同步状态")
    @GetMapping("/query-platform/{id}")
    public Result<Map<String, Object>> queryFromPlatform(@PathVariable Long id) {
        Map<String, Object> data = insuranceSettlementService.queryFromPlatform(id);
        return Result.success(data);
    }
}
