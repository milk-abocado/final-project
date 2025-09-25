package com.example.finalproject.domain.notifications.service;

import com.example.finalproject.domain.notifications.controller.SseController;
import com.example.finalproject.domain.notifications.entity.Notification;
import com.example.finalproject.domain.notifications.entity.Notification.Status;
import com.example.finalproject.domain.notifications.entity.Notification.Type;
import com.example.finalproject.domain.notifications.repository.NotificationRepository;
import com.example.finalproject.domain.slack.service.SlackService;
import com.example.finalproject.domain.notifications.controller.SseController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SlackService slackService;
    private final SseController sseController;

    // 개인 알림
    public void sendUserNotification(Long userId, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Type.USER);
        notification.setMessage(message);

        try {
            slackService.sendUserMessage("[개인 알림] " + message);
            sseController.sendToUser(userId, "[개인 알림] " + message);
            notification.setStatus(Status.SUCCESS);
        } catch (Exception e) {
            notification.setStatus(Status.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        notificationRepository.save(notification);
    }

    // 전체 알림
    public void sendBroadcastNotification(String message) {
        Notification notification = new Notification();
        notification.setUserId(null);
        notification.setType(Type.ALL);
        notification.setMessage(message);

        try {
            slackService.sendAllUserMessage("[전체 알림] " + message);
            sseController.sendToAll("[전체 알림] " + message);
            notification.setStatus(Status.SUCCESS);
        } catch (Exception e) {
            notification.setStatus(Status.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        notificationRepository.save(notification);
    }
}
