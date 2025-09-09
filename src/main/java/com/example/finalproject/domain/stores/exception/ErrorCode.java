package com.example.finalproject.domain.stores.exception;

import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역에서 사용하는 에러 코드 Enum
 * - HttpStatus + 기본 메시지 매핑
 * - ApiException과 함께 사용
 */
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),       // 401
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),          // 403
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),         // 400
    CONFLICT(HttpStatus.CONFLICT, "중복 또는 제약 조건 위반"),         // 409
    LIMIT_EXCEEDED(HttpStatus.CONFLICT, "운영 가능 가게 수 초과"),     // 409 (비즈니스 룰 위반)
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),      // 404
    GONE(HttpStatus.GONE, "폐업한 가게입니다.");                       // 410

    public final HttpStatus status; // HTTP 상태 코드
    public final String message;    // 기본 메시지

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
