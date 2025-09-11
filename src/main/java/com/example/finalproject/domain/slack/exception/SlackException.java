package com.example.finalproject.domain.slack.exception;

public class SlackException extends RuntimeException{
    public SlackException(String message, Throwable cause) {
        super(message,cause);
    }
}
