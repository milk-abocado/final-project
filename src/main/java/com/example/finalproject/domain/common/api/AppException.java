package com.example.finalproject.domain.common.api;

public class AppException extends RuntimeException {
    public final ErrorCode error;
    public AppException(ErrorCode error) { super(error.defaultMessage); this.error = error; }
    public AppException(ErrorCode error, String msg) { super(msg); this.error = error; }
}