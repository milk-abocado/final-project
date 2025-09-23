package com.example.finalproject.domain.notifications.controller;

import com.example.finalproject.domain.notifications.dto.request.NotificationMessageRequest;
import com.example.finalproject.domain.notifications.repository.NotificationRepository;
import com.example.finalproject.domain.notifications.service.NotificationService;
import com.example.finalproject.domain.notifications.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

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

    // 관리자 알림 로그 전체 조회
    @GetMapping("/logs")
    public List<Notification> getAllLogs() {
        return notificationRepository.findAll();
    }

}
