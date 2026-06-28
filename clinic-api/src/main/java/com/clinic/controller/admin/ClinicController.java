package com.clinic.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.PageResult;
import com.clinic.common.result.Result;
import com.clinic.entity.Clinic;
import com.clinic.service.ClinicService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 诊所管理控制器
 * 仅超级管理员可访问
 */
@Api(tags = "租户管理-诊所管理")
@RestController
@RequestMapping("/admin/clinic")
@RequiredArgsConstructor
@Validated
public class ClinicController {

    private final ClinicService clinicService;

    @ApiOperation("获取诊所列表")
    @GetMapping("/list")
    public Result<PageResult<Clinic>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<Clinic> page = clinicService.list(keyword, status, pageNum, pageSize);

        PageResult<Clinic> result = new PageResult<>();
        result.setList(page.getRecords());
        result.setTotal(page.getTotal());
       /* result.setPageNum(pageNum);
        result.setPageSize(pageSize);*/

        return Result.success(result);
    }

    @ApiOperation("获取所有营业中的诊所")
    @GetMapping("/activeList")
    public Result<List<Clinic>> activeList() {
        List<Clinic> list = clinicService.getActiveClinics();
        return Result.success(list);
    }

    @ApiOperation("获取诊所详情")
    @GetMapping("/{id}")
    public Result<Clinic> getById(@PathVariable @NotNull(message = "诊所ID不能为空") Long id) {
        Clinic clinic = clinicService.getById(id);
        if (clinic == null) {
            return Result.error(404, "诊所不存在");
        }
        return Result.success(clinic);
    }

    @ApiOperation("创建诊所")
    @OperationLog(module = "租户管理-诊所管理", type = OperationType.CREATE, desc = "创建新诊所")
    @PostMapping("/create")
    public Result<String> create(@RequestBody @Valid Clinic clinic) {
        clinicService.create(clinic);
        return Result.success("创建成功");
    }

    @ApiOperation("更新诊所")
    @OperationLog(module = "租户管理-诊所管理", type = OperationType.UPDATE, desc = "更新诊所信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody @Valid Clinic clinic) {
        if (clinic.getId() == null) {
            return Result.error(400, "诊所ID不能为空");
        }
        clinicService.update(clinic);
        return Result.success("更新成功");
    }

    @ApiOperation("删除诊所")
    @OperationLog(module = "租户管理-诊所管理", type = OperationType.DELETE, desc = "删除诊所")
    @PostMapping("/delete")
    public Result<String> delete(@RequestBody Map<String, Long> params) {
        Long id = params.get("id");
        if (id == null) {
            return Result.error(400, "诊所ID不能为空");
        }
        clinicService.delete(id);
        return Result.success("删除成功");
    }

    @ApiOperation("检查诊所编码是否存在")
    @GetMapping("/checkCode")
    public Result<Boolean> checkCode(@RequestParam String clinicCode) {
        boolean exists = clinicService.checkCodeExists(clinicCode);
        return Result.success(exists);
    }
}
