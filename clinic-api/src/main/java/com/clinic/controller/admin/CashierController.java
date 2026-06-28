package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.service.CashierService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "收费管理")
@RestController
@RequestMapping("/admin/cashier")
@RequiredArgsConstructor
public class CashierController {

    private final CashierService cashierService;

    @ApiOperation("获取待缴费列表")
    @GetMapping("/unpaid")
    public Result<Map<String, Object>> unpaidList(
            @RequestParam Long clinicId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = cashierService.unpaidList(clinicId, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("收费")
    @OperationLog(module = "收费管理", type = OperationType.PAY, desc = "收费结算")
    @PostMapping("/pay")
    public Result<Map<String, Object>> pay(@RequestBody Map<String, Object> params) {
        Object orderIdObj = params.get("orderId");
        Object paymentMethodObj = params.get("paymentMethod");

        if (orderIdObj == null) {
            return Result.error(400, "orderId不能为空");
        }
        if (paymentMethodObj == null) {
            return Result.error(400, "paymentMethod不能为空");
        }

        String orderId = orderIdObj.toString();
        Integer paymentMethod = Integer.valueOf(paymentMethodObj.toString());

        Long paymentId = cashierService.pay(orderId, paymentMethod);

        Map<String, Object> data = new HashMap<>();
        data.put("message", "收费成功");
        data.put("paymentId", paymentId);
        return Result.success(data);
    }

    @ApiOperation("退费")
    @OperationLog(module = "收费管理", type = OperationType.REFUND, desc = "退费处理")
    @PostMapping("/refund")
    public Result<String> refund(@RequestBody Map<String, Object> params) {
        Long orderId = Long.valueOf(params.get("orderId").toString());

        cashierService.refund(orderId);
        return Result.success("退费成功");
    }

    @ApiOperation("优惠")
    @OperationLog(module = "收费管理", type = OperationType.UPDATE, desc = "优惠减免")
    @PostMapping("/discount")
    public Result<String> discount(@RequestBody Map<String, Object> params) {
        Long orderId = Long.valueOf(params.get("orderId").toString());
        BigDecimal discountAmount = new BigDecimal(params.get("discountAmount").toString());

        cashierService.discount(orderId, discountAmount);
        return Result.success("优惠成功");
    }
}
