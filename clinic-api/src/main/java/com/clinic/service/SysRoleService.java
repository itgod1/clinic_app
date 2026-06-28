package com.clinic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.clinic.entity.SysRole;

import java.util.List;

/**
 * 角色服务接口
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户ID查询角色列表
     */
    List<SysRole> getRolesByUserId(Long userId);

    /**
     * 查询诊所角色列表
     */
    List<SysRole> getRolesByClinicId(Long clinicId);

    /**
     * 分配角色菜单
     */
    void assignRoleMenus(Long roleId, List<Long> menuIds);

    /**
     * 获取角色拥有的菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);
}
