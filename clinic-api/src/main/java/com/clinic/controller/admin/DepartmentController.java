package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.Department;
import com.clinic.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "诊所管理-科室管理")
@RestController
@RequestMapping("/admin/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @ApiOperation("获取科室列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) Integer deptType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = departmentService.list(clinicId, deptType, keyword, status, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("创建科室")
    @OperationLog(module = "科室管理", type = OperationType.CREATE, desc = "创建新科室")
    @PostMapping("/create")
    public Result<String> create(@RequestBody Department department) {
        // 业务校验
        departmentService.validateCodeUnique(department.getDeptCode(), department.getClinicId());

        departmentService.create(department);
        return Result.success("创建成功");
    }

    @ApiOperation("更新科室")
    @OperationLog(module = "科室管理", type = OperationType.UPDATE, desc = "更新科室信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody Department department) {
        // 业务校验
        departmentService.validateCodeUnique(department.getDeptCode(), department.getClinicId(), department.getId());

        departmentService.update(department);
        return Result.success("更新成功");
    }

    @ApiOperation("删除科室")
    @OperationLog(module = "科室管理", type = OperationType.DELETE, desc = "删除科室")
    @DeleteMapping("/{deptId}")
    public Result<String> delete(@PathVariable Long deptId) {
        departmentService.delete(deptId);
        return Result.success("删除成功");
    }
}
