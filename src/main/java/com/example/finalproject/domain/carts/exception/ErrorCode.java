package com.example.finalproject.domain.carts.exception;

import org.springframework.http.HttpStatus;

/**
 * 장바구니 전용 에러 코드 Enum
 * - HttpStatus + 기본 메시지 매핑
 * - ApiException과 함께 사용
 */
public enum ErrorCode {
    // 인증/권한
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),                    // 401
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),                       // 403

    // 요청 오류
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),                     // 400

    // 메뉴/가게 관련
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 메뉴입니다."),               // 404
    MENU_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "해당 메뉴는 주문할 수 없습니다."),      // 400
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 가게입니다."),             // 404
    STORE_CLOSED(HttpStatus.BAD_REQUEST, "현재 영업 시간이 아닙니다."),             // 400
    GONE(HttpStatus.GONE, "폐업한 가게입니다."),                                   // 410

    // 장바구니 관련
    CART_EMPTY(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다."),                 // 400
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "장바구니 아이템을 찾을 수 없습니다."),// 404

    // 충돌/중복
    CONFLICT(HttpStatus.CONFLICT, "중복 또는 제약 조건 위반"),                     // 409
    CONFLICT_STORE(HttpStatus.CONFLICT, "다른 가게의 메뉴가 존재합니다."),          // 409

    // 일반 리소스
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.");                 // 404


    public final HttpStatus status; // HTTP 상태 코드
    public final String message;    // 기본 메시지

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
