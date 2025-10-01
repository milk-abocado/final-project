package com.example.finalproject.domain.menus.exception;

import org.springframework.http.HttpStatus;

/**
 * 메뉴 전용 에러 코드 Enum
 * - HttpStatus + 기본 메시지 매핑
 * - ApiException과 함께 사용
 */
public enum ErrorCode {
    // 인증/권한
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),                                 // 401
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),                                    // 403

    // 요청 오류
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),                                   // 400

    // 메뉴 관련
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 메뉴입니다."),                            // 404
    MENU_DELETED(HttpStatus.GONE, "삭제된 메뉴입니다."),                                         // 410
    MENU_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "해당 메뉴는 주문할 수 없습니다."),                   // 400
    DUPLICATE_MENU_NAME(HttpStatus.CONFLICT, "이미 존재하는 메뉴 이름입니다."),                   // 409
    MENU_DUPLICATE_NAME(HttpStatus.CONFLICT, "동일한 이름의 메뉴가 이미 존재합니다."),             // 409
    MENU_INVALID_STATUS(HttpStatus.BAD_REQUEST, "유효하지 않은 메뉴 상태입니다."),                // 400
    MENU_PRICE_INVALID(HttpStatus.BAD_REQUEST, "메뉴 가격은 0원 초과여야 합니다."),               // 400
    MENU_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "메뉴 삭제는 이곳에서 할 수 없습니다."),       // 400
    MENU_RESTORE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "삭제된 메뉴만 복구할 수 있습니다."),         // 400

    // 가게 관련
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 가게입니다."),                           // 404
    STORE_CLOSED(HttpStatus.BAD_REQUEST, "현재 영업 시간이 아닙니다."),                           // 400
    GONE(HttpStatus.GONE, "폐업한 가게입니다."),                                                 // 410

    // 카테고리
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카테고리입니다."),                    // 404
    CATEGORY_NOT_BELONG_TO_MENU(HttpStatus.BAD_REQUEST, "해당 메뉴의 카테고리가 아닙니다."),      // 400

    // 옵션 관련
    OPTION_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 옵션 그룹입니다."),               // 404
    OPTION_GROUP_NOT_BELONG_TO_MENU(HttpStatus.BAD_REQUEST, "해당 메뉴의 옵션 그룹이 아닙니다."), // 400
    OPTION_INVALID_SELECTION(HttpStatus.BAD_REQUEST, "옵션의 선택 수 설정이 잘못되었습니다."),     // 400
    OPTION_CHOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 옵션 선택지입니다."),            // 404
    OPTION_CHOICE_NOT_BELONG_TO_GROUP(HttpStatus.BAD_REQUEST, "해당 메뉴의 선택지가 아닙니다."),  // 400

    // 충돌/중복
    CONFLICT(HttpStatus.CONFLICT, "중복 또는 제약 조건 위반"),                                   // 409

    // 일반 리소스
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.");                               // 404


    public final HttpStatus status; // HTTP 상태 코드
    public final String message;    // 기본 메시지

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
