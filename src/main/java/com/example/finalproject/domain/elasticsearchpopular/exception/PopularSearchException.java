package com.example.finalproject.domain.elasticsearchpopular.exception;

public class PopularSearchException extends RuntimeException {

    private final PopularSearchErrorCode errorCode;

    public PopularSearchException(PopularSearchErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public PopularSearchException(PopularSearchErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }

    public PopularSearchErrorCode getErrorCode() {
        return errorCode;
    }
}
