package com.example.finalproject.domain.coupons.exception;

import org.springframework.http.HttpStatus;

public enum CouponErrorCode {

    // 인증/인가 관련
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 쿠폰 생성/등록 관련
    COUPON_CODE_CONFLICT(HttpStatus.CONFLICT, "이미 존재하는 쿠폰 코드입니다."),
    COUPON_EXPIRED(HttpStatus.CONFLICT, "만료된 쿠폰입니다."),
    USER_COUPON_DUPLICATE(HttpStatus.CONFLICT, "이미 동일한 쿠폰을 보유하고 있습니다."),

    // 쿠폰 조회 관련
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰 코드가 존재하지 않습니다."),
    USER_COUPONS_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자가 보유한 쿠폰이 없습니다."),

    // 쿠폰 사용 관련
    USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 쿠폰이 존재하지 않습니다."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용한 쿠폰입니다."),
    COUPON_DUPLICATE_USE(HttpStatus.CONFLICT, "쿠폰은 1개만 사용 가능합니다."),

    // 사용자 조회 관련 (쿠폰 등록/조회 시 유저 없음)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당하는 유저가 없습니다.");

    private final HttpStatus status;
    private final String message;

    CouponErrorCode(HttpStatus status, String message) {
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
