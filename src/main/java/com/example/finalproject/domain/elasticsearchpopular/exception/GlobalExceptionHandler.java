package com.example.finalproject.domain.elasticsearchpopular.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PopularSearchException.class)
    public ResponseEntity<String> handlePopularSearchException(PopularSearchException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ex.getMessage());
    }
}
