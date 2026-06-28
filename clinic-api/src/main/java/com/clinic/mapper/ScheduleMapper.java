package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    @Select("SELECT * FROM schedule WHERE clinic_id = #{clinicId} " +
            "AND doctor_id = #{doctorId} " +
            "AND schedule_date = #{scheduleDate} " +
            "AND time_slot_start = #{timeSlotStart} " +
            "AND status = 1 " +
            "AND deleted = 0")
    Schedule selectByDoctorAndDateAndSlot(@Param("clinicId") Long clinicId,
                                           @Param("doctorId") Long doctorId,
                                           @Param("scheduleDate") LocalDate scheduleDate,
                                           @Param("timeSlotStart") LocalTime timeSlotStart);

    @Select("SELECT * FROM schedule WHERE clinic_id = #{clinicId} " +
            "AND doctor_id = #{doctorId} " +
            "AND schedule_date = #{scheduleDate} " +
            "AND time_slot_start = #{timeSlotStart} " +
            "AND deleted = 0")
    Schedule selectBySlotIgnoreStatus(@Param("clinicId") Long clinicId,
                                       @Param("doctorId") Long doctorId,
                                       @Param("scheduleDate") LocalDate scheduleDate,
                                       @Param("timeSlotStart") LocalTime timeSlotStart);

    @Select("SELECT * FROM schedule WHERE clinic_id = #{clinicId} " +
            "AND schedule_date BETWEEN #{startDate} AND #{endDate} " +
            "AND status = 1 " +
            "AND deleted = 0 " +
            "ORDER BY doctor_id, schedule_date, time_slot_start")
    List<Schedule> selectByDateRange(@Param("clinicId") Long clinicId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM schedule WHERE clinic_id = #{clinicId} " +
            "AND doctor_id = #{doctorId} " +
            "AND schedule_date BETWEEN #{startDate} AND #{endDate} " +
            "AND status = 1 " +
            "AND deleted = 0 " +
            "ORDER BY schedule_date, time_slot_start")
    List<Schedule> selectByDoctorAndDateRange(@Param("clinicId") Long clinicId,
                                               @Param("doctorId") Long doctorId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}
