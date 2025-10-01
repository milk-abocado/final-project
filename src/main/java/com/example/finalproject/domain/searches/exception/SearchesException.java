package com.example.finalproject.domain.searches.exception;

public class SearchesException extends RuntimeException {
  private final SearchesErrorCode errorCode;

  public SearchesException(SearchesErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public SearchesErrorCode getErrorCode() {
    return errorCode;
  }
}
