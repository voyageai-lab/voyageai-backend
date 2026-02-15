package com.voyageai.voyageaibackend.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import com.voyageai.voyageaibackend.kafka.event.PlanningProgressEvent;
import com.voyageai.voyageaibackend.kafka.event.PlanningResultEvent;
import com.voyageai.voyageaibackend.service.RedisTaskService;
import com.voyageai.voyageaibackend.service.TravelPlanService;
import com.voyageai.voyageaibackend.web.controller.TaskStreamController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for planning progress and result events from Python worker.
 *
 * <p>Listens to two topics:
 * <ul>
 *   <li>{@code planning.progress} - Incremental progress updates</li>
 *   <li>{@code planning.result} - Final success/failure with itinerary</li>
 * </ul>
 *
 * <p>On receiving events:
 * <ol>
 *   <li>Updates Redis task state</li>
 *   <li>Pushes SSE events to connected frontends</li>
 *   <li>On result: saves itinerary to MongoDB via TravelPlanService</li>
 * </ol>
 *
 * <p>Uses Spring's {@code @KafkaListener} for declarative consumer binding.
 * The containerFactory references match the factories defined in KafkaConfig.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

  private final RedisTaskService taskService;
  private final TravelPlanService travelPlanService;
  private final TaskStreamController taskStreamController;
  private final ObjectMapper objectMapper;

  /**
   * Handles progress events from the Python worker.
   *
   * <p>Updates the task progress in Redis and triggers SSE push to frontend.
   * This provides real-time progress feedback during AI itinerary generation.
   *
   * @param event Progress event with stage, percent, and message
   */
  @KafkaListener(
      topics = "${kafka.topic.planning.progress:planning.progress}",
      groupId = "${spring.kafka.consumer.group-id:voyageai-java}",
      containerFactory = "progressListenerFactory"
  )
  public void handleProgressEvent(PlanningProgressEvent event) {
    String taskId = event.getTaskId();
    // Set MDC trace context for structured logging correlation
    MDC.put("taskId", taskId);
    try {
      log.info(
          "Received progress event: taskId={}, stage={}, percent={}",
          taskId,
          event.getStage(),
          event.getPercent()
      );

      // Update task progress in Redis
      taskService.updateProgress(taskId, event.getMessage(), event.getPercent());

      // Get updated task and push SSE event
      taskService.getTask(taskId).ifPresent(task ->
          taskStreamController.notifyTaskUpdate(taskId, task)
      );
    } catch (Exception e) {
      log.error("Failed to handle progress event: taskId={}, error={}", taskId, e.getMessage(), e);
    } finally {
      MDC.clear();
    }
  }

  /**
   * Handles result events from the Python worker.
   *
   * <p>On COMPLETED:
   * <ol>
   *   <li>Saves itinerary to MongoDB via TravelPlanService</li>
   *   <li>Marks task as completed in Redis with itinerary JSON</li>
   *   <li>Pushes SSE completed event to frontend</li>
   * </ol>
   *
   * <p>On FAILED:
   * <ol>
   *   <li>Marks task as failed in Redis with error message</li>
   *   <li>Pushes SSE failed event to frontend</li>
   * </ol>
   *
   * @param event Result event with status, itinerary, and metadata
   */
  @KafkaListener(
      topics = "${kafka.topic.planning.result:planning.result}",
      groupId = "${spring.kafka.consumer.group-id:voyageai-java}",
      containerFactory = "resultListenerFactory"
  )
  public void handleResultEvent(PlanningResultEvent event) {
    String taskId = event.getTaskId();
    // Set MDC trace context for structured logging correlation
    MDC.put("taskId", taskId);
    MDC.put("userId", event.getUserId());
    try {
      log.info(
          "Received result event: taskId={}, status={}, processingTime={}ms",
          taskId,
          event.getStatus(),
          event.getProcessingTimeMs()
      );

      if ("COMPLETED".equals(event.getStatus())) {
        handleCompletedResult(event);
      } else {
        handleFailedResult(event);
      }
    } catch (Exception e) {
      log.error("Failed to handle result event: taskId={}, error={}", taskId, e.getMessage(), e);
      // Try to mark task as failed if result handling itself fails
      try {
        taskService.markFailed(taskId, "Internal error processing result: " + e.getMessage());
      } catch (Exception ex) {
        log.error("Failed to mark task as failed: taskId={}", taskId, ex);
      }
    } finally {
      MDC.clear();
    }
  }

  private void handleCompletedResult(PlanningResultEvent event) {
    String taskId = event.getTaskId();

    // Save itinerary to MongoDB via TravelPlanService
    if (event.getItineraryJson() != null) {
      try {
        travelPlanService.createPlan(
            event.getUserId(),
            event.getProjectId(),
            "Kafka pipeline result",  // Requirements stored in request event
            event.getItineraryJson()
        );
        log.info("Saved itinerary to MongoDB: taskId={}", taskId);
      } catch (Exception e) {
        log.error("Failed to save itinerary: taskId={}, error={}", taskId, e.getMessage(), e);
      }
    }

    // Deserialize itineraryJson (snake_case from Python) into StructuredItinerary
    StructuredItinerary structuredItinerary = null;
    if (event.getItineraryJson() != null) {
      try {
        structuredItinerary = objectMapper.readValue(
            event.getItineraryJson(), StructuredItinerary.class);
        log.info("Deserialized structured itinerary: taskId={}, destination={}",
            taskId,
            structuredItinerary.getMetadata() != null
                ? structuredItinerary.getMetadata().getDestination() : "unknown");
      } catch (Exception e) {
        log.warn("Failed to deserialize itinerary JSON, falling back to raw string: taskId={}, error={}",
            taskId, e.getMessage());
      }
    }

    // Mark task as completed in Redis with structured itinerary + raw JSON
    if (structuredItinerary != null) {
      taskService.markCompleted(taskId, structuredItinerary, event.getItineraryJson());
    } else {
      taskService.markCompleted(taskId, event.getItineraryJson());
    }

    // Push SSE event
    taskService.getTask(taskId).ifPresent(task ->
        taskStreamController.notifyTaskUpdate(taskId, task)
    );

    log.info(
        "Task completed via Kafka: taskId={}, tokens={}, time={}ms",
        taskId,
        event.getTotalTokens(),
        event.getProcessingTimeMs()
    );
  }

  private void handleFailedResult(PlanningResultEvent event) {
    String taskId = event.getTaskId();
    String error = event.getError() != null ? event.getError() : "Unknown error";

    // Mark task as failed
    taskService.markFailed(taskId, error);

    // Push SSE event
    taskService.getTask(taskId).ifPresent(task ->
        taskStreamController.notifyTaskUpdate(taskId, task)
    );

    log.warn("Task failed via Kafka: taskId={}, error={}", taskId, error);
  }
}
