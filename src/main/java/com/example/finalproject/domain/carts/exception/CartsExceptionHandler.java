package com.example.finalproject.domain.carts.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.finalproject.domain.carts")
public class CartsExceptionHandler {

    // 1) 비즈니스 예외 처리
    @ExceptionHandler(CartsException.class)
    public ResponseEntity<?> handle(CartsException e) {
        return ResponseEntity.status(e.getCode().status)
                .body(Map.of(
                        "error", e.getCode().name(),
                        "message", e.getMessage()
                ));
    }

    // 2) 권한 예외 처리
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403) // Forbidden
                .body(Map.of(
                        "error", "ACCESS_DENIED",
                        "message", e.getMessage()
                ));
    }
}
