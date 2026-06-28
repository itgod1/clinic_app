package com.clinic.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.Doctor;
import com.clinic.service.DoctorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "医生管理")
@RestController
@RequestMapping("/admin/doctor")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;

    @ApiOperation("获取医生列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam Long clinicId,
                          @RequestParam(required = false) Long deptId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status)
    {

        Page<Doctor> list = doctorService.list(clinicId, deptId, keyword, status, 1, 10);
        return Result.success(list);
    }

    @ApiOperation("获取医生详情")
    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {

        return Result.success();
    }

    @ApiOperation("创建医生")
    @OperationLog(module = "医生管理", type = OperationType.CREATE, desc = "创建新医生")
    @PostMapping("/create")
    public Result<?> create(@RequestBody Doctor doctor) {

        doctorService.create( doctor);
        return Result.success();
    }

    @ApiOperation("更新医生")
    @OperationLog(module = "医生管理", type = OperationType.UPDATE, desc = "更新医生信息")
    @PostMapping("/update")
    public Result<?> update(@RequestBody Doctor doctor ) {

        doctorService.update(doctor);
        return Result.success();
    }

    @ApiOperation("删除医生")
    @OperationLog(module = "医生管理", type = OperationType.DELETE, desc = "删除医生")
    @PostMapping("/delete")
    public Result<?> delete(@RequestParam Long doctorId) {
        return Result.success();
    }
}