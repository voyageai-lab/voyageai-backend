package com.voyageai.voyageaibackend.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ResourceNotFoundException}.
 */
class ResourceNotFoundExceptionTest {

  @Test
  void constructor_withMessageOnly() {
    String message = "Resource not found";
    
    ResourceNotFoundException exception = new ResourceNotFoundException(message);
    
    assertNotNull(exception);
    assertEquals(message, exception.getMessage());
    assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
    assertEquals(404, exception.getStatusCode());
  }

  @Test
  void constructor_withResourceTypeAndId() {
    String resourceType = "User";
    Long resourceId = 123L;
    
    ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId);
    
    assertNotNull(exception);
    assertTrue(exception.getMessage().contains(resourceType));
    assertTrue(exception.getMessage().contains(resourceId.toString()));
    assertEquals("User not found with id: 123", exception.getMessage());
    assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
    assertEquals(404, exception.getStatusCode());
  }

  @Test
  void constructor_withStringId() {
    String resourceType = "TravelPlan";
    String resourceId = "plan-uuid-123";
    
    ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId);
    
    assertNotNull(exception);
    assertEquals("TravelPlan not found with id: plan-uuid-123", exception.getMessage());
    assertEquals("RESOURCE_NOT_FOUND", exception.getErrorCode());
    assertEquals(404, exception.getStatusCode());
  }

  @Test
  void exceptionInheritance() {
    ResourceNotFoundException exception = new ResourceNotFoundException("Test");
    
    // Verify it extends BusinessException
    assertEquals(BusinessException.class, exception.getClass().getSuperclass());
  }
}

