package com.voyageai.voyageaibackend.kafka.event;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event carrying the user's reply to clarification questions.
 *
 * <p>Published to topic: {@code planning.clarification.reply}
 *
 * <p>Published by: Java Backend when user answers inline questions.
 * <p>Consumed by: Python Worker to resume planning with enriched requirements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClarificationReplyEvent {

  /** Task identifier (matches the original request). */
  private String taskId;

  /** User ID (for audit and ownership). */
  private String userId;

  /** Project ID (for storage grouping). */
  private String projectId;

  /** User's answers to clarification questions. Each entry has "question" and "answer" keys. */
  private List<Map<String, String>> answers;

  /** The original planning requirements (before clarification). */
  private String originalRequirements;

  /** Conversation context from the original planning request (may be null for first-time requests). */
  private String conversationContext;

  /** ISO-8601 timestamp. */
  private Instant timestamp;
}
