package com.voyageai.voyageaibackend.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import com.voyageai.voyageaibackend.kafka.event.PlanningProgressEvent;
import com.voyageai.voyageaibackend.kafka.event.PlanningResultEvent;
import com.voyageai.voyageaibackend.service.RedisTaskService;
import com.voyageai.voyageaibackend.service.TravelPlanService;
import com.voyageai.voyageaibackend.web.controller.TaskStreamController;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for KafkaConsumerService using embedded Kafka.
 *
 * <p>Verifies that progress and result events are correctly consumed,
 * Redis task state is updated, and SSE notifications are triggered.
 */
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    topics = {"planning.request", "planning.progress", "planning.result"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:0",
        "port=0"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.group-id=test-consumer",
    "kafka.topic.planning.request=planning.request",
    "kafka.topic.planning.progress=planning.progress",
    "kafka.topic.planning.result=planning.result",
    "planning.mode=kafka"
})
@SuppressWarnings("deprecation")
class KafkaConsumerServiceTest {

  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  @MockBean
  private RedisTaskService taskService;

  @MockBean
  private TravelPlanService travelPlanService;

  @MockBean
  private TaskStreamController taskStreamController;

  private <T> KafkaTemplate<String, T> createProducer(Class<T> valueClass) {
    Map<String, Object> props = Map.of(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class
    );
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
  }

  @Test
  void handleProgressEvent_shouldUpdateRedisAndTriggerSse() {
    // Given
    PlanningTask mockTask = new PlanningTask();
    mockTask.setTaskId("progress-task-1");
    when(taskService.getTask("progress-task-1")).thenReturn(Optional.of(mockTask));

    PlanningProgressEvent event = PlanningProgressEvent.builder()
        .taskId("progress-task-1")
        .stage("RAG_SEARCH")
        .percent(30)
        .message("Searching knowledge base...")
        .timestamp(Instant.now())
        .build();

    // When - publish to progress topic
    KafkaTemplate<String, PlanningProgressEvent> producer = createProducer(PlanningProgressEvent.class);
    producer.send("planning.progress", "progress-task-1", event);

    // Then - verify Redis update and SSE notification
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      verify(taskService, atLeastOnce()).updateProgress(
          eq("progress-task-1"),
          eq("Searching knowledge base..."),
          eq(30)
      );
      verify(taskStreamController, atLeastOnce())
          .notifyTaskUpdate(eq("progress-task-1"), any(PlanningTask.class));
    });
  }

  @Test
  void handleResultEvent_completed_shouldSaveAndNotify() {
    // Given
    PlanningTask mockTask = new PlanningTask();
    mockTask.setTaskId("result-task-1");
    when(taskService.getTask("result-task-1")).thenReturn(Optional.of(mockTask));

    PlanningResultEvent event = PlanningResultEvent.builder()
        .taskId("result-task-1")
        .userId("user-123")
        .projectId("project-456")
        .status("COMPLETED")
        .itineraryJson("{\"days\": []}")
        .processingTimeMs(5000L)
        .totalTokens(3500)
        .timestamp(Instant.now())
        .build();

    // When
    KafkaTemplate<String, PlanningResultEvent> producer = createProducer(PlanningResultEvent.class);
    producer.send("planning.result", "result-task-1", event);

    // Then
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      // Verify itinerary saved
      verify(travelPlanService, atLeastOnce()).createPlan(
          eq("user-123"),
          eq("project-456"),
          anyString(),
          eq("{\"days\": []}")
      );

      // Verify task marked completed with structured itinerary
      verify(taskService, atLeastOnce()).markCompleted(
          eq("result-task-1"),
          any(StructuredItinerary.class),
          eq("{\"days\": []}")
      );

      // Verify SSE notification
      verify(taskStreamController, atLeastOnce())
          .notifyTaskUpdate(eq("result-task-1"), any(PlanningTask.class));
    });
  }

  @Test
  void handleResultEvent_failed_shouldMarkFailedAndNotify() {
    // Given
    PlanningTask mockTask = new PlanningTask();
    mockTask.setTaskId("failed-task-1");
    when(taskService.getTask("failed-task-1")).thenReturn(Optional.of(mockTask));

    PlanningResultEvent event = PlanningResultEvent.builder()
        .taskId("failed-task-1")
        .userId("user-789")
        .projectId("project-abc")
        .status("FAILED")
        .error("OpenAI API rate limit exceeded")
        .processingTimeMs(1200L)
        .timestamp(Instant.now())
        .build();

    // When
    KafkaTemplate<String, PlanningResultEvent> producer = createProducer(PlanningResultEvent.class);
    producer.send("planning.result", "failed-task-1", event);

    // Then
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
      verify(taskService, atLeastOnce()).markFailed(
          eq("failed-task-1"),
          eq("OpenAI API rate limit exceeded")
      );
      verify(taskStreamController, atLeastOnce())
          .notifyTaskUpdate(eq("failed-task-1"), any(PlanningTask.class));
    });
  }
}
