package com.example.finalproject.domain.notifications.controller;

import com.example.finalproject.domain.notifications.dto.request.NotificationMessageRequest;
import com.example.finalproject.domain.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/users/{userId}")
    public String sendToUser(@PathVariable Long userId,
                             @RequestBody NotificationMessageRequest req) {
        notificationService.sendUserNotification(userId, req.getMessage());
        return "개인 알림 전송 요청 완료";
    }

    @PostMapping("/all")
    public String sendToAll(@RequestBody NotificationMessageRequest req) {
        notificationService.sendBroadcastNotification(req.getMessage());
        return "전체 알림 전송 요청 완료";
    }
}
