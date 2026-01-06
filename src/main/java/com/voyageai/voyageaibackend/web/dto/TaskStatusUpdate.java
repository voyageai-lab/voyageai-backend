package com.voyageai.voyageaibackend.web.dto;

import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for SSE task status updates.
 * 
 * <p>This DTO is sent through Server-Sent Events (SSE) to provide
 * real-time task progress updates to the frontend.
 * 
 * <p>SSE Event types:
 * <ul>
 *   <li><b>status</b>: Status change (PENDING → PROCESSING → COMPLETED/FAILED/CANCELLED)</li>
 *   <li><b>progress</b>: Progress update with message and percentage</li>
 *   <li><b>completed</b>: Task completed successfully (includes result)</li>
 *   <li><b>failed</b>: Task failed (includes error message)</li>
 *   <li><b>cancelled</b>: Task cancelled by user</li>
 * </ul>
 * 
 * <p>Example usage in frontend:
 * <pre>
 * const eventSource = new EventSource('/api/planning/tasks/${taskId}/stream');
 * 
 * eventSource.addEventListener('progress', (event) => {
 *   const data = JSON.parse(event.data);
 *   updateProgressBar(data.progressPercent);
 *   updateStatusText(data.progressMessage);
 * });
 * 
 * eventSource.addEventListener('completed', (event) => {
 *   const data = JSON.parse(event.data);
 *   displayItinerary(data.structuredItinerary);
 *   eventSource.close();
 * });
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusUpdate {

  /**
   * Task ID.
   */
  private String taskId;

  /**
   * Current task status.
   */
  private TaskStatus status;

  /**
   * Progress message for UI display.
   * Example: "正在分析您的需求...", "调用大语言模型生成行程..."
   * 
   * <p>Updated during PROCESSING state, displayed in frontend loading overlay.
   */
  private String progressMessage;

  /**
   * Progress percentage (0-100).
   * 
   * <p>Used to render progress bar in frontend.
   */
  private Integer progressPercent;

  /**
   * Structured itinerary result (only present when task completes successfully).
   * 
   * <p>Contains complete day-by-day itinerary with geographic coordinates
   * for map integration.
   */
  private StructuredItinerary structuredItinerary;

  /**
   * Raw text result (legacy/fallback field).
   */
  private String result;

  /**
   * Error message (only present when task fails).
   */
  private String errorMessage;

  /**
   * Timestamp of this update.
   */
  private Instant timestamp;
}

