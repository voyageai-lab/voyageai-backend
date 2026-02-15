package com.voyageai.voyageaibackend.kafka.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event for real-time task progress updates.
 *
 * <p>Published to topic: {@code planning.progress}
 *
 * <p>Published by: Python AI Worker during processing.
 * <p>Consumed by: Java Backend to update Redis and trigger SSE.
 *
 * <p>Progress stages and typical percent values:
 * <pre>
 *   RECEIVED     (5%)   Worker received the request
 *   RAG_SEARCH   (20%)  Hybrid search in progress
 *   TOOL_SELECT  (30%)  Tool-RAG selecting tools
 *   TOOL_CALLING (50%)  Executing tool calls
 *   GENERATING   (70%)  LLM generating itinerary
 *   SAVING       (90%)  Persisting results
 *   COMPLETED    (100%) Done (result event follows)
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningProgressEvent {

  /** Task identifier (matches the original request). */
  private String taskId;

  /** Processing stage name (e.g., RAG_SEARCH, TOOL_CALLING). */
  private String stage;

  /** Progress percentage (0-100). */
  private Integer percent;

  /** Human-readable progress message for the UI. */
  private String message;

  /** ISO-8601 timestamp. */
  private Instant timestamp;
}
