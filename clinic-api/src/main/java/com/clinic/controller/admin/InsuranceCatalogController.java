package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.InsuranceCatalog;
import com.clinic.service.InsuranceCatalogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "医保目录管理")
@RestController
@RequestMapping("/admin/insurance/catalog")
@RequiredArgsConstructor
public class InsuranceCatalogController {

    private final InsuranceCatalogService insuranceCatalogService;

    @ApiOperation("获取医保目录列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String catalogType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = insuranceCatalogService.list(clinicId, catalogType, keyword, status, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取医保目录详情")
    @GetMapping("/{id}")
    public Result<InsuranceCatalog> getById(@PathVariable Long id) {
        InsuranceCatalog record = insuranceCatalogService.getById(id);
        if (record == null) {
            return Result.badRequest("记录不存在");
        }
        return Result.success(record);
    }

    @ApiOperation("创建医保目录项")
    @OperationLog(module = "医保目录管理", type = OperationType.CREATE, desc = "创建医保目录项")
    @PostMapping("/create")
    public Result<String> create(@RequestBody InsuranceCatalog catalog) {
        insuranceCatalogService.create(catalog);
        return Result.success("创建成功");
    }

    @ApiOperation("更新医保目录项")
    @OperationLog(module = "医保目录管理", type = OperationType.UPDATE, desc = "更新医保目录项")
    @PostMapping("/update")
    public Result<String> update(@RequestBody InsuranceCatalog catalog) {
        insuranceCatalogService.update(catalog);
        return Result.success("更新成功");
    }

    @ApiOperation("删除医保目录项")
    @OperationLog(module = "医保目录管理", type = OperationType.DELETE, desc = "删除医保目录项")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam Long id) {
        insuranceCatalogService.delete(id);
        return Result.success("删除成功");
    }
}
