package com.example.finalproject.domain.slack.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Slack 관련 예외를 전역적으로 처리
 */
@RestControllerAdvice
public class SlackExceptionHandler {

    @ExceptionHandler(SlackException.class)
    public ResponseEntity<?> handle(SlackException e) {
        return ResponseEntity.status(e.getCode().status)
                .body(Map.of(
                        "error", e.getCode().name(),
                        "message", e.getMessage()
                ));
    }
}
