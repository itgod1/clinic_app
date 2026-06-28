package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据角色ID查询菜单列表
     */
    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort_order")
    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID列表查询菜单列表
     */
    @Select("SELECT DISTINCT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id IN (${roleIds}) AND m.deleted = 0 AND m.status = 1 " +
            "ORDER BY m.sort_order")
    List<SysMenu> selectMenusByRoleIds(@Param("roleIds") String roleIds);

    /**
     * 查询所有有效菜单
     */
    @Select("SELECT * FROM sys_menu WHERE deleted = 0 AND status = 1 ORDER BY sort_order")
    List<SysMenu> selectAllMenus();

    /**
     * 查询用户权限标识列表
     */
    @Select("SELECT DISTINCT m.permission FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.permission IS NOT NULL AND m.permission != '' " +
            "AND m.deleted = 0 AND m.status = 1")
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);
}
