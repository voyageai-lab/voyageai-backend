package com.voyageai.voyageaibackend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BusinessException}.
 * Tests all constructors and getter methods.
 */
class BusinessExceptionTest {

  @Test
  void constructor_withMessageOnly() {
    String message = "Test business error";
    
    BusinessException exception = new BusinessException(message);
    
    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
    assertEquals("BUSINESS_ERROR", exception.getErrorCode());
    assertEquals(400, exception.getStatusCode());
  }

  @Test
  void constructor_withMessageAndErrorCode() {
    String message = "Custom business error";
    String errorCode = "CUSTOM_ERROR";
    
    BusinessException exception = new BusinessException(message, errorCode);
    
    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
    assertEquals(errorCode, exception.getErrorCode());
    assertEquals(400, exception.getStatusCode());
  }

  @Test
  void constructor_withAllParameters() {
    String message = "Conflict error";
    String errorCode = "CONFLICT";
    int statusCode = 409;
    
    BusinessException exception = new BusinessException(message, errorCode, statusCode);
    
    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
    assertEquals(errorCode, exception.getErrorCode());
    assertEquals(statusCode, exception.getStatusCode());
  }

  @Test
  void exceptionInheritance() {
    BusinessException exception = new BusinessException("Test");
    
    // Verify it extends RuntimeException
    assertEquals(RuntimeException.class, exception.getClass().getSuperclass());
  }
}

