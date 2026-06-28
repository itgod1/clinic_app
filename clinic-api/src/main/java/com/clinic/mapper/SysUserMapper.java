package com.clinic.mapper;

import com.clinic.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据条件查询用户列表（包含诊所名称）
     */
    @Select("SELECT u.*, c.clinic_name as clinicName, r.role_name as roleName " +
            "FROM sys_user u " +
            "LEFT JOIN clinic c ON u.clinic_id = c.id " +
            "LEFT JOIN sys_role r ON u.role_id = r.id " +
            "WHERE u.deleted = 0 " +
            "<if test='clinicId != null'> AND u.clinic_id = #{clinicId} </if> " +
            "<if test='roleId != null'> AND u.role_id = #{roleId} </if> " +
            "<if test='status != null'> AND u.status = #{status} </if> " +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (u.username LIKE CONCAT('%',#{keyword},'%') OR u.real_name LIKE CONCAT('%',#{keyword},'%') OR u.phone LIKE CONCAT('%',#{keyword},'%')) " +
            "</if> " +
            "ORDER BY u.created_at DESC")
    List<SysUser> selectUserListWithClinic(@Param("clinicId") Long clinicId,
                                           @Param("roleId") Long roleId,
                                           @Param("status") Integer status,
                                           @Param("keyword") String keyword);

    /**
     * 根据ID查询用户详情（包含诊所名称）
     */
    @Select("SELECT u.*, c.clinic_name as clinicName, r.role_name as roleName " +
            "FROM sys_user u " +
            "LEFT JOIN clinic c ON u.clinic_id = c.id " +
            "LEFT JOIN sys_role r ON u.role_id = r.id " +
            "WHERE u.id = #{userId} AND u.deleted = 0")
    SysUser selectUserByIdWithClinic(@Param("userId") Long userId);
}
