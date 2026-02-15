package com.voyageai.voyageaibackend.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voyageai.voyageaibackend.kafka.event.PlanningRequestEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test for KafkaProducerService using embedded Kafka.
 *
 * <p>Uses Spring's @EmbeddedKafka to spin up an in-process Kafka broker,
 * verifying that PlanningRequestEvent messages are correctly serialized
 * and published to the planning.request topic.
 */
@SpringBootTest
@ActiveProfiles("test")
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
    "spring.kafka.consumer.group-id=test-group",
    "kafka.topic.planning.request=planning.request",
    "kafka.topic.planning.progress=planning.progress",
    "kafka.topic.planning.result=planning.result",
    "planning.mode=kafka"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KafkaProducerServiceTest {

  @Autowired
  private KafkaProducerService kafkaProducerService;

  private KafkaConsumer<String, PlanningRequestEvent> consumer;
  private ObjectMapper objectMapper;

  @Autowired
  private org.springframework.kafka.test.EmbeddedKafkaBroker embeddedKafkaBroker;

  @BeforeAll
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    // Create a test consumer to verify messages
    JsonDeserializer<PlanningRequestEvent> deserializer =
        new JsonDeserializer<>(PlanningRequestEvent.class);
    deserializer.addTrustedPackages("*");
    deserializer.setUseTypeHeaders(false);

    Map<String, Object> consumerProps = Map.of(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString(),
        ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group",
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class
    );

    consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), deserializer);
    consumer.subscribe(java.util.List.of("planning.request"));
  }

  @AfterAll
  void tearDown() {
    if (consumer != null) {
      consumer.close();
    }
  }

  @Test
  void sendPlanningRequest_shouldPublishEventToKafkaTopic() {
    // Given
    String taskId = "test-task-001";
    String userId = "user-123";
    String projectId = "project-456";
    String requirements = "Visit Paris for 5 days";
    String taskType = "INITIAL_PLANNING";

    // When
    kafkaProducerService.sendPlanningRequest(taskId, userId, projectId, requirements, taskType);

    // Then - verify message appears on Kafka topic
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      ConsumerRecords<String, PlanningRequestEvent> records =
          consumer.poll(Duration.ofMillis(500));
      assertThat(records.count()).isGreaterThanOrEqualTo(1);

      ConsumerRecord<String, PlanningRequestEvent> record = records.iterator().next();
      assertThat(record.key()).isEqualTo(taskId);

      PlanningRequestEvent event = record.value();
      assertThat(event.getTaskId()).isEqualTo(taskId);
      assertThat(event.getUserId()).isEqualTo(userId);
      assertThat(event.getProjectId()).isEqualTo(projectId);
      assertThat(event.getRequirements()).isEqualTo(requirements);
      assertThat(event.getTaskType()).isEqualTo(taskType);
      assertThat(event.getTimestamp()).isNotNull();
    });
  }

  @Test
  void sendPlanningRequest_shouldUseTaskIdAsPartitionKey() {
    // Given
    String taskId = "partition-key-test";

    // When
    kafkaProducerService.sendPlanningRequest(
        taskId, "user-1", "proj-1", "Test requirements", "INITIAL_PLANNING");

    // Then
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      ConsumerRecords<String, PlanningRequestEvent> records =
          consumer.poll(Duration.ofMillis(500));
      boolean found = false;
      for (ConsumerRecord<String, PlanningRequestEvent> record : records) {
        if ("partition-key-test".equals(record.key())) {
          found = true;
          assertThat(record.value().getTaskId()).isEqualTo(taskId);
        }
      }
      assertThat(found).isTrue();
    });
  }

  @Test
  void sendPlanningRequest_shouldSetTimestamp() {
    // Given
    Instant before = Instant.now();

    // When
    kafkaProducerService.sendPlanningRequest(
        "ts-test", "user-1", "proj-1", "Test", "INITIAL_PLANNING");

    // Then
    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
      ConsumerRecords<String, PlanningRequestEvent> records =
          consumer.poll(Duration.ofMillis(500));
      for (ConsumerRecord<String, PlanningRequestEvent> record : records) {
        if ("ts-test".equals(record.key())) {
          assertThat(record.value().getTimestamp()).isAfterOrEqualTo(before);
          assertThat(record.value().getTimestamp()).isBeforeOrEqualTo(Instant.now());
        }
      }
    });
  }
}
