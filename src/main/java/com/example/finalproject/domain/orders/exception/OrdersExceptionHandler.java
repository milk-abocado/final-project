package com.example.finalproject.domain.orders.exception;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.example.finalproject.domain.orders")
public class OrdersExceptionHandler {

    // 1) 비즈니스 예외 처리
    @ExceptionHandler(OrdersException.class)
    public ResponseEntity<?> handle(OrdersException e) {
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
