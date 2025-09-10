package com.example.finalproject.domain.searches.exception;

import org.apache.coyote.BadRequestException;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

public class SearchesException extends RuntimeException {
    private final HttpStatus status;

    public SearchesException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static SearchesException badRequest(String message) {
        return new SearchesException(HttpStatus.BAD_REQUEST, message);
    }

    public static SearchesException unauthorized(String message) {
        return new SearchesException(HttpStatus.UNAUTHORIZED, message);
    }

    public static SearchesException notFound(String message) {
        return new SearchesException(HttpStatus.NOT_FOUND, message);
    }
}