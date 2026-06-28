package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.FollowUpPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FollowUpPlanMapper extends BaseMapper<FollowUpPlan> {

    @Select("SELECT * FROM follow_up_plan WHERE clinic_id = #{clinicId} AND status = 'PENDING' " +
            "AND plan_date <= #{date} AND deleted = 0 ORDER BY plan_date ASC")
    List<FollowUpPlan> selectDuePlans(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    @Select("SELECT * FROM follow_up_plan WHERE status = 'PENDING' " +
            "AND plan_date <= #{date} AND deleted = 0 ORDER BY clinic_id, plan_date ASC")
    List<FollowUpPlan> selectAllDuePlans(@Param("date") LocalDate date);

    @Select("SELECT * FROM follow_up_plan WHERE clinic_id = #{clinicId} AND status = 'REMINDED' " +
            "AND deleted = 0 ORDER BY plan_date ASC")
    List<FollowUpPlan> selectRemindedPlans(@Param("clinicId") Long clinicId);

    @Select("SELECT COUNT(*) FROM follow_up_plan WHERE clinic_id = #{clinicId} AND status = #{status} AND deleted = 0")
    int countByStatus(@Param("clinicId") Long clinicId, @Param("status") String status);

    @Select("SELECT * FROM follow_up_plan WHERE clinic_id = #{clinicId} AND patient_id = #{patientId} " +
            "AND status IN ('PENDING', 'REMINDED', 'APPOINTED') AND deleted = 0 ORDER BY plan_date DESC")
    List<FollowUpPlan> selectActivePlansByPatient(@Param("clinicId") Long clinicId, @Param("patientId") Long patientId);

    @Update("UPDATE follow_up_plan SET remind_count = remind_count + 1, reminded_at = NOW() WHERE id = #{id}")
    int incrementRemindCount(@Param("id") Long id);
}
