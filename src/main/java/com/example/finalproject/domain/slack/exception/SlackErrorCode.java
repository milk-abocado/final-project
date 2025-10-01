package com.example.finalproject.domain.slack.exception;

import org.springframework.http.HttpStatus;

/**
 * Slack 관련 에러 코드 Enum
 * - HttpStatus + 기본 메시지 매핑
 * - SlackException과 함께 사용
 */
public enum SlackErrorCode {
    SEND_MESSAGE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "슬랙 메시지 전송 실패"),
    INVALID_CHANNEL(HttpStatus.BAD_REQUEST, "잘못된 채널 지정"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "슬랙 인증 실패"),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "알 수 없는 슬랙 오류");

    public final HttpStatus status; // HTTP 상태 코드
    public final String message;    // 기본 메시지

    SlackErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
