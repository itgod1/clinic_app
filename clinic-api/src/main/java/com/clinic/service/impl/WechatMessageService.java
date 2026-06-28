package com.clinic.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信消息推送服务
 * 用于发送模板消息给患者
 */
@Slf4j
@Service
public class WechatMessageService {

    @Value("${wechat.miniapp.appid}")
    private String appid;

    @Value("${wechat.miniapp.secret}")
    private String secret;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 发送回访通知模板消息
     *
     * @param openId 患者微信openid
     * @param patientName 患者姓名
     * @param visitItem 回访项目（如：根管治疗后回访）
     * @param taskId 回访任务ID
     * @param patientId 患者ID
     * @param planId 回访计划ID
     */
    public void sendReturnVisitNotification(String openId, String patientName, String visitItem,
            Long taskId, Long patientId, Long planId) {
        try {
            // 1. 获取 access_token
            String accessToken = getAccessToken();

            // 2. 构建模板消息
            String url = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;

            // 构建小程序页面路径，带参数跳转到AI回访页面
            String pagePath = String.format(
                "pages/ai/return-visit?patient_id=%d&plan_id=%d&task_id=%d&patientName=%s&visitItem=%s&from=message",
                patientId, planId, taskId, patientName, visitItem
            );

            Map<String, Object> message = new HashMap<>();
            message.put("touser", openId);
            message.put("template_id", "你的模板ID"); // 需要在微信公众平台申请
            message.put("page", pagePath); // 点击跳转小程序AI回访页面
            message.put("miniprogram_state", "formal");

            // 模板消息数据
            Map<String, Map<String, String>> data = new HashMap<>();
            data.put("thing1", Map.of("value", visitItem)); // 回访项目
            data.put("name2", Map.of("value", patientName)); // 患者姓名
            data.put("thing3", Map.of("value", "请点击查看详情并与AI助手交流")); // 提示
            data.put("time4", Map.of("value", java.time.LocalDateTime.now().toString().substring(0, 10))); // 时间

            message.put("data", data);

            // 3. 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, message, String.class);
            log.info("发送模板消息结果：{}", response.getBody());

        } catch (Exception e) {
            log.error("发送微信模板消息失败", e);
        }
    }

    /**
     * 获取微信 access_token
     */
    private String getAccessToken() {
        String url = String.format(
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
            appid, secret
        );
        
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> result = response.getBody();
        
        if (result != null && result.containsKey("access_token")) {
            return (String) result.get("access_token");
        }
        
        throw new RuntimeException("获取access_token失败");
    }
}
