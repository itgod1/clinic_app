package com.clinic.service.impl;

import com.clinic.entity.Patient;
import com.clinic.entity.ReturnVisitPlan;
import com.clinic.mapper.PatientMapper;
import com.clinic.mapper.ReturnVisitPlanMapper;
import com.clinic.service.AiReturnVisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * AI回访定时任务
 * 每天定时检查需要回访的患者，发送微信通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiReturnVisitScheduler {

    private final ReturnVisitPlanMapper returnVisitPlanMapper;
    private final PatientMapper patientMapper;
    private final AiReturnVisitService aiReturnVisitService;
    private final WechatMessageService wechatMessageService;

    /**
     * 每天上午9点执行回访任务
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduleDailyReturnVisit() {
        log.info("开始执行每日AI回访任务...");
        
        // 查询今天需要回访的计划（默认诊所ID为1，可根据实际情况修改）
        List<ReturnVisitPlan> plans = returnVisitPlanMapper.selectTodayPlans(1L, LocalDate.now());
        
        for (ReturnVisitPlan plan : plans) {
            try {
                // 1. 创建AI回访任务
                Long taskId = aiReturnVisitService.createTask(plan.getId(), plan.getPatientId());
                
                // 2. 发送微信模板消息通知患者
                // 获取患者信息（包括微信openid）
                Patient patient = patientMapper.selectById(plan.getPatientId());
                String openId = patient != null ? patient.getOpenId() : null;
                
                if (openId != null && !openId.isEmpty()) {
                    wechatMessageService.sendReturnVisitNotification(
                        openId,
                        plan.getPatientName(),
                        plan.getVisitItem(),
                        taskId,
                        plan.getPatientId(),
                        plan.getId()
                    );
                } else {
                    log.warn("患者 {} 没有绑定微信openid，跳过发送模板消息", plan.getPatientName());
                }
                
                log.info("已发送回访通知给患者：{}，计划ID：{}", plan.getPatientName(), plan.getId());
                
            } catch (Exception e) {
                log.error("发送回访通知失败，计划ID：{}，错误：{}", plan.getId(), e.getMessage());
            }
        }
        
        log.info("每日AI回访任务执行完成，共处理{}条计划", plans.size());
    }

    /**
     * 每小时检查一次超时未回复的任务
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkTimeoutTasks() {
        log.info("检查超时回访任务...");
        // 实现超时处理逻辑
    }
}
