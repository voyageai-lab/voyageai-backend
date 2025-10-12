package com.voyageai.voyageaibackend.exception;

import lombok.Getter;

/**
 * Base exception class for business logic errors.
 * Provides a structured way to handle application-specific exceptions.
 */
@Getter
public class BusinessException extends RuntimeException {

  /**
   * Error code for API response.
   */
  private final String errorCode;

  /**
   * HTTP status code to return.
   */
  private final int statusCode;

  /**
   * Constructs a new BusinessException with the specified error message.
   *
   * @param message the detail message
   */
  public BusinessException(String message) {
    super(message);
    this.errorCode = "BUSINESS_ERROR";
    this.statusCode = 400;
  }

  /**
   * Constructs a new BusinessException with message and error code.
   *
   * @param message the detail message
   * @param errorCode the error code
   */
  public BusinessException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.statusCode = 400;
  }

  /**
   * Constructs a new BusinessException with full details.
   *
   * @param message the detail message
   * @param errorCode the error code
   * @param statusCode the HTTP status code
   */
  public BusinessException(String message, String errorCode, int statusCode) {
    super(message);
    this.errorCode = errorCode;
    this.statusCode = statusCode;
  }
}

