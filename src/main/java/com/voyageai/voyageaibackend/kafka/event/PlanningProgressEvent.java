package com.voyageai.voyageaibackend.kafka.event;

import java.time.Instant;
import java.util.Map;
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
 *
 * <p>Rich event types (Phase 1 SSE streaming):
 * <pre>
 *   thinking             Agent reasoning text
 *   tool_start           Tool call initiated (tool name, arguments)
 *   tool_result          Tool call completed (result summary, latency)
 *   stage_change         Pipeline stage transition
 *   plan_outline         Plan summary before full generation
 *   clarification_needed Agent asks clarification questions (Phase 2)
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

  /**
   * Granular event subtype for rich SSE streaming.
   *
   * <p>Values: thinking, tool_start, tool_result, stage_change,
   * plan_outline, clarification_needed. Null for legacy progress events.
   */
  private String eventType;

  /**
   * Structured payload for the event (JSON-serializable map).
   *
   * <p>Contents depend on eventType:
   * <ul>
   *   <li>thinking: {@code {"text": "..."}} </li>
   *   <li>tool_start: {@code {"tool": "...", "arguments": {...}}} </li>
   *   <li>tool_result: {@code {"tool": "...", "success": true, "latency_ms": 123, "summary": "..."}} </li>
   *   <li>plan_outline: {@code {"summary": "...", "daily_themes": [...]}} </li>
   *   <li>clarification_needed: {@code {"questions": [...]}} </li>
   * </ul>
   */
  private Map<String, Object> eventData;

  /** ISO-8601 timestamp. */
  private Instant timestamp;
}
