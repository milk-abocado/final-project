package com.example.finalproject.domain.points.exception;

import org.springframework.http.HttpStatus;

public enum PointErrorCode {

    // 인증/인가 관련
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 포인트 적립/사용 관련
    POINTS_INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "포인트 금액은 0보다 커야 합니다."),
    POINTS_EXCEEDS_AVAILABLE(HttpStatus.BAD_REQUEST, "보유 포인트보다 많은 금액은 사용할 수 없습니다."),
    POINTS_NOT_ACCUMULATED_WITH_COUPON(HttpStatus.CONFLICT, "할인 쿠폰 적용 주문에는 포인트 적립이 불가합니다."),

    // 사용자 조회 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 유저가 없습니다.");

    private final HttpStatus status;
    private final String message;

    PointErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
