package com.voyageai.voyageaibackend.web.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TaskStatusResponse}.
 */
class TaskStatusResponseTest {

  @Test
  void builder_shouldCreateResponse() {
    // Given
    String taskId = "task-123";
    TaskStatus status = TaskStatus.COMPLETED;
    String requirements = "Plan a 7-day trip to Tokyo";
    String result = "{\"plan\": \"data\"}";
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();

    // When
    TaskStatusResponse response = TaskStatusResponse.builder()
        .taskId(taskId)
        .status(status)
        .requirements(requirements)
        .result(result)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    // Then
    assertNotNull(response);
    assertEquals(taskId, response.getTaskId());
    assertEquals(status, response.getStatus());
    assertEquals(requirements, response.getRequirements());
    assertEquals(result, response.getResult());
    assertEquals(createdAt, response.getCreatedAt());
    assertEquals(updatedAt, response.getUpdatedAt());
    assertNull(response.getErrorMessage());
  }

  @Test
  void builder_withError_shouldCreateResponse() {
    // Given
    String taskId = "task-123";
    TaskStatus status = TaskStatus.FAILED;
    String errorMessage = "API call failed";
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();

    // When
    TaskStatusResponse response = TaskStatusResponse.builder()
        .taskId(taskId)
        .status(status)
        .errorMessage(errorMessage)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    // Then
    assertNotNull(response);
    assertEquals(taskId, response.getTaskId());
    assertEquals(status, response.getStatus());
    assertEquals(errorMessage, response.getErrorMessage());
    assertNull(response.getResult());
  }

  @Test
  void setters_shouldUpdateFields() {
    // Given
    TaskStatusResponse response = TaskStatusResponse.builder().build();

    // When
    response.setTaskId("task-456");
    response.setStatus(TaskStatus.PROCESSING);
    response.setRequirements("Plan a trip to Paris");
    response.setResult("result");
    response.setErrorMessage("error");
    Instant now = Instant.now();
    response.setCreatedAt(now);
    response.setUpdatedAt(now);

    // Then
    assertEquals("task-456", response.getTaskId());
    assertEquals(TaskStatus.PROCESSING, response.getStatus());
    assertEquals("Plan a trip to Paris", response.getRequirements());
    assertEquals("result", response.getResult());
    assertEquals("error", response.getErrorMessage());
    assertEquals(now, response.getCreatedAt());
    assertEquals(now, response.getUpdatedAt());
  }

  @Test
  void toString_shouldContainFields() {
    // Given
    TaskStatusResponse response = TaskStatusResponse.builder()
        .taskId("task-123")
        .status(TaskStatus.COMPLETED)
        .result("result")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    // When
    String result = response.toString();

    // Then
    assertNotNull(result);
  }
}

