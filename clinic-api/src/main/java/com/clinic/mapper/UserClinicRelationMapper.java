package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.UserClinicRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserClinicRelationMapper extends BaseMapper<UserClinicRelation> {

    /**
     * 查询用户关联的所有诊所
     */
    @Select("SELECT * FROM user_clinic_relation " +
            "WHERE user_id = #{userId} AND user_type = #{userType} " +
            "AND status = 1 AND deleted = 0")
    List<UserClinicRelation> selectByUserId(@Param("userId") Long userId, 
                                            @Param("userType") Integer userType);

    /**
     * 查询用户的默认诊所
     */
    @Select("SELECT * FROM user_clinic_relation " +
            "WHERE user_id = #{userId} AND user_type = #{userType} " +
            "AND is_default = 1 AND status = 1 AND deleted = 0 " +
            "LIMIT 1")
    UserClinicRelation selectDefaultClinic(@Param("userId") Long userId,
                                           @Param("userType") Integer userType);

    /**
     * 检查用户是否有诊所权限
     */
    @Select("SELECT COUNT(*) FROM user_clinic_relation " +
            "WHERE user_id = #{userId} AND user_type = #{userType} " +
            "AND clinic_id = #{clinicId} AND status = 1 AND deleted = 0")
    int checkUserClinicAccess(@Param("userId") Long userId,
                              @Param("userType") Integer userType,
                              @Param("clinicId") Long clinicId);
}
