package com.clinic.task;

import com.clinic.entity.ReturnVisitPlan;
import com.clinic.mapper.ReturnVisitPlanMapper;
import com.clinic.service.NotificationService;
import com.clinic.service.ReturnVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RevisitReminderTask {

    private final ReturnVisitService returnVisitService;
    private final ReturnVisitPlanMapper planMapper;
    private final NotificationService notificationService;

    /**
     * 每天早上8点检查到期的复诊提醒
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkDueRevisits() {
        log.info("开始检查到期复诊提醒...");
        LocalDate today = LocalDate.now();
        List<ReturnVisitPlan> duePlans = planMapper.selectAllDueRevisitPlans(today);
        
        log.info("发现 {} 条到期复诊提醒", duePlans.size());
        
        for (ReturnVisitPlan plan : duePlans) {
            try {
                // 增加提醒次数
                planMapper.incrementRemindCount(plan.getId());
                
                // 创建通知给医生
                String title = "复诊提醒";
                String content = String.format("患者 %s 的 %s 复诊已到期（计划日期：%s），请及时联系患者预约。",
                        plan.getPatientName(),
                        plan.getVisitItem(),
                        plan.getPlanDate());
                
                notificationService.createNotification(
                        plan.getClinicId(),
                        plan.getDoctorId(),
                        title,
                        content,
                        2, // 类型：复诊提醒
                        plan.getId(),
                        "REVISIT_PLAN"
                );
                
                log.info("已发送复诊提醒通知：patient={}, item={}", plan.getPatientName(), plan.getVisitItem());
            } catch (Exception e) {
                log.error("处理复诊提醒失败: planId={}", plan.getId(), e);
            }
        }
        
        log.info("复诊提醒检查完成");
    }

    /**
     * 每小时检查一次逾期复诊（状态仍为PENDING但已过期）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkOverdueRevisits() {
        log.info("开始检查逾期复诊...");
        // 可以在这里添加逾期提醒逻辑
    }
}
