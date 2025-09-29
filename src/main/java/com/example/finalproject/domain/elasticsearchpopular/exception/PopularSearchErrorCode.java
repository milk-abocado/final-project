package com.example.finalproject.domain.elasticsearchpopular.exception;

import org.springframework.http.HttpStatus;

public enum PopularSearchErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "데이터를 찾을 수 없습니다."),
    ELASTIC_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Elasticsearch 처리 중 오류가 발생했습니다."),
    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 처리 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    PopularSearchErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
