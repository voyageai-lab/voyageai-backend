package com.voyageai.voyageaibackend.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for REST API.
 * Provides centralized exception handling across all controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles BusinessException and its subclasses.
   *
   * @param ex the exception
   * @param request the web request
   * @return error response entity
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(
      BusinessException ex,
      WebRequest request
  ) {
    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(Instant.now())
        .status(ex.getStatusCode())
        .error(HttpStatus.valueOf(ex.getStatusCode()).getReasonPhrase())
        .message(ex.getMessage())
        .errorCode(ex.getErrorCode())
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.valueOf(ex.getStatusCode()));
  }

  /**
   * Handles validation errors from @Valid annotation.
   *
   * @param ex the validation exception
   * @param request the web request
   * @return error response entity with field-level errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException ex,
      WebRequest request
  ) {
    Map<String, String> fieldErrors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      fieldErrors.put(fieldName, errorMessage);
    });

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(Instant.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Failed")
        .message("Input validation failed")
        .errorCode("VALIDATION_ERROR")
        .path(request.getDescription(false).replace("uri=", ""))
        .fieldErrors(fieldErrors)
        .build();

    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handles all other unhandled exceptions.
   *
   * @param ex the exception
   * @param request the web request
   * @return error response entity
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(
      Exception ex,
      WebRequest request
  ) {
    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(Instant.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error("Internal Server Error")
        .message("An unexpected error occurred")
        .errorCode("INTERNAL_ERROR")
        .path(request.getDescription(false).replace("uri=", ""))
        .build();

    // Log the full exception for debugging
    ex.printStackTrace();

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Standard error response structure.
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class ErrorResponse {
    /**
     * Timestamp when the error occurred.
     */
    private Instant timestamp;

    /**
     * HTTP status code.
     */
    private int status;

    /**
     * Error type description.
     */
    private String error;

    /**
     * Detailed error message.
     */
    private String message;

    /**
     * Application-specific error code.
     */
    private String errorCode;

    /**
     * Request path that caused the error.
     */
    private String path;

    /**
     * Field-level validation errors (for validation failures).
     */
    private Map<String, String> fieldErrors;
  }
}

