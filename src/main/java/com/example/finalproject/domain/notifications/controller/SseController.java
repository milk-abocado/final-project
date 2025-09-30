package com.example.finalproject.domain.notifications.controller;

import com.example.finalproject.domain.notifications.exception.NotificationErrorCode;
import com.example.finalproject.domain.notifications.exception.NotificationException;
import com.example.finalproject.domain.users.entity.Users;
import com.example.finalproject.domain.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@RestController
public class SseController {

    private final UsersRepository usersRepository;

    // 접속 중인 사용자 (userId -> emitter)
    private static final Map<Long, SseEmitter> clients = new ConcurrentHashMap<>();

    // 사용자 구독 (브라우저/앱이 이 엔드포인트 열어둠)
    @GetMapping("/subscribe/{userId}")
    public SseEmitter subscribe(@PathVariable Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getAllowNotifications())) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_ALLOWED);
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        clients.put(userId, emitter);

        emitter.onCompletion(() -> clients.remove(userId));
        emitter.onTimeout(() -> clients.remove(userId));
        emitter.onError((e) -> clients.remove(userId));

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("SSE 연결이 완료되었습니다."));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    // 특정 사용자에게 전송 (쿠폰 등)
    public static void sendToUser(Long userId, String message) {
        SseEmitter emitter = clients.get(userId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(message));
        } catch (IOException e) {
            clients.remove(userId);
            emitter.completeWithError(e);
        }
    }

    // 전체 사용자에게 전송 (이벤트, 공지사항 등 )
    public static void sendToAll(String message) {
        for (Map.Entry<Long, SseEmitter> entry : clients.entrySet()) {
            SseEmitter emitter = entry.getValue();
            try {
                emitter.send(SseEmitter.event()
                        .name("broadcast")
                        .data(message));
            } catch (IOException e) {
                clients.remove(entry.getKey());
                emitter.completeWithError(e);
            }
        }
    }
}
