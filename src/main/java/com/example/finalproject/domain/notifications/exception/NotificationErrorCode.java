package com.example.finalproject.domain.notifications.exception;

import org.springframework.http.HttpStatus;

public enum NotificationErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 보낼 유저를 찾을 수 없습니다."),
    NOTIFICATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "해당 유저는 알림을 허용하지 않았습니다."),
    DELIVERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "알림 전송에 실패했습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
