package com.example.finalproject.domain.elasticsearchpopular.exception;

public class PopularSearchException extends RuntimeException {

    private final PopularSearchErrorCode errorCode;

    //enum만 넣어서 메시지 자동 사용
    public PopularSearchException(PopularSearchErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    //enum + 커스텀 메시지
    public PopularSearchException(PopularSearchErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public PopularSearchErrorCode getErrorCode() {
        return errorCode;
    }
}
