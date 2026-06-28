package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.common.util.SecurityUtils;
import com.clinic.entity.Clinic;
import com.clinic.entity.SysRole;
import com.clinic.entity.SysUser;
import com.clinic.entity.SysUserRole;
import com.clinic.mapper.ClinicMapper;
import com.clinic.mapper.SysRoleMapper;
import com.clinic.mapper.SysUserMapper;
import com.clinic.mapper.SysUserRoleMapper;
import com.clinic.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ClinicMapper clinicMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<SysUser> list(Long clinicId, Long roleId, String keyword, Integer status, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getClinicId, clinicId);
        queryWrapper.eq(SysUser::getDeleted, 0);

        if (roleId != null) {
            queryWrapper.eq(SysUser::getRoleId, roleId);
        }

        if (status != null) {
            queryWrapper.eq(SysUser::getStatus, status);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }

        queryWrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> result = sysUserMapper.selectPage(page, queryWrapper);

        // 填充诊所名称和角色名称
        fillClinicAndRoleNames(result.getRecords());

        return result;
    }

    /**
     * 填充用户的诊所名称和角色名称
     */
    private void fillClinicAndRoleNames(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        // 获取所有诊所ID和角色ID（过滤掉null）
        List<Long> clinicIds = users.stream()
                .map(SysUser::getClinicId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        List<Long> roleIds = users.stream()
                .map(SysUser::getRoleId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        log.debug("需要查询的诊所ID: {}, 角色ID: {}", clinicIds, roleIds);

        // 批量查询诊所信息
        Map<Long, String> clinicNameMap = Map.of();
        if (!clinicIds.isEmpty()) {
            List<Clinic> clinics = clinicMapper.selectBatchIds(clinicIds);
            log.debug("查询到的诊所: {}", clinics.stream().map(c -> c.getId() + ":" + c.getClinicName()).collect(Collectors.toList()));
            clinicNameMap = clinics.stream()
                    .collect(Collectors.toMap(Clinic::getId, Clinic::getClinicName, (k1, k2) -> k1));
        }

        // 批量查询角色信息
        Map<Long, String> roleNameMap = Map.of();
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            roleNameMap = roles.stream()
                    .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (k1, k2) -> k1));
        }

        // 填充名称
        for (SysUser user : users) {
            String clinicName = clinicNameMap.get(user.getClinicId());
            String roleName = roleNameMap.get(user.getRoleId());
            
            user.setClinicName(clinicName != null ? clinicName : "-");
            user.setRoleName(roleName != null ? roleName : "-");
            
            log.debug("用户: {}, clinicId: {}, clinicName: {}", user.getUsername(), user.getClinicId(), user.getClinicName());
        }
    }

    @Override
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(SysUser user) {
        // 校验用户名是否已存在
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, user.getUsername());
        queryWrapper.eq(SysUser::getDeleted, 0);
        if (sysUserMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 设置默认密码
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword("123456");
        }

        // 密码加密（使用标准 BCrypt，不手动加盐，BCrypt 内部已包含随机盐值）
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setSalt(null); // BCrypt 不需要额外存储盐值

        user.setStatus(user.getStatus() != null ? user.getStatus() : 1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        sysUserMapper.insert(user);
    }

    @Override
    public void update(SysUser user) {
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    public void delete(Long userId) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setDeleted(1);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        SysUser existUser = sysUserMapper.selectById(userId);
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 使用标准 BCrypt 加密密码
        String encodedPassword = passwordEncoder.encode(newPassword);

        SysUser user = new SysUser();
        user.setId(userId);
        user.setPassword(encodedPassword);
        user.setSalt(null);
        user.setUpdatedAt(LocalDateTime.now());
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 校验用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 删除原有角色关联
        sysUserRoleMapper.deleteByUserId(userId);

        // 添加新的角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    public boolean checkUsernameExists(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        queryWrapper.eq(SysUser::getDeleted, 0);
        return sysUserMapper.selectCount(queryWrapper) > 0;
    }
}
