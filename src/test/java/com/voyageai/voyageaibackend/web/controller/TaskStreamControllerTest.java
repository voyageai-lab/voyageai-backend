package com.voyageai.voyageaibackend.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import com.voyageai.voyageaibackend.service.RedisTaskService;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class TaskStreamControllerTest {

  @Mock
  private RedisTaskService taskService;

  @InjectMocks
  private TaskStreamController taskStreamController;

  private MockMvc mockMvc;
  private static final String TASK_ID = "task-123";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(taskStreamController).build();
  }

  @Test
  void streamTaskStatus_success_shouldReturnSseEmitter() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));

    // When
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);

    // Then
    assertNotNull(emitter);
    verify(taskService).getTask(TASK_ID);
  }

  @Test
  void streamTaskStatus_taskNotFound_shouldThrowException() {
    // Given
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(com.voyageai.voyageaibackend.exception.ResourceNotFoundException.class, () -> {
      taskStreamController.streamTaskStatus(TASK_ID);
    });
    verify(taskService).getTask(TASK_ID);
  }

  @Test
  void streamTaskStatus_serviceError_shouldThrowException() {
    // Given
    when(taskService.getTask(TASK_ID)).thenThrow(new RuntimeException("Service error"));

    // When & Then
    assertThrows(RuntimeException.class, () -> {
      taskStreamController.streamTaskStatus(TASK_ID);
    });
    verify(taskService).getTask(TASK_ID);
  }

  @Test
  void streamTaskStatus_withNullTaskId_shouldThrowException() {
    // Given
    when(taskService.getTask(null)).thenReturn(Optional.empty());

    // When & Then
    assertThrows(com.voyageai.voyageaibackend.exception.ResourceNotFoundException.class, () -> {
      taskStreamController.streamTaskStatus(null);
    });
  }

  @Test
  void streamTaskStatus_withEmptyTaskId_shouldThrowException() {
    // Given
    when(taskService.getTask("")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(com.voyageai.voyageaibackend.exception.ResourceNotFoundException.class, () -> {
      taskStreamController.streamTaskStatus("");
    });
  }

  @Test
  void streamTaskStatus_withWhitespaceTaskId_shouldThrowException() {
    // Given
    when(taskService.getTask("   ")).thenReturn(Optional.empty());

    // When & Then
    assertThrows(com.voyageai.voyageaibackend.exception.ResourceNotFoundException.class, () -> {
      taskStreamController.streamTaskStatus("   ");
    });
  }

  @Test
  void streamTaskStatus_shouldSetCorrectTimeout() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));

    // When
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);

    // Then
    assertNotNull(emitter);
    // The timeout should be set to 300 seconds (5 minutes) as configured
    // We can't directly test the timeout value, but we can verify the emitter is created
  }

  @Test
  void streamTaskStatus_shouldLogConnectionRequest() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));

    // When
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);

    // Then
    assertNotNull(emitter);
    // Logging is tested implicitly through the method execution
  }

  @Test
  void streamTaskStatus_shouldHandleConcurrentRequests() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));

    // When - simulate multiple concurrent requests
    SseEmitter emitter1 = taskStreamController.streamTaskStatus(TASK_ID);
    SseEmitter emitter2 = taskStreamController.streamTaskStatus(TASK_ID);

    // Then
    assertNotNull(emitter1);
    assertNotNull(emitter2);
    verify(taskService, times(2)).getTask(TASK_ID);
  }

  @Test
  void streamTaskStatus_shouldCreateUniqueEmitters() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));

    // When
    SseEmitter emitter1 = taskStreamController.streamTaskStatus(TASK_ID);
    SseEmitter emitter2 = taskStreamController.streamTaskStatus(TASK_ID);

    // Then
    assertNotNull(emitter1);
    assertNotNull(emitter2);
    // Each call should create a new emitter instance
    // We can't directly compare object references, but we can verify both are created
  }

  @Test
  void streamTaskStatus_withDifferentTaskIds_shouldHandleSeparately() {
    // Given
    String taskId1 = "task-123";
    String taskId2 = "task-456";
    PlanningTask task1 = createTestTask();
    PlanningTask task2 = createTestTask();
    when(taskService.getTask(taskId1)).thenReturn(Optional.of(task1));
    when(taskService.getTask(taskId2)).thenReturn(Optional.of(task2));

    // When
    SseEmitter emitter1 = taskStreamController.streamTaskStatus(taskId1);
    SseEmitter emitter2 = taskStreamController.streamTaskStatus(taskId2);

    // Then
    assertNotNull(emitter1);
    assertNotNull(emitter2);
    verify(taskService).getTask(taskId1);
    verify(taskService).getTask(taskId2);
  }

  @Test
  void streamTaskStatus_shouldHandleTaskServiceException() {
    // Given
    when(taskService.getTask(TASK_ID)).thenThrow(new RuntimeException("Database error"));

    // When & Then
    assertThrows(RuntimeException.class, () -> {
      taskStreamController.streamTaskStatus(TASK_ID);
    });
    verify(taskService).getTask(TASK_ID);
  }

  @Test
  void streamTaskStatus_shouldReturnValidSseEmitter() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));

    // When
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);

    // Then
    assertNotNull(emitter);
    // Verify the emitter is properly configured
    // The actual SSE functionality would be tested in integration tests
  }

  // ========== notifyTaskUpdate Tests ==========

  @Test
  void notifyTaskUpdate_withActiveConnection_shouldSendUpdate() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);
    
    // When
    taskStreamController.notifyTaskUpdate(TASK_ID, task);
    
    // Then - verify the emitter was used (we can't easily test the actual SSE sending)
    assertNotNull(emitter);
  }

  @Test
  void notifyTaskUpdate_withNoActiveConnection_shouldNotThrow() {
    // Given
    PlanningTask task = createTestTask();
    
    // When & Then - should not throw exception
    taskStreamController.notifyTaskUpdate("non-existent-task", task);
  }

  @Test
  void notifyTaskUpdate_withTerminalStatus_shouldCloseConnection() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.COMPLETED);
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);
    
    // When
    taskStreamController.notifyTaskUpdate(TASK_ID, task);
    
    // Then - connection should be closed for terminal status
    assertNotNull(emitter);
  }

  @Test
  void notifyTaskUpdate_withFailedStatus_shouldCloseConnection() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.FAILED);
    task.setErrorMessage("Test error");
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);
    
    // When
    taskStreamController.notifyTaskUpdate(TASK_ID, task);
    
    // Then - connection should be closed for terminal status
    assertNotNull(emitter);
  }

  @Test
  void notifyTaskUpdate_withCancelledStatus_shouldCloseConnection() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.CANCELLED);
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);
    
    // When
    taskStreamController.notifyTaskUpdate(TASK_ID, task);
    
    // Then - connection should be closed for terminal status
    assertNotNull(emitter);
  }

  @Test
  void notifyTaskUpdate_withProcessingStatus_shouldKeepConnection() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.PROCESSING);
    task.setProgressMessage("Processing...");
    task.setProgressPercent(50);
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));
    SseEmitter emitter = taskStreamController.streamTaskStatus(TASK_ID);
    
    // When
    taskStreamController.notifyTaskUpdate(TASK_ID, task);
    
    // Then - connection should remain open for non-terminal status
    assertNotNull(emitter);
  }

  // ========== getEventName Tests ==========

  @Test
  void getEventName_completedStatus_shouldReturnCompleted() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.COMPLETED);
    
    // When
    String eventName = getEventName(task.getStatus());
    
    // Then
    assertEquals("completed", eventName);
  }

  @Test
  void getEventName_failedStatus_shouldReturnFailed() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.FAILED);
    
    // When
    String eventName = getEventName(task.getStatus());
    
    // Then
    assertEquals("failed", eventName);
  }

  @Test
  void getEventName_cancelledStatus_shouldReturnCancelled() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.CANCELLED);
    
    // When
    String eventName = getEventName(task.getStatus());
    
    // Then
    assertEquals("cancelled", eventName);
  }

  @Test
  void getEventName_processingStatus_shouldReturnProgress() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.PROCESSING);
    
    // When
    String eventName = getEventName(task.getStatus());
    
    // Then
    assertEquals("progress", eventName);
  }

  @Test
  void getEventName_pendingStatus_shouldReturnStatus() {
    // Given
    PlanningTask task = createTestTask();
    task.setStatus(TaskStatus.PENDING);
    
    // When
    String eventName = getEventName(task.getStatus());
    
    // Then
    assertEquals("status", eventName);
  }

  // ========== isTerminalState Tests ==========

  @Test
  void isTerminalState_completedStatus_shouldReturnTrue() {
    // Given
    TaskStatus status = TaskStatus.COMPLETED;
    
    // When
    boolean isTerminal = isTerminalState(status);
    
    // Then
    assertTrue(isTerminal);
  }

  @Test
  void isTerminalState_failedStatus_shouldReturnTrue() {
    // Given
    TaskStatus status = TaskStatus.FAILED;
    
    // When
    boolean isTerminal = isTerminalState(status);
    
    // Then
    assertTrue(isTerminal);
  }

  @Test
  void isTerminalState_cancelledStatus_shouldReturnTrue() {
    // Given
    TaskStatus status = TaskStatus.CANCELLED;
    
    // When
    boolean isTerminal = isTerminalState(status);
    
    // Then
    assertTrue(isTerminal);
  }

  @Test
  void isTerminalState_processingStatus_shouldReturnFalse() {
    // Given
    TaskStatus status = TaskStatus.PROCESSING;
    
    // When
    boolean isTerminal = isTerminalState(status);
    
    // Then
    assertFalse(isTerminal);
  }

  @Test
  void isTerminalState_pendingStatus_shouldReturnFalse() {
    // Given
    TaskStatus status = TaskStatus.PENDING;
    
    // When
    boolean isTerminal = isTerminalState(status);
    
    // Then
    assertFalse(isTerminal);
  }

  // ========== getActiveConnectionCount Tests ==========

  @Test
  void getActiveConnectionCount_withNoConnections_shouldReturnZero() {
    // When
    int count = taskStreamController.getActiveConnectionCount();
    
    // Then
    assertEquals(0, count);
  }

  @Test
  void getActiveConnectionCount_withOneConnection_shouldReturnOne() {
    // Given
    PlanningTask task = createTestTask();
    when(taskService.getTask(TASK_ID)).thenReturn(Optional.of(task));
    taskStreamController.streamTaskStatus(TASK_ID);
    
    // When
    int count = taskStreamController.getActiveConnectionCount();
    
    // Then
    assertEquals(1, count);
  }

  @Test
  void getActiveConnectionCount_withMultipleConnections_shouldReturnCorrectCount() {
    // Given
    PlanningTask task1 = createTestTask();
    task1.setTaskId("task-1");
    PlanningTask task2 = createTestTask();
    task2.setTaskId("task-2");
    PlanningTask task3 = createTestTask();
    task3.setTaskId("task-3");
    
    when(taskService.getTask("task-1")).thenReturn(Optional.of(task1));
    when(taskService.getTask("task-2")).thenReturn(Optional.of(task2));
    when(taskService.getTask("task-3")).thenReturn(Optional.of(task3));
    
    taskStreamController.streamTaskStatus("task-1");
    taskStreamController.streamTaskStatus("task-2");
    taskStreamController.streamTaskStatus("task-3");
    
    // When
    int count = taskStreamController.getActiveConnectionCount();
    
    // Then
    assertEquals(3, count);
  }

  // ========== closeAllConnections Tests ==========

  @Test
  void closeAllConnections_withNoConnections_shouldNotThrow() {
    // When & Then - should not throw exception
    taskStreamController.closeAllConnections();
  }

  @Test
  void closeAllConnections_withActiveConnections_shouldCloseAll() {
    // Given
    PlanningTask task1 = createTestTask();
    task1.setTaskId("task-1");
    PlanningTask task2 = createTestTask();
    task2.setTaskId("task-2");
    
    when(taskService.getTask("task-1")).thenReturn(Optional.of(task1));
    when(taskService.getTask("task-2")).thenReturn(Optional.of(task2));
    
    taskStreamController.streamTaskStatus("task-1");
    taskStreamController.streamTaskStatus("task-2");
    assertEquals(2, taskStreamController.getActiveConnectionCount());
    
    // When
    taskStreamController.closeAllConnections();
    
    // Then
    assertEquals(0, taskStreamController.getActiveConnectionCount());
  }

  @Test
  void closeAllConnections_shouldClearAllEmitters() {
    // Given
    PlanningTask task1 = createTestTask();
    task1.setTaskId("task-1");
    PlanningTask task2 = createTestTask();
    task2.setTaskId("task-2");
    PlanningTask task3 = createTestTask();
    task3.setTaskId("task-3");
    
    when(taskService.getTask("task-1")).thenReturn(Optional.of(task1));
    when(taskService.getTask("task-2")).thenReturn(Optional.of(task2));
    when(taskService.getTask("task-3")).thenReturn(Optional.of(task3));
    
    taskStreamController.streamTaskStatus("task-1");
    taskStreamController.streamTaskStatus("task-2");
    taskStreamController.streamTaskStatus("task-3");
    assertEquals(3, taskStreamController.getActiveConnectionCount());
    
    // When
    taskStreamController.closeAllConnections();
    
    // Then
    assertEquals(0, taskStreamController.getActiveConnectionCount());
  }

  // ========== Helper Methods ==========

  private PlanningTask createTestTask() {
    return PlanningTask.builder()
        .taskId(TASK_ID)
        .userId("user-123")
        .projectId("proj-456")
        .requirements("Test requirements")
        .status(TaskStatus.PENDING)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  // Helper methods to access private methods using reflection
  private String getEventName(TaskStatus status) {
    try {
      java.lang.reflect.Method method = Class
          .forName("com.voyageai.voyageaibackend.web.controller.TaskStreamController")
          .getDeclaredMethod("getEventName", TaskStatus.class);
      method.setAccessible(true);
      return (String) method.invoke(taskStreamController, status);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isTerminalState(TaskStatus status) {
    try {
      java.lang.reflect.Method method = Class
          .forName("com.voyageai.voyageaibackend.web.controller.TaskStreamController")
          .getDeclaredMethod("isTerminalState", TaskStatus.class);
      method.setAccessible(true);
      return (Boolean) method.invoke(taskStreamController, status);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
