package com.example.finalproject.domain.notifications.exception;

public class NotificationException extends RuntimeException {
    private final NotificationErrorCode errorCode;

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public NotificationErrorCode getErrorCode() {
        return errorCode;
    }
}
