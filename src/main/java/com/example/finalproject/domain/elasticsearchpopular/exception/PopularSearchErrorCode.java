package com.example.finalproject.domain.elasticsearchpopular.exception;

import org.springframework.http.HttpStatus;

public enum PopularSearchErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    ELASTIC_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Elasticsearch 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    PopularSearchErrorCode(HttpStatus status, String message) {
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
