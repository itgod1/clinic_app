package com.clinic.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    /**
     * 查询今日挂号数
     */
    @Select("SELECT COUNT(*) FROM registration WHERE clinic_id = #{clinicId} AND reg_date = #{today} AND deleted = 0")
    Integer selectTodayRegCount(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    /**
     * 查询昨日挂号数
     */
    @Select("SELECT COUNT(*) FROM registration WHERE clinic_id = #{clinicId} AND reg_date = #{yesterday} AND deleted = 0")
    Integer selectYesterdayRegCount(@Param("clinicId") Long clinicId, @Param("yesterday") LocalDate yesterday);

    /**
     * 查询今日待就诊人数
     */
    @Select("SELECT COUNT(*) FROM registration WHERE clinic_id = #{clinicId} AND reg_date = #{today} AND status = 1 AND deleted = 0")
    Integer selectWaitingCount(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    /**
     * 查询昨日待就诊人数
     */
    @Select("SELECT COUNT(*) FROM registration WHERE clinic_id = #{clinicId} AND reg_date = #{yesterday} AND status = 1 AND deleted = 0")
    Integer selectYesterdayWaitingCount(@Param("clinicId") Long clinicId, @Param("yesterday") LocalDate yesterday);

    /**
     * 查询今日未缴费订单数
     */
    @Select("SELECT COUNT(*) FROM prescription WHERE clinic_id = #{clinicId} AND created_at >= #{today} AND created_at < DATE_ADD(#{today}, INTERVAL 1 DAY) AND payment_status = 0 AND deleted = 0")
    Integer selectUnpaidCount(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    /**
     * 查询今日实收金额（包含所有已支付记录，不限于今天创建的）
     */
    @Select("SELECT COALESCE(SUM(actual_amount), 0) FROM prescription WHERE clinic_id = #{clinicId} AND created_at >= #{today} AND created_at < DATE_ADD(#{today}, INTERVAL 1 DAY) AND payment_status = 1 AND deleted = 0")
    BigDecimal selectTodayRevenue(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    /**
     * 查询昨日实收金额
     */
    @Select("SELECT COALESCE(SUM(actual_amount), 0) FROM prescription WHERE clinic_id = #{clinicId} AND created_at >= #{yesterday} AND created_at < DATE_ADD(#{yesterday}, INTERVAL 1 DAY) AND payment_status = 1 AND deleted = 0")
    BigDecimal selectYesterdayRevenue(@Param("clinicId") Long clinicId, @Param("yesterday") LocalDate yesterday);

    /**
     * 查询近7天挂号趋势
     */
    @Select("SELECT reg_date as date, COUNT(*) as count FROM registration " +
            "WHERE clinic_id = #{clinicId} AND reg_date >= #{startDate} AND reg_date <= #{endDate} AND deleted = 0 " +
            "GROUP BY reg_date ORDER BY reg_date")
    List<Map<String, Object>> selectRegTrend(@Param("clinicId") Long clinicId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询近7天收入趋势
     */
    @Select("SELECT DATE(created_at) as date, COALESCE(SUM(actual_amount), 0) as amount FROM prescription " +
            "WHERE clinic_id = #{clinicId} AND created_at >= #{startDate} AND created_at < DATE_ADD(#{endDate}, INTERVAL 1 DAY) AND payment_status = 1 AND deleted = 0 " +
            "GROUP BY DATE(created_at) ORDER BY DATE(created_at)")
    List<Map<String, Object>> selectRevenueTrend(@Param("clinicId") Long clinicId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询今日医生接诊排行
     */
    @Select("SELECT doctor_id as doctorId, doctor_name as doctorName, COUNT(*) as count " +
            "FROM registration WHERE clinic_id = #{clinicId} AND reg_date = #{today} AND status = 2 AND deleted = 0 " +
            "GROUP BY doctor_id, doctor_name ORDER BY count DESC LIMIT 5")
    List<Map<String, Object>> selectTodayTopDoctors(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    /**
     * 查询今日热销项目
     */
    @Select("SELECT pi.item_name as itemName, SUM(pi.quantity) as totalQuantity, SUM(pi.subtotal) as totalAmount " +
            "FROM prescription_item pi " +
            "INNER JOIN prescription p ON pi.prescription_id = p.id " +
            "WHERE p.clinic_id = #{clinicId} AND DATE(p.created_at) = #{today} AND p.deleted = 0 " +
            "GROUP BY pi.item_name ORDER BY totalQuantity DESC LIMIT 5")
    List<Map<String, Object>> selectTodayTopItems(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    /**
     * 查询低库存商品
     */
    @Select("SELECT id, item_name as itemName, stock, low_stock_alert as lowStockAlert " +
            "FROM stock_item WHERE clinic_id = #{clinicId} AND stock <= low_stock_alert AND deleted = 0 LIMIT 5")
    List<Map<String, Object>> selectLowStockItems(@Param("clinicId") Long clinicId);

    /**
     * 查询日报表数据
     */
    @Select("SELECT " +
            "  COUNT(*) as regCount, " +
            "  SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as regCancelCount, " +
            "  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as actualVisitCount " +
            "FROM registration WHERE clinic_id = #{clinicId} AND reg_date = #{date} AND deleted = 0")
    Map<String, Object> selectDailyRegStats(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    /**
     * 查询日新增患者数
     */
    @Select("SELECT COUNT(*) FROM patient WHERE clinic_id = #{clinicId} AND DATE(created_at) = #{date} AND deleted = 0")
    Integer selectDailyNewPatientCount(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    /**
     * 查询日复诊患者数
     */
    @Select("SELECT COUNT(*) FROM registration WHERE clinic_id = #{clinicId} AND reg_date = #{date} AND visit_type = 2 AND deleted = 0")
    Integer selectDailyOldPatientCount(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    /**
     * 查询日处方统计
     */
    @Select("SELECT " +
            "  COUNT(*) as prescriptionCount, " +
            "  COALESCE(SUM(total_amount), 0) as prescriptionAmount, " +
            "  COALESCE(SUM(discount_amount), 0) as discountAmount, " +
            "  COALESCE(SUM(actual_amount), 0) as actualAmount " +
            "FROM prescription WHERE clinic_id = #{clinicId} AND DATE(created_at) = #{date} AND deleted = 0")
    Map<String, Object> selectDailyPrescriptionStats(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    /**
     * 查询科室排名
     */
    @Select("SELECT r.dept_id as deptId, r.dept_name as deptName, COUNT(*) as regCount, COUNT(DISTINCT r.patient_id) as patientCount " +
            "FROM registration r " +
            "WHERE r.clinic_id = #{clinicId} AND r.reg_date >= #{startDate} AND r.reg_date <= #{endDate} AND r.deleted = 0 " +
            "GROUP BY r.dept_id, r.dept_name ORDER BY regCount DESC")
    List<Map<String, Object>> selectDeptRanking(@Param("clinicId") Long clinicId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询医生业绩
     */
    @Select("SELECT " +
            "  r.doctor_id as doctorId, " +
            "  r.doctor_name as doctorName, " +
            "  COUNT(*) as regCount, " +
            "  COUNT(DISTINCT r.patient_id) as patientCount, " +
            "  COALESCE(SUM(p.actual_amount), 0) as revenue " +
            "FROM registration r " +
            "LEFT JOIN prescription p ON r.id = p.registration_id AND p.deleted = 0 " +
            "WHERE r.clinic_id = #{clinicId} AND r.reg_date >= #{startDate} AND r.reg_date <= #{endDate} " +
            "AND r.deleted = 0 " +
            "<if test='doctorId != null'> AND r.doctor_id = #{doctorId} </if> " +
            "GROUP BY r.doctor_id, r.doctor_name ORDER BY regCount DESC")
    List<Map<String, Object>> selectDoctorPerformance(@Param("clinicId") Long clinicId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("doctorId") Long doctorId);

    /**
     * 查询药品消耗排行
     */
    @Select("SELECT pi.item_name as itemName, SUM(pi.quantity) as totalQuantity, SUM(pi.subtotal) as totalAmount " +
            "FROM prescription_item pi " +
            "INNER JOIN prescription p ON pi.prescription_id = p.id " +
            "WHERE p.clinic_id = #{clinicId} AND DATE(p.created_at) >= #{startDate} AND DATE(p.created_at) <= #{endDate} AND p.deleted = 0 " +
            "GROUP BY pi.item_name ORDER BY totalQuantity DESC LIMIT 20")
    List<Map<String, Object>> selectMedicineConsume(@Param("clinicId") Long clinicId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // ========== 调试查询方法 ==========

    /**
     * 查询所有诊所今日处方数
     */
    @Select("SELECT COUNT(*) FROM prescription WHERE created_at >= #{today} AND created_at < DATE_ADD(#{today}, INTERVAL 1 DAY) AND deleted = 0")
    Integer selectTodayPrescriptionCountAll(@Param("today") LocalDate today);

    /**
     * 查询指定诊所今日处方数
     */
    @Select("SELECT COUNT(*) FROM prescription WHERE clinic_id = #{clinicId} AND created_at >= #{today} AND created_at < DATE_ADD(#{today}, INTERVAL 1 DAY) AND deleted = 0")
    Integer selectTodayPrescriptionCount(@Param("clinicId") Long clinicId, @Param("today") LocalDate today);

    /**
     * 查询指定诊所处方总数
     */
    @Select("SELECT COUNT(*) FROM prescription WHERE clinic_id = #{clinicId} AND deleted = 0")
    Integer selectPrescriptionCountByClinic(@Param("clinicId") Long clinicId);

    /**
     * 查询处方诊所分布
     */
    @Select("SELECT clinic_id as clinicId, COUNT(*) as count FROM prescription WHERE deleted = 0 GROUP BY clinic_id")
    List<Map<String, Object>> selectPrescriptionClinicDistribution();

    /**
     * 查询最近处方记录
     */
    @Select("SELECT id, clinic_id as clinicId, prescription_no as prescriptionNo, total_amount as totalAmount, " +
            "actual_amount as actualAmount, payment_status as paymentStatus, created_at as createdAt " +
            "FROM prescription WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> selectRecentPrescriptions(@Param("limit") Integer limit);
}
