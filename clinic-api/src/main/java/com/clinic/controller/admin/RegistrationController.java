package com.clinic.controller.admin;

import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.Result;
import com.clinic.entity.Registration;
import com.clinic.service.RegistrationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "诊疗管理-挂号管理")
@RestController
@RequestMapping("/admin/registration")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @ApiOperation("获取挂号列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam Long clinicId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String regDate,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Map<String, Object> data = registrationService.list(clinicId, keyword, regDate, startDate, endDate, doctorId, status, pageNum, pageSize);
        return Result.success(data);
    }

    @ApiOperation("获取挂号详情")
    @GetMapping("/{id}")
    public Result<Registration> getById(@PathVariable Long id) {
        Registration registration = registrationService.getById(id);
        if (registration == null) {
            return Result.badRequest("挂号记录不存在");
        }
        return Result.success(registration);
    }

    @ApiOperation("创建挂号")
    @OperationLog(module = "诊疗管理-挂号管理", type = OperationType.CREATE, desc = "创建新挂号")
    @PostMapping("/create")
    public Result<String> create(@RequestBody Registration registration) {
        // 参数校验
        registrationService.validateRegistration(registration);

        registrationService.create(registration);
        return Result.success("挂号成功");
    }

    @ApiOperation("取消挂号")
    @OperationLog(module = "诊疗管理-挂号管理", type = OperationType.UPDATE, desc = "取消挂号")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestBody Map<String, Object> params) {
        Object id = params.get("id");
        if (id == null) {
            return Result.badRequest("挂号ID不能为空");
        }

        Long idLong = Long.valueOf(id.toString());
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;

        registrationService.cancel(idLong, reason);
        return Result.success("取消成功");
    }

    @ApiOperation("更新挂号状态")
    @OperationLog(module = "诊疗管理-挂号管理", type = OperationType.UPDATE, desc = "更新挂号状态")
    @PostMapping("/updateStatus")
    public Result<String> updateStatus(@RequestBody Registration registration) {
        registrationService.updateStatus(registration);
        return Result.success("状态更新成功");
    }
}
