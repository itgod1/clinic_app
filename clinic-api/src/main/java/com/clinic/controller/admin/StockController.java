package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.StockItem;
import com.clinic.service.StockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Api(tags = "药库管理")
@RestController
@RequestMapping("/admin/item")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @ApiOperation("获取药品列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer itemType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean lowStockAlert,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = stockService.list(clinicId, keyword, itemType, status, lowStockAlert, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取药品详情")
    @GetMapping("/{id}")
    public Result<StockItem> getById(@PathVariable Long id) {
        StockItem item = stockService.getById(id);
        if (item == null) {
            return Result.badRequest("药品不存在");
        }
        return Result.success(item);
    }

    @ApiOperation("创建药品")
    @OperationLog(module = "药库管理", type = OperationType.CREATE, desc = "创建新药品")
    @PostMapping("/create")
    public Result<String> create(@RequestBody StockItem item) {
        // 参数校验
        if (!StringUtils.hasText(item.getItemName())) {
            return Result.badRequest("药品名称不能为空");
        }
        if (!StringUtils.hasText(item.getItemCode())) {
            return Result.badRequest("药品编码不能为空");
        }

        // 业务校验
        stockService.validateItemCodeUnique(item.getItemCode(), item.getClinicId());

        // 执行业务
        stockService.create(item);
        return Result.success("创建成功");
    }

    @ApiOperation("更新药品")
    @OperationLog(module = "药库管理", type = OperationType.UPDATE, desc = "更新药品信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody StockItem item) {
        stockService.update(item);
        return Result.success("更新成功");
    }

    @ApiOperation("更新药品价格")
    @OperationLog(module = "药库管理", type = OperationType.UPDATE, desc = "更新药品价格")
    @PostMapping("/updatePrice")
    public Result<String> updatePrice(@RequestBody Map<String, Object> params) {
        Long itemId = Long.valueOf(params.get("itemId").toString());
        BigDecimal retailPrice = new BigDecimal(params.get("retailPrice").toString());
        BigDecimal memberPrice = new BigDecimal(params.get("memberPrice").toString());

        stockService.updatePrice(itemId, retailPrice, memberPrice);
        return Result.success("价格更新成功");
    }

    @ApiOperation("删除药品")
    @OperationLog(module = "药库管理", type = OperationType.DELETE, desc = "删除药品")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long itemId) {
        stockService.delete(itemId);
        return Result.success("删除成功");
    }

    @ApiOperation("药品入库")
    @OperationLog(module = "药库管理", type = OperationType.IMPORT, desc = "药品入库")
    @PostMapping("/stockIn")
    public Result<String> stockIn(@RequestBody Map<String, Object> params) {
        Long itemId = Long.valueOf(params.get("itemId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());

        stockService.stockIn(itemId, quantity);
        return Result.success("入库成功");
    }

    @ApiOperation("药品出库")
    @OperationLog(module = "药库管理", type = OperationType.UPDATE, desc = "药品出库")
    @PostMapping("/stockOut")
    public Result<String> stockOut(@RequestBody Map<String, Object> params) {
        Long itemId = Long.valueOf(params.get("itemId").toString());
        Integer quantity = Integer.valueOf(params.get("quantity").toString());

        stockService.stockOut(itemId, quantity);
        return Result.success("出库成功");
    }
}
