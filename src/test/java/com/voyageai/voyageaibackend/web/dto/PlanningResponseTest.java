package com.voyageai.voyageaibackend.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlanningResponse}.
 */
class PlanningResponseTest {

  @Test
  void builder_shouldCreateResponse() {
    // Given
    String taskId = "task-123";
    String message = "Task submitted";

    // When
    PlanningResponse response = PlanningResponse.builder()
        .taskId(taskId)
        .message(message)
        .build();

    // Then
    assertNotNull(response);
    assertEquals(taskId, response.getTaskId());
    assertEquals(message, response.getMessage());
  }

  @Test
  void setters_shouldUpdateFields() {
    // Given
    PlanningResponse response = PlanningResponse.builder().build();

    // When
    response.setTaskId("task-456");
    response.setMessage("Updated message");

    // Then
    assertEquals("task-456", response.getTaskId());
    assertEquals("Updated message", response.getMessage());
  }

  @Test
  void toString_shouldContainFields() {
    // Given
    PlanningResponse response = PlanningResponse.builder()
        .taskId("task-123")
        .message("Test message")
        .build();

    // When
    String result = response.toString();

    // Then
    assertNotNull(result);
  }
}

