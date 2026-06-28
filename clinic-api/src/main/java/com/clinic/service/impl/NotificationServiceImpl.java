package com.clinic.service.impl;

import com.clinic.entity.Notification;
import com.clinic.mapper.NotificationMapper;
import com.clinic.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    public void createScheduleNotification(Long clinicId, Long doctorId, String scheduleDate) {
        Notification notification = new Notification();
        notification.setClinicId(clinicId);
        notification.setDoctorId(doctorId);
        notification.setTitle("排班通知");
        notification.setContent("您有新的排班安排，日期：" + scheduleDate);
        notification.setType(1); // 排班通知
        notification.setIsRead(0);
        notification.setRelatedType("SCHEDULE");
        notificationMapper.insert(notification);
    }

    @Override
    public void createNotification(Long clinicId, Long doctorId, String title, String content,
                                   Integer type, Long relatedId, String relatedType) {
        Notification notification = new Notification();
        notification.setClinicId(clinicId);
        notification.setDoctorId(doctorId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setRelatedId(relatedId);
        notification.setRelatedType(relatedType);
        notificationMapper.insert(notification);
    }

    @Override
    public List<Notification> getUnreadNotifications(Long clinicId, Long doctorId) {
        return notificationMapper.selectUnreadByDoctor(clinicId, doctorId);
    }

    @Override
    public List<Notification> getRecentNotifications(Long clinicId, Long doctorId, Integer limit) {
        return notificationMapper.selectRecentByDoctor(clinicId, doctorId, limit);
    }

    @Override
    public void markAsRead(Long id, Long doctorId) {
        notificationMapper.markAsRead(id, doctorId);
    }

    @Override
    public Long getUnreadCount(Long clinicId, Long doctorId) {
        return notificationMapper.countUnread(clinicId, doctorId);
    }
}
