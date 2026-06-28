package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.Patient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PatientMapper extends BaseMapper<Patient> {

    /**
     * 根据openid查询患者
     */
    @Select("SELECT * FROM patient WHERE open_id = #{openId} LIMIT 1")
    Patient selectByOpenId(@Param("openId") String openId);
}