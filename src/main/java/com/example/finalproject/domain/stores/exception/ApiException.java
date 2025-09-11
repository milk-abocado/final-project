package com.example.finalproject.domain.stores.exception;

/**
 * 애플리케이션 공통 예외 처리 클래스
 * - RuntimeException을 상속하여 Unchecked 예외로 사용
 * - ErrorCode(상태/메시지) + 추가 메시지를 함께 전달
 */
public class ApiException extends RuntimeException {

    // 에러 코드 (HttpStatus + 기본 메시지 포함)
    private final ErrorCode code;

    /**
     * 생성자
     * @param code 에러 코드 (예: NOT_FOUND, FORBIDDEN 등)
     * @param message 사용자에게 보여줄 상세 메시지
     */
    public ApiException(ErrorCode code, String message) {
        super(message);   // RuntimeException의 message 필드에 저장
        this.code = code;
    }

    /**
     * 에러 코드 반환
     */
    public ErrorCode getCode() {
        return code;
    }
}
