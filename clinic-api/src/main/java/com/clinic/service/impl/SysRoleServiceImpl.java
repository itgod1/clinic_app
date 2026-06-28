package com.clinic.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.clinic.entity.SysRole;
import com.clinic.entity.SysRoleMenu;
import com.clinic.mapper.SysRoleMapper;
import com.clinic.mapper.SysRoleMenuMapper;
import com.clinic.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        return sysRoleMapper.selectRolesByUserId(userId);
    }

    @Override
    public List<SysRole> getRolesByClinicId(Long clinicId) {
        // 查询系统默认角色 + 诊所自定义角色
        List<SysRole> roles = sysRoleMapper.selectDefaultRoles();
        roles.addAll(sysRoleMapper.selectByClinicId(clinicId));
        return roles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleMenus(Long roleId, List<Long> menuIds) {
        // 删除原有权限
        sysRoleMenuMapper.deleteByRoleId(roleId);

        // 新增权限
        if (menuIds != null && !menuIds.isEmpty()) {
            List<SysRoleMenu> list = menuIds.stream()
                    .map(menuId -> {
                        SysRoleMenu roleMenu = new SysRoleMenu();
                        roleMenu.setRoleId(roleId);
                        roleMenu.setMenuId(menuId);
                        return roleMenu;
                    })
                    .collect(Collectors.toList());
            // 批量插入
            for (SysRoleMenu roleMenu : list) {
                sysRoleMenuMapper.insert(roleMenu);
            }
        }
    }

    @Override
    public List<Long> getRoleMenuIds(Long roleId) {
        // 这里需要查询sys_role_menu表
        // 简化实现，实际应该写SQL查询
        return new ArrayList<>();
    }
}
