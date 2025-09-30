package com.example.finalproject.domain.coupons.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.finalproject.domain.coupons")
public class CouponExceptionHandler {

    @ExceptionHandler(CouponException.class)
    public ResponseEntity<Map<String, Object>> handleCouponException(CouponException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", e.getErrorCode().getStatus().value(),
                        "error", e.getErrorCode().name(),
                        "message", e.getErrorCode().getMessage()
                ));
    }
}
