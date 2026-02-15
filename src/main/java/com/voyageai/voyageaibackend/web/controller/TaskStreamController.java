package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import com.voyageai.voyageaibackend.kafka.event.PlanningProgressEvent;
import com.voyageai.voyageaibackend.service.RedisTaskService;
import com.voyageai.voyageaibackend.web.dto.TaskStatusUpdate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller for Server-Sent Events (SSE) task status streaming.
 * 
 * <p>This controller provides real-time task status updates to frontend clients
 * using the SSE protocol. SSE is ideal for server-to-client streaming scenarios
 * where bidirectional communication is not needed.
 * 
 * <p>Key features:
 * <ul>
 *   <li><b>Real-time updates</b>: Push task status changes immediately without polling</li>
 *   <li><b>Automatic reconnection</b>: Browser automatically reconnects if connection drops</li>
 *   <li><b>HTTP-based</b>: Uses standard HTTP, no special protocol needed</li>
 *   <li><b>Named events</b>: Different event types for different update categories</li>
 * </ul>
 * 
 * <p>Why SSE over WebSocket for this use case?
 * - One-way communication (server → client) - tasks don't need client messages
 * - Simpler protocol (HTTP-based)
 * - Browser auto-reconnection
 * - Firewall-friendly
 * 
 * <p>Frontend usage:
 * <pre>
 * const eventSource = new EventSource('/api/planning/tasks/${taskId}/stream');
 * 
 * // Listen for progress updates
 * eventSource.addEventListener('progress', (event) => {
 *   const data = JSON.parse(event.data);
 *   console.log(`${data.progressMessage} (${data.progressPercent}%)`);
 * });
 * 
 * // Listen for completion
 * eventSource.addEventListener('completed', (event) => {
 *   const data = JSON.parse(event.data);
 *   displayItinerary(data.structuredItinerary);
 *   eventSource.close(); // Close connection when done
 * });
 * 
 * // Handle errors
 * eventSource.onerror = (error) => {
 *   console.error('SSE error:', error);
 *   eventSource.close();
 * };
 * </pre>
 */
@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Streaming", description = "SSE endpoints for real-time task status updates")
public class TaskStreamController {

  private final RedisTaskService taskService;

  @Value("${sse.timeout.seconds:60}")
  private long sseTimeoutSeconds;

  /**
   * Active SSE emitters keyed by task ID.
   * 
   * <p>ConcurrentHashMap ensures thread-safe access when multiple
   * threads are updating task status and sending SSE events.
   */
  private final ConcurrentHashMap<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

  /**
   * Establishes SSE connection for real-time task status updates.
   * 
   * <p>Connection lifecycle:
   * <ol>
   *   <li>Client connects to this endpoint</li>
   *   <li>Server sends immediate status update (current task state)</li>
   *   <li>Server sends subsequent updates as task progresses</li>
   *   <li>Connection closes when task reaches terminal state (COMPLETED/FAILED/CANCELLED)</li>
   *   <li>Client can reconnect if connection drops</li>
   * </ol>
   * 
   * <p>Timeout: Connection automatically closes after {@code sse.timeout.seconds}
   * if no updates are sent. Client should reconnect if needed.
   *
   * @param taskId Task ID to stream updates for
   * @return SseEmitter for streaming updates
   */
  @GetMapping(value = "/tasks/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @Operation(summary = "Stream task status updates (SSE)",
             description = "Server-Sent Events for real-time task status updates")
  @SecurityRequirement(name = "bearer-jwt")
  public SseEmitter streamTaskStatus(@PathVariable String taskId) {
    log.info("SSE connection requested for task: {}", taskId);

    // Create emitter with timeout
    long timeoutMillis = sseTimeoutSeconds * 1000;
    SseEmitter emitter = new SseEmitter(timeoutMillis);

    // Get current task status
    PlanningTask task = taskService.getTask(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

    try {
      // Send initial status immediately
      sendStatusUpdate(emitter, task, "status");

      // Register emitter for future updates
      activeEmitters.put(taskId, emitter);
      log.info("SSE connection established for task: {} - total active connections: {}", 
          taskId, activeEmitters.size());

      // Configure callbacks
      emitter.onCompletion(() -> {
        log.debug("SSE connection completed for task: {}", taskId);
        activeEmitters.remove(taskId);
      });

      emitter.onTimeout(() -> {
        log.debug("SSE connection timeout for task: {}", taskId);
        activeEmitters.remove(taskId);
        emitter.complete();
      });

      emitter.onError((ex) -> {
        log.error("SSE connection error for task {}: {}", taskId, ex.getMessage());
        activeEmitters.remove(taskId);
      });

      log.info("SSE connection established for task: {}", taskId);

    } catch (IOException e) {
      log.error("Failed to send initial status for task {}: {}", taskId, e.getMessage());
      emitter.completeWithError(e);
      activeEmitters.remove(taskId);
    }

    return emitter;
  }

  /**
   * Notifies all connected clients about a task update.
   * 
   * <p>This method is called by {@link RedisTaskService} whenever a task
   * status or progress changes. It sends an SSE event to all connected clients
   * subscribed to this task.
   * 
   * <p>Event types sent:
   * - "status": Task status changed (PENDING → PROCESSING, etc.)
   * - "progress": Progress update (message + percentage)
   * - "completed": Task completed successfully (includes result)
   * - "failed": Task failed (includes error message)
   * - "cancelled": Task cancelled by user
   *
   * @param taskId Task ID that was updated
   * @param task Updated task object
   */
  public void notifyTaskUpdate(String taskId, PlanningTask task) {
    log.info("notifyTaskUpdate called for task: {} with status: {}", taskId, task.getStatus());
    log.info("Active emitters count: {}, keys: {}", activeEmitters.size(), activeEmitters.keySet());
    
    SseEmitter emitter = activeEmitters.get(taskId);
    
    if (emitter == null) {
      log.warn("No active SSE connection for task: {} - available tasks: {}", 
          taskId, activeEmitters.keySet());
      return;
    }

    try {
      // Determine event name based on task status
      String eventName = getEventName(task.getStatus());

      // Send update
      sendStatusUpdate(emitter, task, eventName);

      // If task reached terminal state, close connection
      if (isTerminalState(task.getStatus())) {
        log.info("Task {} reached terminal state {}, closing SSE connection", 
            taskId, task.getStatus());
        emitter.complete();
        activeEmitters.remove(taskId);
      }

    } catch (IOException e) {
      log.error("Failed to send SSE update for task {}: {}", taskId, e.getMessage());
      emitter.completeWithError(e);
      activeEmitters.remove(taskId);
    }
  }

  /**
   * Notifies connected clients about a rich agent event (Phase 1 SSE streaming).
   *
   * <p>This method is called by {@link com.voyageai.voyageaibackend.kafka.KafkaConsumerService}
   * when a progress event with {@code eventType} is received. It bypasses the Redis round-trip
   * for lower latency, sending the event directly to the SSE emitter.
   *
   * @param taskId Task ID
   * @param event Progress event with rich eventType/eventData
   */
  public void notifyAgentEvent(String taskId, PlanningProgressEvent event) {
    SseEmitter emitter = activeEmitters.get(taskId);
    if (emitter == null) {
      log.debug("No active SSE connection for agent event: taskId={}, eventType={}",
          taskId, event.getEventType());
      return;
    }

    try {
      TaskStatusUpdate update = TaskStatusUpdate.builder()
          .taskId(taskId)
          .status(TaskStatus.PROCESSING)
          .progressMessage(event.getMessage())
          .progressPercent(event.getPercent())
          .eventType(event.getEventType())
          .eventData(event.getEventData())
          .timestamp(Instant.now())
          .build();

      emitter.send(SseEmitter.event()
          .name("agent_event")
          .data(update));

      log.debug("Sent SSE agent_event '{}' for task {}: percent={}%",
          event.getEventType(), taskId, event.getPercent());
    } catch (IOException e) {
      log.error("Failed to send SSE agent event for task {}: {}", taskId, e.getMessage());
      emitter.completeWithError(e);
      activeEmitters.remove(taskId);
    }
  }

  /**
   * Sends a status update through SSE.
   *
   * @param emitter SSE emitter
   * @param task Task to send
   * @param eventName Event name
   * @throws IOException if sending fails
   */
  private void sendStatusUpdate(SseEmitter emitter, PlanningTask task, String eventName) 
      throws IOException {
    TaskStatusUpdate update = TaskStatusUpdate.builder()
        .taskId(task.getTaskId())
        .status(task.getStatus())
        .progressMessage(task.getProgressMessage())
        .progressPercent(task.getProgressPercent())
        .structuredItinerary(task.getStructuredItinerary())
        .result(task.getResult())
        .errorMessage(task.getErrorMessage())
        .timestamp(Instant.now())
        .build();

    emitter.send(SseEmitter.event()
        .name(eventName)
        .data(update));

    log.debug("Sent SSE event '{}' for task {}: status={}, progress={}%", 
        eventName, task.getTaskId(), task.getStatus(), task.getProgressPercent());
  }

  /**
   * Determines SSE event name based on task status.
   *
   * @param status Task status
   * @return Event name for SSE
   */
  private String getEventName(TaskStatus status) {
    return switch (status) {
      case COMPLETED -> "completed";
      case FAILED -> "failed";
      case CANCELLED -> "cancelled";
      case PROCESSING -> "progress";
      default -> "status";
    };
  }

  /**
   * Checks if a task status is terminal (no more updates expected).
   *
   * @param status Task status
   * @return true if terminal state
   */
  private boolean isTerminalState(TaskStatus status) {
    return status == TaskStatus.COMPLETED 
        || status == TaskStatus.FAILED 
        || status == TaskStatus.CANCELLED;
  }

  /**
   * Gets count of active SSE connections (for monitoring).
   *
   * @return Number of active connections
   */
  public int getActiveConnectionCount() {
    return activeEmitters.size();
  }

  /**
   * Closes all active SSE connections (for shutdown/maintenance).
   */
  public void closeAllConnections() {
    log.info("Closing all {} active SSE connections", activeEmitters.size());
    
    activeEmitters.forEach((taskId, emitter) -> {
      try {
        emitter.complete();
      } catch (Exception e) {
        log.warn("Error closing SSE connection for task {}: {}", taskId, e.getMessage());
      }
    });

    activeEmitters.clear();
  }
}

