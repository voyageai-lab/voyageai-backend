package com.voyageai.voyageaibackend.web.dto;

import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskType;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for task status query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {

  /**
   * Task ID.
   */
  private String taskId;

  /**
   * Task type (INITIAL_PLANNING, CONVERSATION_UPDATE, TOOL_CALL).
   */
  private TaskType taskType;

  /**
   * Current status of the task.
   */
  private TaskStatus status;

  /**
   * Progress message for UI display (PROCESSING state only).
   */
  private String progressMessage;

  /**
   * Progress percentage 0-100 (PROCESSING state only).
   */
  private Integer progressPercent;

  /**
   * User's original travel requirements/prompt.
   * This helps users track what they requested.
   */
  private String requirements;

  /**
   * Structured itinerary with geographic coordinates (null if not completed).
   */
  private StructuredItinerary structuredItinerary;

  /**
   * Generated travel plan text (null if not completed).
   * Legacy/fallback field.
   */
  private String result;

  /**
   * Error message (null if no error).
   */
  private String errorMessage;

  /**
   * Timestamp when task was created.
   */
  private Instant createdAt;

  /**
   * Timestamp when task was last updated.
   */
  private Instant updatedAt;

  /**
   * Timestamp when task completed (null if still processing).
   */
  private Instant completedAt;
}

