package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.Clinic;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ClinicMapper extends BaseMapper<Clinic> {
}
