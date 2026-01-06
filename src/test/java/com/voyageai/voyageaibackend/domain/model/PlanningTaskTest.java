package com.voyageai.voyageaibackend.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PlanningTask}.
 */
class PlanningTaskTest {

  @Test
  void builder_shouldCreatePlanningTask() {
    // Given
    String taskId = "task-123";
    String userId = "user-456";
    TaskStatus status = TaskStatus.PENDING;
    String requirements = "Plan a 5-day trip to Tokyo";
    Instant now = Instant.now();

    // When
    PlanningTask task = PlanningTask.builder()
        .taskId(taskId)
        .userId(userId)
        .status(status)
        .requirements(requirements)
        .createdAt(now)
        .updatedAt(now)
        .build();

    // Then
    assertNotNull(task);
    assertEquals(taskId, task.getTaskId());
    assertEquals(userId, task.getUserId());
    assertEquals(status, task.getStatus());
    assertEquals(requirements, task.getRequirements());
    assertEquals(now, task.getCreatedAt());
    assertEquals(now, task.getUpdatedAt());
    assertNull(task.getResult());
    assertNull(task.getErrorMessage());
    assertNull(task.getCompletedAt());
  }

  @Test
  void setters_shouldUpdateFields() {
    // Given
    PlanningTask task = PlanningTask.builder()
        .taskId("task-123")
        .userId("user-456")
        .status(TaskStatus.PENDING)
        .requirements("Requirements")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    // When
    task.setStatus(TaskStatus.COMPLETED);
    task.setResult("{\"plan\": \"data\"}");
    Instant newTime = Instant.now();
    task.setUpdatedAt(newTime);
    task.setCompletedAt(newTime);

    // Then
    assertEquals(TaskStatus.COMPLETED, task.getStatus());
    assertEquals("{\"plan\": \"data\"}", task.getResult());
    assertEquals(newTime, task.getUpdatedAt());
    assertEquals(newTime, task.getCompletedAt());
  }

  @Test
  void setErrorMessage_shouldSetError() {
    // Given
    PlanningTask task = PlanningTask.builder()
        .taskId("task-123")
        .userId("user-456")
        .status(TaskStatus.PENDING)
        .requirements("Requirements")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    // When
    task.setStatus(TaskStatus.FAILED);
    task.setErrorMessage("API call failed");
    Instant completionTime = Instant.now();
    task.setCompletedAt(completionTime);

    // Then
    assertEquals(TaskStatus.FAILED, task.getStatus());
    assertEquals("API call failed", task.getErrorMessage());
    assertEquals(completionTime, task.getCompletedAt());
  }

  @Test
  void taskStatus_allValuesAccessible() {
    // Test all enum values
    assertEquals(TaskStatus.PENDING, TaskStatus.valueOf("PENDING"));
    assertEquals(TaskStatus.PROCESSING, TaskStatus.valueOf("PROCESSING"));
    assertEquals(TaskStatus.COMPLETED, TaskStatus.valueOf("COMPLETED"));
    assertEquals(TaskStatus.FAILED, TaskStatus.valueOf("FAILED"));
    assertEquals(TaskStatus.CANCELLED, TaskStatus.valueOf("CANCELLED"));
    
    // Test values() method
    TaskStatus[] statuses = TaskStatus.values();
    assertEquals(5, statuses.length); // Updated to 5: PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
  }

  @Test
  void noArgsConstructor_shouldCreateEmptyTask() {
    // When
    PlanningTask task = new PlanningTask();

    // Then
    assertNotNull(task);
    assertNull(task.getTaskId());
    assertNull(task.getUserId());
  }

  @Test
  void allArgsConstructor_shouldCreateFullTask() {
    // Given
    String taskId = "task-123";
    String userId = "user-456";
    String projectId = "proj-789";
    String requirements = "Requirements";
    TaskStatus status = TaskStatus.PENDING;
    String result = null;
    String errorMessage = null;
    Instant createdAt = Instant.now();
    Instant updatedAt = Instant.now();
    Instant completedAt = null;

    // When - Use builder pattern as constructor signature changed
    PlanningTask task = PlanningTask.builder()
        .taskId(taskId)
        .userId(userId)
        .projectId(projectId)
        .requirements(requirements)
        .status(status)
        .result(result)
        .errorMessage(errorMessage)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .completedAt(completedAt)
        .build();

    // Then
    assertEquals(taskId, task.getTaskId());
    assertEquals(userId, task.getUserId());
    assertEquals(projectId, task.getProjectId());
    assertEquals(requirements, task.getRequirements());
    assertEquals(status, task.getStatus());
    assertEquals(result, task.getResult());
    assertEquals(errorMessage, task.getErrorMessage());
    assertEquals(createdAt, task.getCreatedAt());
    assertEquals(updatedAt, task.getUpdatedAt());
    assertEquals(completedAt, task.getCompletedAt());
  }
}
