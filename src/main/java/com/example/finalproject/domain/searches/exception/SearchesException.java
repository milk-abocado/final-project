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

@ControllerAdvice
public class SearchesException {
    @ExceptionHandler
    public ResponseEntity<String> handleBadRequest(BadRequestException exception) {
        return ResponseEntity.status(400).body(exception.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handleUnauthorized(UnauthorizedException exception) {
        return ResponseEntity.status(401).body(exception.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(404).body(exception.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}