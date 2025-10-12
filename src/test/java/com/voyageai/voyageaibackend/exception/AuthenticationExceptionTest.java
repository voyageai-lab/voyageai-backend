package com.voyageai.voyageaibackend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AuthenticationException}.
 */
class AuthenticationExceptionTest {

  @Test
  void constructor_setsCorrectValues() {
    String message = "Invalid credentials";
    
    AuthenticationException exception = new AuthenticationException(message);
    
    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
    assertEquals("AUTHENTICATION_FAILED", exception.getErrorCode());
    assertEquals(401, exception.getStatusCode());
  }

  @Test
  void exceptionInheritance() {
    AuthenticationException exception = new AuthenticationException("Test");
    
    // Verify it extends BusinessException
    assertEquals(BusinessException.class, exception.getClass().getSuperclass());
  }
}

