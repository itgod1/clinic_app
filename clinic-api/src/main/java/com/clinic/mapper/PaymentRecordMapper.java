package com.clinic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.clinic.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {

    /**
     * 根据处方ID查询支付记录
     */
    @Select("SELECT * FROM payment_record WHERE prescription_id = #{prescriptionId} AND deleted = 0 ORDER BY created_at DESC")
    List<PaymentRecord> selectByPrescriptionId(@Param("prescriptionId") Long prescriptionId);

    /**
     * 根据订单号查询支付记录
     */
    @Select("SELECT * FROM payment_record WHERE order_no = #{orderNo} AND deleted = 0 LIMIT 1")
    PaymentRecord selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询今日支付方式统计
     */
    @Select("SELECT payment_method as paymentMethod, COALESCE(SUM(actual_amount), 0) as amount " +
            "FROM payment_record WHERE clinic_id = #{clinicId} AND DATE(created_at) = #{date} " +
            "AND payment_status = 1 AND deleted = 0 " +
            "GROUP BY payment_method")
    List<Map<String, Object>> selectDailyPaymentMethodStats(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);

    /**
     * 查询今日收入明细（按项目）
     */
    @Select("SELECT pi.item_name as itemName, SUM(pi.quantity) as quantity, SUM(pi.subtotal) as amount " +
            "FROM payment_record pr " +
            "INNER JOIN prescription p ON pr.prescription_id = p.id " +
            "INNER JOIN prescription_item pi ON p.id = pi.prescription_id " +
            "WHERE pr.clinic_id = #{clinicId} AND DATE(pr.pay_time) = #{date} AND pr.deleted = 0 " +
            "GROUP BY pi.item_name ORDER BY amount DESC")
    List<Map<String, Object>> selectDailyRevenueDetails(@Param("clinicId") Long clinicId, @Param("date") LocalDate date);
}
