package com.voyageai.voyageaibackend.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an asynchronous travel planning task.
 * 
 * <p>This model is used to track the status and result of AI-generated travel plans.
 * Tasks are stored in Redis for distributed access and automatic expiration (TTL: 24 hours).
 * 
 * <p>Task types:
 * <ul>
 *   <li>INITIAL_PLANNING: First-time itinerary generation for a project</li>
 *   <li>CONVERSATION_UPDATE: Iterative modifications through conversation
 *       (e.g., "Make Day 3 less busy")</li>
 *   <li>TOOL_CALL: External tool invocations
 *       (e.g., "Find hotels near Day 1 activities")</li>
 * </ul>
 * 
 * <p>Status transitions:
 * <pre>
 * PENDING → PROCESSING → COMPLETED
 *                     ↓
 *                   FAILED
 *                     ↓
 *                 CANCELLED (user-initiated)
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningTask {

  /**
   * Unique task identifier (UUID format).
   * Example: "task-123e4567-e89b-12d3-a456-426614174000"
   */
  private String taskId;

  /**
   * User ID who submitted the task.
   */
  private String userId;

  /**
   * Project ID that this task belongs to.
   * Tasks in the same project form a conversation thread.
   */
  private String projectId;

  /**
   * User's travel requirements/prompt.
   * Example: "Plan a 5-day trip to Tokyo for cherry blossom season with $2000 budget"
   */
  private String requirements;

  /**
   * Type of task (determines processing and frontend handling).
   */
  private TaskType taskType;

  /**
   * Current status of the task.
   */
  private TaskStatus status;

  /**
   * Progress message for real-time UI updates.
   * Example: "正在分析您的需求...", "调用大语言模型生成行程...", "规划地图路线..."
   * 
   * <p>Updated during PROCESSING state to provide user feedback on task progress.
   * Used by SSE real-time streaming to update frontend UI.
   */
  private String progressMessage;

  /**
   * Progress percentage (0-100).
   * Example: 0 (started), 40 (calling AI), 80 (saving), 100 (complete)
   * 
   * <p>Used with progressMessage to display progress bar in frontend.
   */
  private Integer progressPercent;

  /**
   * Structured itinerary data with geographic coordinates for map integration.
   * 
   * <p>Only populated when taskType is INITIAL_PLANNING or CONVERSATION_UPDATE.
   * Null for TOOL_CALL tasks (tool results stored in conversation history instead).
   * 
   * <p>Contains complete itinerary with:
   * - Day-by-day activities
   * - Geographic coordinates (latitude/longitude) for each location
   * - Activity IDs for frontend bidirectional linking with map markers
   */
  private StructuredItinerary structuredItinerary;

  /**
   * Raw text result (legacy/backup field).
   * 
   * <p>Contains the AI-generated text response.
   * Primarily used for debugging and as a fallback if structuredItinerary parsing fails.
   */
  private String result;

  /**
   * Error message if task failed (null if successful).
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
   * Timestamp when task was completed or failed (null if still processing).
   */
  private Instant completedAt;

  /**
   * Task type enum (determines processing logic and frontend handling).
   */
  public enum TaskType {
    /**
     * Initial travel planning request.
     * Generates a complete itinerary from scratch based on user requirements.
     * Frontend: Replaces entire itinerary view with new plan.
     */
    INITIAL_PLANNING,

    /**
     * Conversation-driven itinerary update.
     * Modifies existing itinerary based on user feedback.
     * Example: "Make Day 3 less busy", "Add more food experiences"
     * Frontend: Updates specific parts of itinerary view.
     */
    CONVERSATION_UPDATE,

    /**
     * Tool call for external data (hotels, restaurants, flights, weather).
     * Example: "Find hotels near Day 1 activities", "Show me restaurants with Michelin stars"
     * Frontend: Displays results as rich cards in conversation, doesn't modify itinerary.
     */
    TOOL_CALL
  }

  /**
   * Task status enum.
   */
  public enum TaskStatus {
    /**
     * Task is queued and waiting to be processed.
     */
    PENDING,

    /**
     * Task is currently being processed by the AI service.
     */
    PROCESSING,

    /**
     * Task completed successfully with result.
     */
    COMPLETED,

    /**
     * Task failed due to error.
     */
    FAILED,

    /**
     * Task was cancelled by user before completion.
     * Only PENDING or PROCESSING tasks can be cancelled.
     */
    CANCELLED
  }
}

