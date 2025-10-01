package com.example.finalproject.domain.auth.exception;

import com.example.finalproject.domain.carts.exception.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.example.finalproject")
public class AuthExceptionHandler {

    @ExceptionHandler(AuthApiException.class)
    public ResponseEntity<AuthErrorResponse> handleAuth(AuthApiException ex, HttpServletRequest req) {
        var c = ex.getErrorCode();
        var body = AuthErrorResponse.of(c, ex.getMessage(), req.getRequestURI(), ex.getDetails(), null);
        return ResponseEntity.status(c.status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new AuthErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .collect(Collectors.toList());
        var body = AuthErrorResponse.of(AuthErrorCode.REQUEST_BODY_INVALID, null, req.getRequestURI(), null, fields);
        return ResponseEntity.status(AuthErrorCode.REQUEST_BODY_INVALID.status).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<AuthErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        var fields = ex.getConstraintViolations().stream()
                .map(v -> new AuthErrorResponse.FieldError(v.getPropertyPath().toString(), v.getMessage()))
                .collect(Collectors.toList());
        var body = AuthErrorResponse.of(AuthErrorCode.PARAMETER_MISSING, null, req.getRequestURI(), null, fields);
        return ResponseEntity.status(AuthErrorCode.PARAMETER_MISSING.status).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AuthErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        var body = AuthErrorResponse.of(AuthErrorCode.INVALID_ACCESS_TOKEN, ex.getMessage(), req.getRequestURI(), null, null);
        return ResponseEntity.status(AuthErrorCode.INVALID_ACCESS_TOKEN.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthErrorResponse> handleEtc(Exception ex, HttpServletRequest req) {
        var body = AuthErrorResponse.of(AuthErrorCode.INTERNAL_ERROR, ex.getMessage(), req.getRequestURI(), null, null);
        return ResponseEntity.status(AuthErrorCode.INTERNAL_ERROR.status).body(body);
    }
}