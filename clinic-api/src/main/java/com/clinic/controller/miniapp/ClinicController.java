package com.clinic.controller.miniapp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.result.Result;
import com.clinic.entity.Clinic;
import com.clinic.security.UserContext;
import com.clinic.service.ClinicService;
import com.clinic.service.DepartmentService;
import com.clinic.service.MiniappUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 小程序 - 诊所相关接口
 */
@Api(tags = "小程序-诊所管理")
@RestController("miniappClinicController")
@RequestMapping("/miniapp")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;
    private final DepartmentService departmentService;
    private final MiniappUserService miniappUserService;

    /**
     * 获取可用的诊所列表（患者选择用）
     */
    @ApiOperation("获取可用诊所列表")
    @GetMapping("/clinics")
    public Result<Page<Clinic>> getClinicList(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页条数") @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam("搜索关键词") @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<Clinic> queryWrapper = new LambdaQueryWrapper<>();

        // 只查询正常状态的诊所
        queryWrapper.eq(Clinic::getBusinessStatus, 1);

        // 如果有搜索关键词，按名称搜索（过滤掉 "undefined" 字符串）
        if (keyword != null && !keyword.trim().isEmpty() && !"undefined".equals(keyword.trim())) {
            queryWrapper.like(Clinic::getClinicName, keyword.trim());
        }

        // 按创建时间倒序
        queryWrapper.orderByDesc(Clinic::getCreatedAt);

        Page<Clinic> pageParam = new Page<>(page, pageSize);
        Page<Clinic> resultPage = clinicService.page(pageParam, queryWrapper);

        return Result.success(resultPage);
    }

    /**
     * 获取诊所详情
     */
    @ApiOperation("获取诊所详情")
    @GetMapping("/clinics/{id}")
    public Result<Clinic> getClinicDetail(@PathVariable Long id) {
        Clinic clinic = clinicService.getById(id);
        if (clinic == null || clinic.getBusinessStatus() != 1) {
            return Result.error("诊所不存在或已停用");
        }
        return Result.success(clinic);
    }

    /**
     * 获取科室列表（公开接口）
     */
    @ApiOperation("获取科室列表")
    @GetMapping("/departments")
    public Result<Map<String, Object>> getDepartmentList(
            @ApiParam("诊所ID") @RequestParam(required = false) Long clinicId,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页条数") @RequestParam(defaultValue = "100") Integer pageSize) {

        // 使用 DepartmentService.list 方法，它返回 Map
        Map<String, Object> result = departmentService.list(clinicId, null, null, 1, page, pageSize);

        return Result.success(result);
    }

    /**
     * 切换当前诊所
     */
    @ApiOperation("切换当前诊所")
    @PostMapping("/clinic/switch")
    public Result<?> switchClinic(
            @ApiParam("诊所ID") @RequestParam Long clinicId) {

        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        // 检查诊所是否存在且正常营业
        Clinic clinic = clinicService.getById(clinicId);
        if (clinic == null || clinic.getBusinessStatus() != 1) {
            return Result.error("诊所不存在或已停用");
        }

        // 更新用户当前诊所
        boolean success = miniappUserService.switchClinic(userId, clinicId);
        if (success) {
            return Result.success("切换诊所成功", clinic);
        } else {
            return Result.error("切换诊所失败");
        }
    }
}
