package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.Registration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    /**
     * 查询医生当前叫号（就诊中或最后一个已完成的号码）
     */
    @Select("SELECT COALESCE(MAX(queue_no), 0) FROM registration " +
            "WHERE doctor_id = #{doctorId} " +
            "AND reg_date = #{regDate} " +
            "AND status IN (2, 3, 4) " +  // 2已签到 3就诊中 4已完成
            "AND deleted = 0")
    Integer selectCurrentNumber(@Param("doctorId") Long doctorId, @Param("regDate") LocalDate regDate);

    /**
     * 统计某医生某日期某时间段前面还有多少人在排队
     */
    @Select("SELECT COUNT(*) FROM registration " +
            "WHERE doctor_id = #{doctorId} " +
            "AND reg_date = #{regDate} " +
            "AND queue_no < #{queueNo} " +
            "AND status = 1 " +  // 1已挂号（排队中）
            "AND deleted = 0")
    Integer countAheadInQueue(@Param("doctorId") Long doctorId, 
                              @Param("regDate") LocalDate regDate, 
                              @Param("queueNo") Integer queueNo);

    /**
     * 统计某医生某日期某时间段的排队总人数
     */
    @Select("SELECT COUNT(*) FROM registration " +
            "WHERE doctor_id = #{doctorId} " +
            "AND reg_date = #{regDate} " +
            "AND status = 1 " +  // 1已挂号（排队中）
            "AND deleted = 0")
    Integer countQueueTotal(@Param("doctorId") Long doctorId, @Param("regDate") LocalDate regDate);

    /**
     * 物理删除创建时间早于指定日期的挂号记录
     */
    @Delete("DELETE FROM registration WHERE created_at < #{beforeDate}")
    int deleteByCreatedAtBefore(@Param("beforeDate") LocalDateTime beforeDate);
}