package com.clinic.controller.admin;

import com.clinic.common.result.Result;
import com.clinic.entity.Notification;
import com.clinic.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "通知管理")
@RestController
@RequestMapping("/admin/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @ApiOperation("获取未读通知列表")
    @GetMapping("/unread")
    public Result<List<Notification>> getUnread(
            @RequestParam Long clinicId,
            @RequestParam Long doctorId) {
        List<Notification> list = notificationService.getUnreadNotifications(clinicId, doctorId);
        return Result.success(list);
    }

    @ApiOperation("获取最近通知")
    @GetMapping("/recent")
    public Result<List<Notification>> getRecent(
            @RequestParam Long clinicId,
            @RequestParam Long doctorId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<Notification> list = notificationService.getRecentNotifications(clinicId, doctorId, limit);
        return Result.success(list);
    }

    @ApiOperation("标记通知为已读")
    @PostMapping("/read/{id}")
    public Result<String> markAsRead(
            @PathVariable Long id,
            @RequestBody Map<String, Long> params)
    {
        Long doctorId = params.get("doctorId");
        notificationService.markAsRead(id, doctorId);
        return Result.success("标记成功");
    }

    @ApiOperation("获取未读通知数量")
    @GetMapping("/unreadCount")
    public Result<Long> getUnreadCount(
            @RequestParam Long clinicId,
            @RequestParam Long doctorId) {
        Long count = notificationService.getUnreadCount(clinicId, doctorId);
        return Result.success(count);
    }
}
