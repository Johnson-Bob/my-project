package com.example.util.http;

import org.springframework.http.HttpStatus;

public record HttpErrorInfo(HttpStatus httpStatus, String path, String message) {

  public int getStatus() {
    return httpStatus.value();
  }

  public String getError() {
    return httpStatus.getReasonPhrase();
  }

}
