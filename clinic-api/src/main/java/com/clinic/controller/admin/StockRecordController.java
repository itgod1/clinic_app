package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.service.StockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 库存出入库记录控制器
 */
@Api(tags = "药库管理-出入库记录")
@RestController
@RequestMapping("/admin/stock")
@RequiredArgsConstructor
public class StockRecordController {

    private final StockService stockService;

    @ApiOperation("药品入库")
    @OperationLog(module = "药库管理-出入库", type = OperationType.IMPORT, desc = "药品入库")
    @PostMapping("/in")
    public Result<String> stockIn(@RequestBody Map<String, Object> params) {
        Long itemId = Long.valueOf(params.get("itemId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        String batchNo = params.get("batchNo") != null ? params.get("batchNo").toString() : null;
        String supplier = params.get("supplier") != null ? params.get("supplier").toString() : null;
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;

        stockService.stockIn(itemId, quantity);
        return Result.success("入库成功");
    }

    @ApiOperation("药品出库")
    @OperationLog(module = "药库管理-出入库", type = OperationType.UPDATE, desc = "药品出库")
    @PostMapping("/out")
    public Result<String> stockOut(@RequestBody Map<String, Object> params) {
        Long itemId = Long.valueOf(params.get("itemId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());
        String remark = params.get("remark") != null ? params.get("remark").toString() : null;

        stockService.stockOut(itemId, quantity);
        return Result.success("出库成功");
    }

    @ApiOperation("入库记录列表")
    @GetMapping("/inList")
    public Result<Map<String, Object>> inList(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 这里应该调用service查询入库记录
        Map<String, Object> data = stockService.getStockInList(clinicId, startDate, endDate, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("出库记录列表")
    @GetMapping("/outList")
    public Result<Map<String, Object>> outList(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        // 这里应该调用service查询出库记录
        Map<String, Object> data = stockService.getStockOutList(clinicId, startDate, endDate, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("库存盘点")
    @OperationLog(module = "药库管理", type = OperationType.UPDATE, desc = "库存盘点")
    @PostMapping("/check")
    public Result<String> stockCheck(@RequestBody Map<String, Object> params) {
        // 实现库存盘点逻辑
        return Result.success("盘点成功");
    }
}
