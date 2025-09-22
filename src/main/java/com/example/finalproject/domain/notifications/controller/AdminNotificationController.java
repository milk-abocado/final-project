package com.example.finalproject.domain.notifications.controller;

import com.example.finalproject.domain.notifications.dto.request.NotificationMessageRequest;
import com.example.finalproject.domain.notifications.entity.Notification;
import com.example.finalproject.domain.notifications.exception.NotificationException;
import com.example.finalproject.domain.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationRepository notificationRepository;

    // 특정 사용자 알림
    @PostMapping("/user/{userId}")
    public String sendToUser(@PathVariable Long userId,
                             @RequestBody NotificationMessageRequest req) {
        try {
            // DB 저장
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setType("USER");
            notification.setMessage(req.getMessage());
            notificationRepository.save(notification);

            // SSE 전송
            SseController.sendToUser(userId, "[개인 알림] " + req.getMessage());
            return "개인 알림 전송 완료";
        } catch (Exception e) {
            throw new NotificationException("개인 알림 전송 실패", e);
        }
    }

    // 전체 알림
    @PostMapping("/all")
    public String sendEvent(@RequestBody NotificationMessageRequest req) {
        try {
            // DB 저장 (전체 알림 → userId null)
            Notification notification = new Notification();
            notification.setUserId(null);
            notification.setType("ALL");
            notification.setMessage(req.getMessage());
            notificationRepository.save(notification);

            // SSE 전송
            SseController.sendToAll("[전체 알림] " + req.getMessage());
            return "전체 알림 전송 완료";
        } catch (Exception e) {
            throw new NotificationException("전체 알림 전송 실패", e);
        }
    }
}
