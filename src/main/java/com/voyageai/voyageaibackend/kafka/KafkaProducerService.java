package com.voyageai.voyageaibackend.kafka;

import com.voyageai.voyageaibackend.kafka.event.ClarificationReplyEvent;
import com.voyageai.voyageaibackend.kafka.event.PlanningRequestEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

/**
 * Kafka producer for publishing planning request events.
 *
 * <p>This service is the entry point for the async planning pipeline.
 * When a user submits a planning request, it is published to the
 * {@code planning.request} topic for consumption by the Python AI Worker.
 *
 * <p>The taskId is used as the Kafka message key to ensure all events
 * for the same task are routed to the same partition (ordering guarantee).
 *
 * <p>Producer guarantees:
 * <ul>
 *   <li>Idempotent producer enabled (no duplicate sends on retry)</li>
 *   <li>{@code acks=all} for durability</li>
 *   <li>Async send with callback for error handling</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

  private final KafkaTemplate<String, PlanningRequestEvent> requestKafkaTemplate;

  @Value("${kafka.topic.planning.request:planning.request}")
  private String requestTopic;

  @Value("${kafka.topic.planning.clarification.reply:planning.clarification.reply}")
  private String clarificationReplyTopic;

  /**
   * Publishes a planning request event to Kafka.
   *
   * @param taskId              unique task ID (used as partition key)
   * @param userId              authenticated user ID
   * @param projectId           project this task belongs to
   * @param requirements        user's travel requirements
   * @param taskType            type of planning task
   * @param conversationContext previous conversation history for follow-ups (nullable)
   * @return CompletableFuture that completes when Kafka acknowledges the send
   */
  public CompletableFuture<SendResult<String, PlanningRequestEvent>> sendPlanningRequest(
      String taskId,
      String userId,
      String projectId,
      String requirements,
      String taskType,
      String conversationContext
  ) {
    PlanningRequestEvent event = PlanningRequestEvent.builder()
        .taskId(taskId)
        .userId(userId)
        .projectId(projectId)
        .requirements(requirements)
        .taskType(taskType)
        .conversationContext(conversationContext)
        .timestamp(Instant.now())
        .build();

    log.info("Publishing planning request to Kafka: taskId={}, topic={}", taskId, requestTopic);

    return requestKafkaTemplate.send(requestTopic, taskId, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish planning request: taskId={}, error={}",
                taskId, ex.getMessage(), ex);
          } else {
            log.info("Published planning request: taskId={}, partition={}, offset={}",
                taskId,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          }
        });
  }

  /**
   * Publishes a clarification reply event to Kafka (Phase 2).
   *
   * <p>This is sent when a user answers the agent's clarification questions.
   * The Python worker consumes this to resume planning with enriched requirements.
   *
   * @param taskId              task ID for the original planning request
   * @param userId              authenticated user ID
   * @param projectId           project this task belongs to
   * @param answers             user's answers to clarification questions
   * @param originalRequirements original requirements text
   */
  @SuppressWarnings("unchecked")
  public void sendClarificationReply(
      String taskId,
      String userId,
      String projectId,
      List<Map<String, String>> answers,
      String originalRequirements,
      String conversationContext
  ) {
    ClarificationReplyEvent event = ClarificationReplyEvent.builder()
        .taskId(taskId)
        .userId(userId)
        .projectId(projectId)
        .answers(answers)
        .originalRequirements(originalRequirements)
        .conversationContext(conversationContext)
        .timestamp(Instant.now())
        .build();

    log.info("Publishing clarification reply to Kafka: taskId={}, topic={}", 
        taskId, clarificationReplyTopic);

    // Reuse the existing KafkaTemplate with raw type cast.
    // Both event types use JSON serialization so this works transparently.
    ((KafkaTemplate<String, Object>) (KafkaTemplate<?, ?>) requestKafkaTemplate)
        .send(clarificationReplyTopic, taskId, event)
        .whenComplete((result, ex) -> {
          if (ex != null) {
            log.error("Failed to publish clarification reply: taskId={}, error={}",
                taskId, ex.getMessage(), ex);
          } else {
            log.info("Published clarification reply: taskId={}", taskId);
          }
        });
  }
}
