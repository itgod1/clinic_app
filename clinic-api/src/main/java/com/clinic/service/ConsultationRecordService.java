package com.clinic.service;

import com.clinic.entity.ConsultationRecord;
import java.util.Map;

public interface ConsultationRecordService {

    Map<String, Object> list(Long clinicId, Integer recordType, Long doctorId, String startDate, String endDate, String keyword, Integer pageNum, Integer pageSize);

    ConsultationRecord getById(Long id);

    void create(ConsultationRecord record);

    void update(ConsultationRecord record);

    void delete(Long id);

    String generateRecordNo(Integer recordType);

    /**
     * 获取工作台复诊提醒（即将到期+近期逾期的咨询记录）
     */
    java.util.List<ConsultationRecord> getUpcomingRevisits(Long clinicId, Long doctorId);

    /**
     * 标记咨询记录已提醒
     */
    void remind(Long id);

    /**
     * 标记咨询记录已完成
     */
    void complete(Long id);
}
