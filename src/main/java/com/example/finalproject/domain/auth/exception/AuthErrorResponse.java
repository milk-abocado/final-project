package com.example.finalproject.domain.auth.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthErrorResponse(
        boolean error,
        int status,
        String code,
        String message,
        String path,
        Instant timestamp,
        Object details,
        List<FieldError> fieldErrors
) {
    public static AuthErrorResponse of(AuthErrorCode code, String message, String path, Object details, List<FieldError> fields) {
        return new AuthErrorResponse(true, code.status.value(), code.name(),
                message == null ? code.defaultMessage : message,
                path, Instant.now(), details, fields);
    }
    public record FieldError(String field, String reason) {}
}