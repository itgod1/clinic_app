package com.clinic.service;

import com.clinic.entity.Registration;
import com.clinic.vo.QueueInfoVO;

import java.util.Map;

public interface RegistrationService {

    /**
     * 获取挂号列表（带枚举名称）
     */
    Map<String, Object> list(Long clinicId, String keyword, String regDate, String startDate, String endDate, Long doctorId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 获取当前用户的挂号列表
     */
    Map<String, Object> listByUserId(Long userId, Long clinicId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 获取挂号详情（带枚举名称）
     */
    Registration getById(Long id);

    /**
     * 创建挂号
     */
    void create(Registration registration);

    /**
     * 取消挂号
     */
    void cancel(Long regId, String reason);

    /**
     * 更新挂号状态
     */
    void updateStatus(Registration registration);

    /**
     * 生成挂号单号
     */
    String generateRegNo();

    /**
     * 生成排队号
     */
    Integer generateQueueNo(Long doctorId, java.time.LocalDate regDate);

    /**
     * 校验挂号信息
     */
    void validateRegistration(Registration registration);

    /**
     * 获取挂号排队信息
     */
    QueueInfoVO getQueueInfo(Long registrationId);
}
