package com.clinic.service;

import com.clinic.entity.Notification;

import java.util.List;

public interface NotificationService {

    void createScheduleNotification(Long clinicId, Long doctorId, String scheduleDate);

    void createNotification(Long clinicId, Long doctorId, String title, String content,
                            Integer type, Long relatedId, String relatedType);

    List<Notification> getUnreadNotifications(Long clinicId, Long doctorId);

    List<Notification> getRecentNotifications(Long clinicId, Long doctorId, Integer limit);

    void markAsRead(Long id, Long doctorId);

    Long getUnreadCount(Long clinicId, Long doctorId);
}
