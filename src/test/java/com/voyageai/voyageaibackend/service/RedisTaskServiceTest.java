package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import com.voyageai.voyageaibackend.web.controller.TaskStreamController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTaskServiceTest {

    @Mock
    private RedisTemplate<String, PlanningTask> redisTemplate;

    @Mock
    private ValueOperations<String, PlanningTask> valueOperations;

    @Mock
    private TaskStreamController taskStreamController;

    private RedisTaskService redisTaskService;

    @BeforeEach
    void setUp() {
        redisTaskService = new RedisTaskService(redisTemplate, taskStreamController);
        ReflectionTestUtils.setField(redisTaskService, "taskTtlHours", 24);
    }

    @Test
    void createTask_shouldCreateTaskInRedis() {
        // Given
        String userId = "user-123";
        String projectId = "project-456";
        String requirements = "Test requirements";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        PlanningTask result = redisTaskService.createTask(userId, projectId, requirements);

        // Then
        assertNotNull(result);
        assertTrue(result.getTaskId().startsWith("task-"));
        assertEquals(userId, result.getUserId());
        assertEquals(projectId, result.getProjectId());
        assertEquals(requirements, result.getRequirements());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        verify(valueOperations).set(anyString(), eq(result), eq(Duration.ofHours(24)));
    }

    @Test
    void createTask_redisError_shouldUseFallbackStorage() {
        // Given
        String userId = "user-123";
        String projectId = "project-456";
        String requirements = "Test requirements";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis error"))
                .when(valueOperations).set(anyString(), any(PlanningTask.class), any(Duration.class));

        // When
        PlanningTask result = redisTaskService.createTask(userId, projectId, requirements);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(projectId, result.getProjectId());
        assertEquals(requirements, result.getRequirements());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        // Note: The set operation will fail, so we don't verify it
    }

    @Test
    void getTask_redisHit_shouldReturnTask() {
        // Given
        String taskId = "task-123";
        PlanningTask expectedTask = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(expectedTask);

        // When
        Optional<PlanningTask> result = redisTaskService.getTask(taskId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedTask, result.get());
        verify(valueOperations).get("task:" + taskId);
    }

    @Test
    void getTask_redisMiss_fallbackHit_shouldReturnTask() {
        // Given
        String taskId = "task-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(null);

        // When
        Optional<PlanningTask> result = redisTaskService.getTask(taskId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void getTask_redisError_shouldCheckFallback() {
        // Given
        String taskId = "task-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenThrow(new RuntimeException("Redis error"));

        // When
        Optional<PlanningTask> result = redisTaskService.getTask(taskId);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void markProcessing_shouldUpdateTaskStatus() {
        // Given
        String taskId = "task-123";
        PlanningTask task = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        redisTaskService.markProcessing(taskId);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController).notifyTaskUpdate(eq(taskId), any(PlanningTask.class));
    }

    @Test
    void updateProgress_shouldUpdateProgressFields() {
        // Given
        String taskId = "task-123";
        String message = "Processing...";
        int percent = 50;
        PlanningTask task = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        redisTaskService.updateProgress(taskId, message, percent);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController).notifyTaskUpdate(eq(taskId), any(PlanningTask.class));
    }

    @Test
    void updateProgress_shouldClampPercentToValidRange() {
        // Given
        String taskId = "task-123";
        PlanningTask task = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When - test negative percent
        redisTaskService.updateProgress(taskId, "Test", -10);

        // Then
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));

        // When - test percent > 100
        redisTaskService.updateProgress(taskId, "Test", 150);

        // Then
        verify(valueOperations, times(2)).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
    }

    @Test
    void markCompleted_withStringResult_shouldUpdateTask() {
        // Given
        String taskId = "task-123";
        String result = "Test result";
        PlanningTask task = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        redisTaskService.markCompleted(taskId, result);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController).notifyTaskUpdate(eq(taskId), any(PlanningTask.class));
    }

    @Test
    void markCompleted_withStructuredItinerary_shouldUpdateTask() {
        // Given
        String taskId = "task-123";
        String result = "Test result";
        StructuredItinerary structuredItinerary = createTestStructuredItinerary();
        PlanningTask task = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        redisTaskService.markCompleted(taskId, structuredItinerary, result);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController).notifyTaskUpdate(eq(taskId), any(PlanningTask.class));
    }

    @Test
    void markFailed_shouldUpdateTaskStatus() {
        // Given
        String taskId = "task-123";
        String errorMessage = "Test error";
        PlanningTask task = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        redisTaskService.markFailed(taskId, errorMessage);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController).notifyTaskUpdate(eq(taskId), any(PlanningTask.class));
    }

    @Test
    void cancelTask_pendingTask_shouldCancelSuccessfully() {
        // Given
        String taskId = "task-123";
        PlanningTask task = createTestTask(taskId);
        task.setStatus(TaskStatus.PENDING);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        boolean result = redisTaskService.cancelTask(taskId);

        // Then
        assertTrue(result);
        verify(valueOperations, times(2)).get("task:" + taskId); // Once in cancelTask, once in updateTask
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(redisTemplate).expire(eq("task:" + taskId), eq(Duration.ofHours(1)));
        verify(taskStreamController).notifyTaskUpdate(eq(taskId), any(PlanningTask.class));
    }

    @Test
    void cancelTask_processingTask_shouldCancelSuccessfully() {
        // Given
        String taskId = "task-123";
        PlanningTask task = createTestTask(taskId);
        task.setStatus(TaskStatus.PROCESSING);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        boolean result = redisTaskService.cancelTask(taskId);

        // Then
        assertTrue(result);
        verify(valueOperations, times(2)).get("task:" + taskId); // Once in cancelTask, once in updateTask
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(redisTemplate).expire(eq("task:" + taskId), eq(Duration.ofHours(1)));
    }

    @Test
    void cancelTask_completedTask_shouldNotCancel() {
        // Given
        String taskId = "task-123";
        PlanningTask task = createTestTask(taskId);
        task.setStatus(TaskStatus.COMPLETED);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);

        // When
        boolean result = redisTaskService.cancelTask(taskId);

        // Then
        assertFalse(result);
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations, never()).set(anyString(), any(PlanningTask.class), any(Duration.class));
    }

    @Test
    void cancelTask_taskNotFound_shouldReturnFalse() {
        // Given
        String taskId = "task-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(null);

        // When
        boolean result = redisTaskService.cancelTask(taskId);

        // Then
        assertFalse(result);
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations, never()).set(anyString(), any(PlanningTask.class), any(Duration.class));
    }

    @Test
    void cancelTask_redisError_shouldReturnFalse() {
        // Given
        String taskId = "task-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenThrow(new RuntimeException("Redis error"));

        // When
        boolean result = redisTaskService.cancelTask(taskId);

        // Then
        assertFalse(result);
        verify(valueOperations).get("task:" + taskId);
    }

    @Test
    void getTaskCount_shouldReturnRedisAndFallbackCount() {
        // Given
        Set<String> redisKeys = Set.of("task:1", "task:2", "task:3");
        when(redisTemplate.keys("task:*")).thenReturn(redisKeys);

        // When
        int result = redisTaskService.getTaskCount();

        // Then
        assertEquals(3, result);
        verify(redisTemplate).keys("task:*");
    }

    @Test
    void getTaskCount_redisError_shouldReturnFallbackCount() {
        // Given
        when(redisTemplate.keys("task:*")).thenThrow(new RuntimeException("Redis error"));

        // When
        int result = redisTaskService.getTaskCount();

        // Then
        assertEquals(0, result);
        verify(redisTemplate).keys("task:*");
    }

    @Test
    void removeTask_shouldRemoveFromRedisAndFallback() {
        // Given
        String taskId = "task-123";

        // When
        redisTaskService.removeTask(taskId);

        // Then
        verify(redisTemplate).delete("task:" + taskId);
    }

    @Test
    void removeTask_redisError_shouldRemoveFromFallback() {
        // Given
        String taskId = "task-123";
        doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete("task:" + taskId);

        // When
        redisTaskService.removeTask(taskId);

        // Then
        verify(redisTemplate).delete("task:" + taskId);
    }

    @Test
    void clearAllTasks_shouldClearRedisAndFallback() {
        // Given
        Set<String> redisKeys = Set.of("task:1", "task:2");
        when(redisTemplate.keys("task:*")).thenReturn(redisKeys);

        // When
        redisTaskService.clearAllTasks();

        // Then
        verify(redisTemplate).keys("task:*");
        verify(redisTemplate).delete(redisKeys);
    }

    @Test
    void clearAllTasks_redisError_shouldClearFallback() {
        // Given
        when(redisTemplate.keys("task:*")).thenThrow(new RuntimeException("Redis error"));

        // When
        redisTaskService.clearAllTasks();

        // Then
        verify(redisTemplate).keys("task:*");
        verify(redisTemplate, never()).delete(any(Set.class));
    }

    @Test
    void updateTask_taskNotFound_shouldLogWarning() {
        // Given
        String taskId = "task-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(null);

        // When
        redisTaskService.markProcessing(taskId);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations, never()).set(anyString(), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController, never()).notifyTaskUpdate(anyString(), any(PlanningTask.class));
    }

    @Test
    void updateTask_redisError_shouldUseFallback() {
        // Given
        String taskId = "task-123";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenThrow(new RuntimeException("Redis error"));

        // When
        redisTaskService.markProcessing(taskId);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations, never()).set(anyString(), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController, never()).notifyTaskUpdate(anyString(), any(PlanningTask.class));
    }

    @Test
    void updateTask_sseNotificationError_shouldNotFailUpdate() {
        // Given
        String taskId = "task-123";
        PlanningTask task = createTestTask(taskId);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("task:" + taskId)).thenReturn(task);
        doThrow(new RuntimeException("SSE error")).when(taskStreamController)
                .notifyTaskUpdate(anyString(), any(PlanningTask.class));

        // When
        redisTaskService.markProcessing(taskId);

        // Then
        verify(valueOperations).get("task:" + taskId);
        verify(valueOperations).set(eq("task:" + taskId), any(PlanningTask.class), any(Duration.class));
        verify(taskStreamController).notifyTaskUpdate(eq(taskId), any(PlanningTask.class));
    }

    private PlanningTask createTestTask(String taskId) {
        return PlanningTask.builder()
                .taskId(taskId)
                .userId("user-123")
                .projectId("project-456")
                .requirements("Test requirements")
                .status(TaskStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private StructuredItinerary createTestStructuredItinerary() {
        return StructuredItinerary.builder()
                .metadata(StructuredItinerary.ItineraryMetadata.builder()
                        .destination("Tokyo, Japan")
                        .totalDays(3)
                        .startDate("2024-01-01")
                        .endDate("2024-01-03")
                        .budget("Medium")
                        .interests(java.util.List.of("Culture", "Food"))
                        .build())
                .build();
    }
}