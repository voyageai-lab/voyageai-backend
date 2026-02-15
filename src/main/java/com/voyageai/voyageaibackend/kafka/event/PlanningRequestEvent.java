package com.voyageai.voyageaibackend.kafka.event;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event sent when a user submits a travel planning request.
 *
 * <p>Published to topic: {@code planning.request}
 *
 * <p>Partition key: {@code taskId} ensures all events for the same task
 * land on the same partition, preserving ordering.
 *
 * <p>Consumed by: Python AI Worker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningRequestEvent {

  /** Unique task identifier (UUID). Used as Kafka partition key. */
  private String taskId;

  /** Authenticated user who submitted the request. */
  private String userId;

  /** Project ID for conversation context grouping. */
  private String projectId;

  /** User's travel requirements (natural language). */
  private String requirements;

  /** Task type: INITIAL_PLANNING, CONVERSATION_UPDATE, TOOL_CALL. */
  private String taskType;

  /** Previous conversation context for follow-up requests (null for initial). */
  private String conversationContext;

  /** ISO-8601 timestamp when the event was created. */
  private Instant timestamp;
}
