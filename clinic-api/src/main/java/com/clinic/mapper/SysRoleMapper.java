package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色列表
     */
    @Select("SELECT r.* FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 查询诊所默认角色
     */
    @Select("SELECT * FROM sys_role WHERE clinic_id = #{clinicId} AND deleted = 0 ORDER BY sort_order")
    List<SysRole> selectByClinicId(@Param("clinicId") Long clinicId);

    /**
     * 查询系统默认角色（clinic_id = 0）
     */
    @Select("SELECT * FROM sys_role WHERE clinic_id = 0 AND deleted = 0 ORDER BY sort_order")
    List<SysRole> selectDefaultRoles();
}
