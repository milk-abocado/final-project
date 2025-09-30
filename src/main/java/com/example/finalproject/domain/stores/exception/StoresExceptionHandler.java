package com.example.finalproject.domain.stores.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class StoresExceptionHandler {

    // 1) 비즈니스 예외는 그대로 포맷 유지
    @ExceptionHandler(StoresApiException.class)
    public ResponseEntity<?> handle(StoresApiException e) {
        return ResponseEntity.status(e.getCode().status)
                .body(Map.of(
                        "error", e.getCode().name(),
                        "message", e.getMessage()
                ));
    }

    // 2) Bean Validation 예외를 전부 커스텀 문구로 치환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<Map<String, Object>> messages = new ArrayList<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            String field = error.getField();           // 필드 이름
            String message = error.getDefaultMessage(); // 커스터마이즈된 메시지

            messages.add(Map.of(
                    "field", field,
                    "message", Objects.requireNonNull(message),
                    "rejectedValue", Objects.requireNonNull(error.getRejectedValue())  // 사용자가 입력한 잘못된 값
            ));
        }

        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION",
                "messages", messages
        ));
    }
}
