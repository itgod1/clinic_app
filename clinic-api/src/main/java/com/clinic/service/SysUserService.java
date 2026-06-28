package com.clinic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.SysUser;

import java.util.List;

public interface SysUserService {

    Page<SysUser> list(Long clinicId, Long roleId, String keyword, Integer status, Integer pageNum, Integer pageSize);

    SysUser getById(Long id);

    void create(SysUser user);

    void update(SysUser user);

    void delete(Long userId);

    void resetPassword(Long userId, String newPassword);

    /**
     * 分配用户角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return true-已存在
     */
    boolean checkUsernameExists(String username);
}
