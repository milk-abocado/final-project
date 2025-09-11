package com.example.finalproject.domain.auth.exception;

public class AuthApiException extends RuntimeException {
    private final AuthErrorCode errorCode;
    private final transient Object details;

    public AuthApiException(AuthErrorCode errorCode) {
        super(errorCode.defaultMessage);
        this.errorCode = errorCode; this.details = null;
    }
    public AuthApiException(AuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode; this.details = null;
    }
    public AuthApiException(AuthErrorCode errorCode, Object details) {
        super(errorCode.defaultMessage);
        this.errorCode = errorCode; this.details = details;
    }

    public AuthErrorCode getErrorCode() { return errorCode; }
    public Object getDetails() { return details; }

    public static AuthApiException of(AuthErrorCode code){ return new AuthApiException(code); }
}