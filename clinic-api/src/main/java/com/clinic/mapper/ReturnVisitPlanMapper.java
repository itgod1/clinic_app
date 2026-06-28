package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.ReturnVisitPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ReturnVisitPlanMapper extends BaseMapper<ReturnVisitPlan> {

    List<ReturnVisitPlan> selectTodayPlans(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    List<ReturnVisitPlan> selectOverduePlans(@Param("clinicId") Long clinicId);

    int countByStatus(@Param("clinicId") Long clinicId, @Param("status") String status);

    // 复诊提醒相关查询

    @Select("SELECT * FROM return_visit_plan WHERE clinic_id = #{clinicId} AND plan_type = 'REVISIT' " +
            "AND status = 'PENDING' AND plan_date <= #{date} AND deleted = 0 ORDER BY plan_date ASC")
    List<ReturnVisitPlan> selectDueRevisitPlans(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    @Select("SELECT * FROM return_visit_plan WHERE plan_type = 'REVISIT' " +
            "AND status = 'PENDING' AND plan_date <= #{date} AND deleted = 0 ORDER BY clinic_id, plan_date ASC")
    List<ReturnVisitPlan> selectAllDueRevisitPlans(@Param("date") LocalDate date);

    @Select("SELECT * FROM return_visit_plan WHERE clinic_id = #{clinicId} AND plan_type = 'REVISIT' " +
            "AND status = 'REMINDED' AND deleted = 0 ORDER BY plan_date ASC")
    List<ReturnVisitPlan> selectRemindedRevisitPlans(@Param("clinicId") Long clinicId);

    @Select("SELECT * FROM return_visit_plan WHERE clinic_id = #{clinicId} AND plan_type = 'REVISIT' " +
            "AND status = 'APPOINTED' AND deleted = 0 ORDER BY plan_date ASC")
    List<ReturnVisitPlan> selectAppointedRevisitPlans(@Param("clinicId") Long clinicId);

    @Select("SELECT COUNT(*) FROM return_visit_plan WHERE clinic_id = #{clinicId} AND plan_type = #{planType} " +
            "AND status = #{status} AND deleted = 0")
    int countByTypeAndStatus(@Param("clinicId") Long clinicId, @Param("planType") String planType, @Param("status") String status);

    @Select("SELECT * FROM return_visit_plan WHERE clinic_id = #{clinicId} AND patient_id = #{patientId} " +
            "AND plan_type = 'REVISIT' AND status IN ('PENDING', 'REMINDED', 'APPOINTED') " +
            "AND deleted = 0 ORDER BY plan_date DESC")
    List<ReturnVisitPlan> selectActiveRevisitPlansByPatient(@Param("clinicId") Long clinicId, @Param("patientId") Long patientId);

    @Update("UPDATE return_visit_plan SET remind_count = remind_count + 1, reminded_at = NOW() WHERE id = #{id}")
    int incrementRemindCount(@Param("id") Long id);
}
