package com.clinic.controller.miniapp;

import com.clinic.common.result.Result;
import com.clinic.common.util.SecurityUtils;
import com.clinic.entity.Prescription;
import com.clinic.service.PrescriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 小程序 - 药单相关接口
 */
@Api(tags = "小程序-药单管理")
@RestController("miniappPrescriptionController")
@RequestMapping("/miniapp")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * 获取当前用户的药单列表
     */
    @ApiOperation("获取当前用户的药单列表")
    @GetMapping("/prescriptions")
    public Result<Map<String, Object>> myList(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.badRequest("用户未登录");
        }

        Map<String, Object> data = prescriptionService.listForUser(userId, pageNum, pageSize);
        return Result.success(data);
    }

    /**
     * 获取药单详情（含药品明细与用法）
     */
    @ApiOperation("获取药单详情")
    @GetMapping("/prescriptions/{id}")
    public Result<Prescription> detail(
            @ApiParam("药单ID") @PathVariable Long id) {

        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.badRequest("用户未登录");
        }

        Prescription prescription = prescriptionService.getForUser(userId, id);
        return Result.success(prescription);
    }
}
