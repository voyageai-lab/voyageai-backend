package com.voyageai.voyageaibackend.kafka;

import com.voyageai.voyageaibackend.kafka.event.PlanningRequestEvent;
import java.time.Instant;
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

  /**
   * Publishes a planning request event to Kafka.
   *
   * @param taskId       unique task ID (used as partition key)
   * @param userId       authenticated user ID
   * @param projectId    project this task belongs to
   * @param requirements user's travel requirements
   * @param taskType     type of planning task
   * @return CompletableFuture that completes when Kafka acknowledges the send
   */
  public CompletableFuture<SendResult<String, PlanningRequestEvent>> sendPlanningRequest(
      String taskId,
      String userId,
      String projectId,
      String requirements,
      String taskType
  ) {
    PlanningRequestEvent event = PlanningRequestEvent.builder()
        .taskId(taskId)
        .userId(userId)
        .projectId(projectId)
        .requirements(requirements)
        .taskType(taskType)
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
}
