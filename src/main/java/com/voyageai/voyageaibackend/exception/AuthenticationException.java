package com.voyageai.voyageaibackend.exception;

/**
 * Exception thrown when authentication fails.
 * Typically results in HTTP 401 status.
 */
public class AuthenticationException extends BusinessException {

  /**
   * Constructs a new AuthenticationException.
   *
   * @param message the detail message
   */
  public AuthenticationException(String message) {
    super(message, "AUTHENTICATION_FAILED", 401);
  }
}

