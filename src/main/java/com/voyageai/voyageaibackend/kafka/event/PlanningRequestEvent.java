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
 * <p>Partition key: {@code taskId}
 * <p>Consumed by: Python AI Worker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanningRequestEvent {

  private String taskId;
  private String userId;
  private String projectId;
  private String requirements;
  private String taskType;
  private String conversationContext;

  /** When true, worker fetches existing itinerary from DB and runs surgical edit pipeline. */
  private boolean editMode;

  /** User's own OpenAI API key (decrypted). Worker uses this instead of platform default. */
  private String userOpenaiApiKey;

  /** User's own Gemini API key (decrypted). Takes priority over OpenAI when set. */
  private String userGeminiApiKey;

  private Instant timestamp;
}
