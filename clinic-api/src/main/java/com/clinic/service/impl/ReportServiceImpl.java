package com.clinic.service.impl;

import com.clinic.mapper.ReportMapper;
import com.clinic.service.PaymentRecordService;
import com.clinic.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final PaymentRecordService paymentRecordService;

    @Override
    public Map<String, Object> dashboard(Long clinicId) {
        Map<String, Object> data = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekAgo = today.minusDays(6);

        log.info("Dashboard查询 - clinicId: {}, today: {}, yesterday: {}, weekAgo: {}", 
                clinicId, today, yesterday, weekAgo);

        data.put("todayDate", today.toString());

        // 今日挂号数及环比
        Integer todayRegCount = reportMapper.selectTodayRegCount(clinicId, today);
        Integer yesterdayRegCount = reportMapper.selectYesterdayRegCount(clinicId, yesterday);
        data.put("todayRegCount", todayRegCount);
        data.put("todayRegChange", calculateChangePercent(todayRegCount, yesterdayRegCount));

        // 待就诊人数及环比
        Integer waitingCount = reportMapper.selectWaitingCount(clinicId, today);
        Integer yesterdayWaitingCount = reportMapper.selectYesterdayWaitingCount(clinicId, yesterday);
        data.put("waitingCount", waitingCount);
        data.put("waitingChange", calculateChangePercent(waitingCount, yesterdayWaitingCount));

        // 未缴费订单数
        Integer unpaidCount = reportMapper.selectUnpaidCount(clinicId, today);
        data.put("unpaidCount", unpaidCount);

        // 今日收入及环比
        BigDecimal todayRevenue = reportMapper.selectTodayRevenue(clinicId, today);
        BigDecimal yesterdayRevenue = reportMapper.selectYesterdayRevenue(clinicId, yesterday);
        
        log.info("收入统计结果 - clinicId: {}, today: {}, todayRevenue: {}, yesterdayRevenue: {}",
                clinicId, today, todayRevenue, yesterdayRevenue);
        
        data.put("todayRevenue", todayRevenue);
        data.put("todayRevenueChange", calculateChangePercent(todayRevenue, yesterdayRevenue));

        // 近7天挂号趋势
        List<Map<String, Object>> weekRegTrend = reportMapper.selectRegTrend(clinicId, weekAgo, today);
        data.put("weekRegTrend", fillMissingDates(weekRegTrend, weekAgo, today, "date", "count"));

        // 近7天收入趋势
        List<Map<String, Object>> weekRevenueTrend = reportMapper.selectRevenueTrend(clinicId, weekAgo, today);
        data.put("weekRevenueTrend", fillMissingDates(weekRevenueTrend, weekAgo, today, "date", "amount"));

        // 今日医生接诊排行
        List<Map<String, Object>> todayTopDoctors = reportMapper.selectTodayTopDoctors(clinicId, today);
        data.put("todayTopDoctors", todayTopDoctors);

        // 今日热销项目
        List<Map<String, Object>> todayTopItems = reportMapper.selectTodayTopItems(clinicId, today);
        data.put("todayTopItems", todayTopItems);

        // 低库存预警
        List<Map<String, Object>> lowStockItems = reportMapper.selectLowStockItems(clinicId);
        data.put("lowStockItems", lowStockItems);

        return data;
    }

    @Override
    public Map<String, Object> daily(Long clinicId, String date) {
        Map<String, Object> data = new HashMap<>();
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);

        data.put("reportDate", date);

        // 挂号统计
        Map<String, Object> regStats = reportMapper.selectDailyRegStats(clinicId, localDate);
        data.put("regCount", regStats.get("regCount"));
        data.put("regCancelCount", regStats.get("regCancelCount"));
        data.put("actualVisitCount", regStats.get("actualVisitCount"));

        // 新老患者统计
        Integer newPatientCount = reportMapper.selectDailyNewPatientCount(clinicId, localDate);
        Integer oldPatientCount = reportMapper.selectDailyOldPatientCount(clinicId, localDate);
        data.put("newPatientCount", newPatientCount);
        data.put("oldPatientCount", oldPatientCount);

        // 处方统计
        Map<String, Object> prescriptionStats = reportMapper.selectDailyPrescriptionStats(clinicId, localDate);
        data.put("prescriptionCount", prescriptionStats.get("prescriptionCount"));
        data.put("prescriptionAmount", prescriptionStats.get("prescriptionAmount"));
        data.put("discountAmount", prescriptionStats.get("discountAmount"));
        data.put("actualAmount", prescriptionStats.get("actualAmount"));

        // 支付方式统计（从支付流水表查询）
        Map<Integer, BigDecimal> paymentStats = paymentRecordService.getDailyPaymentMethodStats(clinicId);
        data.put("cashAmount", paymentStats.getOrDefault(1, BigDecimal.ZERO));
        data.put("wechatAmount", paymentStats.getOrDefault(2, BigDecimal.ZERO));
        data.put("alipayAmount", paymentStats.getOrDefault(3, BigDecimal.ZERO));
        data.put("cardAmount", paymentStats.getOrDefault(4, BigDecimal.ZERO));

        // 收入明细（项目分类统计）
        List<Map<String, Object>> revenueDetails = paymentRecordService.getDailyRevenueDetails(clinicId);
        data.put("revenueDetails", revenueDetails);

        return data;
    }

    @Override
    public Map<String, Object> monthly(Long clinicId, Integer year, Integer month) {
        Map<String, Object> data = new HashMap<>();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        data.put("year", year);
        data.put("month", month);

        // 月度汇总需要从日报汇总
        int regCount = 0;
        int regCancelCount = 0;
        int actualVisitCount = 0;
        int newPatientCount = 0;
        int oldPatientCount = 0;
        int prescriptionCount = 0;
        BigDecimal prescriptionAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal actualAmount = BigDecimal.ZERO;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Map<String, Object> regStats = reportMapper.selectDailyRegStats(clinicId, date);
            regCount += ((Number) regStats.get("regCount")).intValue();
            regCancelCount += ((Number) regStats.get("regCancelCount")).intValue();
            actualVisitCount += ((Number) regStats.get("actualVisitCount")).intValue();

            newPatientCount += reportMapper.selectDailyNewPatientCount(clinicId, date);
            oldPatientCount += reportMapper.selectDailyOldPatientCount(clinicId, date);

            Map<String, Object> prescriptionStats = reportMapper.selectDailyPrescriptionStats(clinicId, date);
            prescriptionCount += ((Number) prescriptionStats.get("prescriptionCount")).intValue();
            prescriptionAmount = prescriptionAmount.add((BigDecimal) prescriptionStats.get("prescriptionAmount"));
            discountAmount = discountAmount.add((BigDecimal) prescriptionStats.get("discountAmount"));
            actualAmount = actualAmount.add((BigDecimal) prescriptionStats.get("actualAmount"));
        }

        data.put("regCount", regCount);
        data.put("regCancelCount", regCancelCount);
        data.put("actualVisitCount", actualVisitCount);
        data.put("newPatientCount", newPatientCount);
        data.put("oldPatientCount", oldPatientCount);
        data.put("prescriptionCount", prescriptionCount);
        data.put("prescriptionAmount", prescriptionAmount);
        data.put("discountAmount", discountAmount);
        data.put("actualAmount", actualAmount);

        return data;
    }

    @Override
    public List<Map<String, Object>> deptRanking(Long clinicId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        return reportMapper.selectDeptRanking(clinicId, start, end);
    }

    @Override
    public List<Map<String, Object>> doctorPerformance(Long clinicId, String startDate, String endDate, Long doctorId) {
        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        return reportMapper.selectDoctorPerformance(clinicId, start, end, doctorId);
    }

    @Override
    public List<Map<String, Object>> medicineConsume(Long clinicId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE);
        LocalDate end = LocalDate.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE);
        return reportMapper.selectMedicineConsume(clinicId, start, end);
    }

    @Override
    public Map<String, Object> debugTodayPrescription(Long clinicId) {
        Map<String, Object> result = new HashMap<>();
        LocalDate today = LocalDate.now();

        result.put("queryDate", today.toString());
        result.put("queryClinicId", clinicId);

        // 查询所有诊所的今日处方数（用于对比）
        Integer allClinicCount = reportMapper.selectTodayPrescriptionCountAll(today);
        result.put("allClinicsTodayCount", allClinicCount);

        // 查询指定诊所的今日处方数
        if (clinicId != null) {
            Integer specificClinicCount = reportMapper.selectTodayPrescriptionCount(clinicId, today);
            result.put("specificClinicTodayCount", specificClinicCount);

            // 查询该诊所所有处方（不限日期）
            Integer totalCount = reportMapper.selectPrescriptionCountByClinic(clinicId);
            result.put("clinicTotalCount", totalCount);

            // 查询该诊所的诊所ID分布
            List<Map<String, Object>> clinicDistribution = reportMapper.selectPrescriptionClinicDistribution();
            result.put("clinicDistribution", clinicDistribution);
        }

        // 查询最近5条处方记录
        List<Map<String, Object>> recentRecords = reportMapper.selectRecentPrescriptions(5);
        result.put("recentRecords", recentRecords);

        return result;
    }

    /**
     * 计算环比百分比变化
     */
    private int calculateChangePercent(Number today, Number yesterday) {
        if (yesterday == null || yesterday.doubleValue() == 0) {
            return today != null && today.doubleValue() > 0 ? 100 : 0;
        }
        double change = (today.doubleValue() - yesterday.doubleValue()) / yesterday.doubleValue() * 100;
        return (int) Math.round(change);
    }

    /**
     * 填充缺失的日期数据
     */
    private List<Map<String, Object>> fillMissingDates(List<Map<String, Object>> data, LocalDate startDate, LocalDate endDate, String dateKey, String valueKey) {
        Map<String, Object> dataMap = new HashMap<>();
        for (Map<String, Object> item : data) {
            String date = item.get(dateKey).toString();
            dataMap.put(date, item.get(valueKey));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Map<String, Object> item = new HashMap<>();
            item.put(dateKey, date.toString());
            Object value = dataMap.get(date.toString());
            if (valueKey.equals("amount")) {
                item.put(valueKey, value != null ? new BigDecimal(value.toString()) : BigDecimal.ZERO);
            } else {
                item.put(valueKey, value != null ? ((Number) value).intValue() : 0);
            }
            result.add(item);
        }
        return result;
    }
}
