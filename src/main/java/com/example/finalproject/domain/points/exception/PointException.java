package com.example.finalproject.domain.points.exception;

public class PointException extends RuntimeException {

    public PointException(String message) {
        super(message);
    }

    public PointException(String message, Throwable cause) {
        super(message, cause);
    }
}