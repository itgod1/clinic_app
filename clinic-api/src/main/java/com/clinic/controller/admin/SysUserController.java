package com.clinic.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.annotation.OperationLog;
import com.clinic.common.constant.OperationType;
import com.clinic.common.result.PageResult;
import com.clinic.common.result.Result;
import com.clinic.common.util.SecurityUtils;
import com.clinic.controller.dto.SysUserDTO;
import com.clinic.entity.SysUser;
import com.clinic.service.SysRoleService;
import com.clinic.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "系统管理-用户管理")
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@Validated
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final PasswordEncoder passwordEncoder;

    @ApiOperation("获取用户列表")
    @GetMapping("/list")
    public Result<PageResult<SysUserDTO>> list(
            @RequestParam @NotNull(message = "诊所ID不能为空") Long clinicId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        Page<SysUser> page = sysUserService.list(clinicId, roleId, keyword, status, pageNum, pageSize);

        List<SysUserDTO> dtoList = page.getRecords().stream().map(user -> {
            SysUserDTO dto = new SysUserDTO();
            BeanUtils.copyProperties(user, dto);
            // 显式复制非数据库字段
            dto.setClinicName(user.getClinicName());
            dto.setRoleName(user.getRoleName());
            // 密码和盐值不返回
            dto.setPassword(null);
            dto.setSalt(null);
            return dto;
        }).collect(Collectors.toList());

        PageResult<SysUserDTO> result = new PageResult<>();
        result.setList(dtoList);
        result.setTotal(page.getTotal());
        /*result.setPageNum(pageNum);
        result.setPageSize(pageSize);*/

        return Result.success(result);
    }

    @ApiOperation("获取用户详情")
    @GetMapping("/{id}")
    public Result<SysUserDTO> getById(@PathVariable @NotNull(message = "用户ID不能为空") Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        SysUserDTO dto = new SysUserDTO();
        BeanUtils.copyProperties(user, dto);
        // 显式复制非数据库字段
        dto.setClinicName(user.getClinicName());
        dto.setRoleName(user.getRoleName());
        // 密码和盐值不返回
        dto.setPassword(null);
        dto.setSalt(null);

        return Result.success(dto);
    }

    @ApiOperation("创建用户")
    @OperationLog(module = "系统管理-用户管理", type = OperationType.CREATE, desc = "创建新用户")
    @PostMapping("/create")
    public Result<String> create(@RequestBody @Valid SysUserDTO dto) {
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);

        sysUserService.create(user);
        return Result.success("创建成功");
    }

    @ApiOperation("更新用户")
    @OperationLog(module = "系统管理-用户管理", type = OperationType.UPDATE, desc = "更新用户信息")
    @PostMapping("/update")
    public Result<String> update(@RequestBody @Valid SysUserDTO dto) {
        if (dto.getId() == null) {
            return Result.error(400, "用户ID不能为空");
        }

        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);

        sysUserService.update(user);
        return Result.success("更新成功");
    }

    @ApiOperation("删除用户")
    @OperationLog(module = "系统管理-用户管理", type = OperationType.DELETE, desc = "删除用户")
    @PostMapping("/delete")
    public Result<String> delete(@RequestParam @NotNull(message = "用户ID不能为空") Long userId) {
        sysUserService.delete(userId);
        return Result.success("删除成功");
    }

    @ApiOperation("重置密码")
    @OperationLog(module = "系统管理-用户管理", type = OperationType.UPDATE, desc = "重置用户密码")
    @PostMapping("/resetPassword")
    public Result<String> resetPassword(@RequestBody Map<String, Object> params) {
        Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
        String newPassword = params.get("newPassword") != null ? params.get("newPassword").toString() : null;

        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }

        sysUserService.resetPassword(userId, newPassword);
        return Result.success("密码重置成功");
    }

    @ApiOperation("修改密码")
    @PostMapping("/changePassword")
    public Result<String> changePassword(@RequestBody Map<String, Object> params) {
        String oldPassword = params.get("oldPassword") != null ? params.get("oldPassword").toString() : null;
        String newPassword = params.get("newPassword") != null ? params.get("newPassword").toString() : null;

        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error(400, "原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error(400, "新密码不能为空");
        }

        // 获取当前登录用户ID
        Long userId = SecurityUtils.getCurrentUserId();
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 验证原密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(400, "原密码错误");
        }

        // 修改密码
        sysUserService.resetPassword(userId, newPassword);
        return Result.success("密码修改成功");
    }

    @ApiOperation("获取用户角色列表")
    @GetMapping("/roles/{userId}")
    public Result<?> getUserRoles(@PathVariable @NotNull(message = "用户ID不能为空") Long userId) {
        List<?> roles = sysRoleService.getRolesByUserId(userId);
        return Result.success(roles);
    }

    @ApiOperation("分配用户角色")
    @OperationLog(module = "系统管理-用户管理", type = OperationType.UPDATE, desc = "分配用户角色")
    @PostMapping("/assignRoles")
    @SuppressWarnings("unchecked")
    public Result<String> assignRoles(@RequestBody Map<String, Object> params) {
        Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
        List<Long> roleIds = params.get("roleIds") != null ? (List<Long>) params.get("roleIds") : null;

        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }

        sysUserService.assignRoles(userId, roleIds);
        return Result.success("角色分配成功");
    }

    @ApiOperation("修改用户状态")
    @OperationLog(module = "系统管理-用户管理", type = OperationType.UPDATE, desc = "修改用户状态")
    @PostMapping("/changeStatus")
    public Result<String> changeStatus(@RequestBody Map<String, Object> params) {
        Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;

        if (userId == null) {
            return Result.error(400, "用户ID不能为空");
        }
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态值无效");
        }

        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        sysUserService.update(user);

        return Result.success("状态修改成功");
    }
}
