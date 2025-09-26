package com.example.finalproject.domain.common.api;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "EMAIL_NOT_FOUND", "해당 이메일로 가입된 계정 없음"),
    SOCIAL_ACCOUNT_ONLY(HttpStatus.CONFLICT, "SOCIAL_ACCOUNT_ONLY", "소셜 로그인 계정"),
    RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT", "너무 자주 요청(쿨다운)"),
    INVALID_CODE(HttpStatus.BAD_REQUEST, "INVALID_CODE", "잘못된 코드"),
    CODE_EXPIRED(HttpStatus.GONE, "CODE_EXPIRED", "만료"),
    CODE_ALREADY_USED(HttpStatus.CONFLICT, "CODE_ALREADY_USED", "이미 사용됨"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증 필요"),
    INVALID_OLD_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_OLD_PASSWORD", "기존 비번 불일치"),
    PASSWORD_POLICY(HttpStatus.BAD_REQUEST, "PASSWORD_POLICY", "비밀번호 정책 위반");
    public final HttpStatus status; public final String code; public final String defaultMessage;
    ErrorCode(HttpStatus s, String c, String m){ this.status=s; this.code=c; this.defaultMessage=m; }
}

