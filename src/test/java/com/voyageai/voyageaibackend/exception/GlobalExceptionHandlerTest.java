package com.voyageai.voyageaibackend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.voyageai.voyageaibackend.exception.GlobalExceptionHandler.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 */
class GlobalExceptionHandlerTest {

  private GlobalExceptionHandler exceptionHandler;
  private WebRequest webRequest;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler();
    MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    servletRequest.setRequestURI("/api/test");
    webRequest = new ServletWebRequest(servletRequest);
  }

  @Test
  void handleBusinessException_returnsCorrectResponse() {
    BusinessException exception = new BusinessException(
        "Test error message",
        "TEST_ERROR",
        409
    );

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(
        exception, webRequest
    );

    assertNotNull(response.getBody());
    assertEquals(409, response.getBody().getStatus());
    assertEquals("Test error message", response.getBody().getMessage());
    assertEquals("TEST_ERROR", response.getBody().getErrorCode());
    assertEquals("/api/test", response.getBody().getPath());
  }

  @Test
  void handleAuthenticationException_returnsUnauthorized() {
    AuthenticationException exception = new AuthenticationException("Invalid credentials");

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(
        exception, webRequest
    );

    assertNotNull(response.getBody());
    assertEquals(401, response.getBody().getStatus());
    assertEquals("AUTHENTICATION_FAILED", response.getBody().getErrorCode());
  }

  @Test
  void handleResourceNotFoundException_returnsNotFound() {
    ResourceNotFoundException exception = new ResourceNotFoundException("User", 123L);

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(
        exception, webRequest
    );

    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getStatus());
    assertEquals("RESOURCE_NOT_FOUND", response.getBody().getErrorCode());
  }

  @Test
  void handleValidationException_returnsFieldErrors() {
    // Create a mock MethodArgumentNotValidException
    BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
        new Object(), "testObject"
    );
    bindingResult.addError(new FieldError(
        "testObject", "email", "Email must be valid"
    ));
    bindingResult.addError(new FieldError(
        "testObject", "password", "Password must be at least 8 characters long"
    ));

    MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
        null, bindingResult
    );

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(
        exception, webRequest
    );
    assertNotNull(response.getBody());
    assertEquals(400, response.getBody().getStatus());
    assertEquals("VALIDATION_ERROR", response.getBody().getErrorCode());
    assertEquals("Input validation failed", response.getBody().getMessage());
    assertNotNull(response.getBody().getFieldErrors());
    assertEquals(2, response.getBody().getFieldErrors().size());
  }

  @Test
  void handleGlobalException_returnsInternalServerError() {
    Exception exception = new RuntimeException("Unexpected error");

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(
        exception, webRequest
    );

    assertNotNull(response.getBody());
    assertEquals(500, response.getBody().getStatus());
    assertEquals("INTERNAL_ERROR", response.getBody().getErrorCode());
    assertEquals("An unexpected error occurred", response.getBody().getMessage());
  }

  @Test
  void handleGlobalException_nullPointerException() {
    NullPointerException exception = new NullPointerException("Null value encountered");

    ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(
        exception, webRequest
    );

    assertNotNull(response.getBody());
    assertEquals(500, response.getBody().getStatus());
  }
}

