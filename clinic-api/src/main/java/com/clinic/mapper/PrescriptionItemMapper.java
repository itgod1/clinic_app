package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.PrescriptionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrescriptionItemMapper extends BaseMapper<PrescriptionItem> {

    @Select("SELECT * FROM prescription_item WHERE prescription_id = #{prescriptionId} AND deleted = 0")
    List<PrescriptionItem> selectByPrescriptionId(@Param("prescriptionId") Long prescriptionId);
}
