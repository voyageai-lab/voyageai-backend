package com.voyageai.voyageaibackend.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event carrying the final result of a planning task.
 *
 * <p>Published to topic: {@code planning.result}
 *
 * <p>Published by: Python AI Worker after processing completes (or fails).
 * <p>Consumed by: Java Backend to save to MongoDB, update Redis, trigger SSE.
 *
 * <p>On success: {@code status=COMPLETED}, {@code itineraryJson} is populated.
 * <p>On failure: {@code status=FAILED}, {@code error} describes the issue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningResultEvent {

  /** Task identifier (matches the original request). */
  private String taskId;

  /** User ID (for audit and ownership). */
  private String userId;

  /** Project ID (for storage grouping). */
  private String projectId;

  /** Final status: COMPLETED or FAILED. */
  private String status;

  /** Serialized JSON of StructuredItinerary (null if failed). */
  private String itineraryJson;

  /** List of tool call traces for observability (may be empty). */
  private List<Map<String, Object>> toolTrace;

  /** Error message if status is FAILED (null if succeeded). */
  private String error;

  /** Total processing time in milliseconds. */
  private Long processingTimeMs;

  /** Total LLM tokens consumed. */
  private Integer totalTokens;

  /** ISO-8601 timestamp. */
  private Instant timestamp;
}
