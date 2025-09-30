package com.example.finalproject.domain.slack.exception;

/**
 * Slack 관련 예외 처리 클래스
 * - RuntimeException을 상속하여 Unchecked 예외로 사용
 * - SlackErrorCode(상태/메시지) + 추가 메시지를 함께 전달
 */
public class SlackException extends RuntimeException {

    // 에러 코드 (HttpStatus + 기본 메시지 포함)
    private final SlackErrorCode code;

    /**
     * 생성자
     * @param code 에러 코드 (예: SEND_MESSAGE_FAILED 등)
     * @param message 사용자에게 보여줄 상세 메시지
     */
    public SlackException(SlackErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public SlackException(SlackErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public SlackErrorCode getCode() {
        return code;
    }
}
