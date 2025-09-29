package com.example.finalproject.domain.points.exception;

public class PointException extends RuntimeException {
    private final PointErrorCode errorCode;

    public PointException(PointErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PointErrorCode getErrorCode() {
        return errorCode;
    }
}
