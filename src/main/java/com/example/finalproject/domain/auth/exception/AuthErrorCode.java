package com.example.finalproject.domain.auth.exception;

import org.springframework.http.HttpStatus;

public enum AuthErrorCode {
    // 공통
    REQUEST_BODY_INVALID(HttpStatus.BAD_REQUEST, "요청 본문이 유효하지 않습니다."),
    PARAMETER_MISSING(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 로그인
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일/비밀번호가 일치하지 않습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.FORBIDDEN, "사용자 정보가 없거나 사용할 수 없습니다."),
    LOGIN_IN_PROGRESS(HttpStatus.CONFLICT, "이미 로그인 되었습니다."),
    SESSION_EXISTS(HttpStatus.CONFLICT, "이미 로그인된 세션이 있습니다. 먼저 로그아웃하세요."),

    //로그아웃
    ALREADY_LOGGED_OUT(HttpStatus.CONFLICT, "이미 로그아웃된 세션입니다."),


    // 토큰 갱신
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "잘못된 리프레시 토큰"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.GONE, "리프레시 토큰 만료"),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "잘못된 인증 토큰"),

    // 회원가입
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이메일이 중복되었습니다."),
    EMAIL_VERIFICATION_INVALID_CODE(HttpStatus.BAD_REQUEST, "잘못된 코드"),

    // 비밀번호 재설정
    PASSWORD_RESET_EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 이메일로 가입된 계정 없음"),
    PASSWORD_RESET_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "소셜 계정은 비번 재설정 불가"),
    PASSWORD_RESET_INVALID_CODE(HttpStatus.BAD_REQUEST, "잘못된 코드"),
    PASSWORD_RESET_EXPIRED(HttpStatus.GONE, "만료된 코드"),
    PASSWORD_POLICY_VIOLATION(HttpStatus.BAD_REQUEST, "비밀번호 정책 위반"),
    PASSWORD_OLD_MISMATCH(HttpStatus.BAD_REQUEST, "기존 비밀번호 불일치"),

    // 소셜 가입
    SOCIAL_PARAM_INVALID(HttpStatus.BAD_REQUEST, "파라미터가 올바르지 않습니다."),
    SOCIAL_PROVIDER_DUPLICATE(HttpStatus.CONFLICT, "(provider, provider_id) 중복");

    public final HttpStatus status;
    public final String defaultMessage;
    AuthErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status; this.defaultMessage = defaultMessage;
    }
}