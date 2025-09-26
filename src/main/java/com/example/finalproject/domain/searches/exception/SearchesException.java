package com.example.finalproject.domain.searches.exception;

import org.springframework.http.HttpStatus;

public class SearchesException extends RuntimeException {
  private final HttpStatus status;

  public SearchesException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }

  // 편의 메서드 (정적 팩토리 메서드)
  public static SearchesException badRequest(String message) {
    return new SearchesException(HttpStatus.BAD_REQUEST, message);
  }

  public static SearchesException unauthorized(String message) {
    return new SearchesException(HttpStatus.UNAUTHORIZED, message);
  }

  public static SearchesException forbidden(String message) {
    return new SearchesException(HttpStatus.FORBIDDEN, message);
  }

  public static SearchesException notFound(String message) {
    return new SearchesException(HttpStatus.NOT_FOUND, message);
  }
}
