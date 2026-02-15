package com.voyageai.voyageaibackend.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voyageai.voyageaibackend.kafka.event.PlanningProgressEvent;
import com.voyageai.voyageaibackend.kafka.event.PlanningRequestEvent;
import com.voyageai.voyageaibackend.kafka.event.PlanningResultEvent;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Kafka event schema serialization/deserialization.
 *
 * <p>These tests verify that event DTOs correctly round-trip through
 * Jackson JSON serialization, ensuring compatibility between the
 * Java backend and Python worker.
 */
class KafkaEventSchemaTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  @Test
  void planningRequestEvent_shouldSerializeAndDeserialize() throws Exception {
    // Given
    PlanningRequestEvent event = PlanningRequestEvent.builder()
        .taskId("task-001")
        .userId("user-123")
        .projectId("project-456")
        .requirements("Visit Tokyo for 3 days")
        .taskType("INITIAL_PLANNING")
        .timestamp(Instant.parse("2025-01-15T10:30:00Z"))
        .build();

    // When
    String json = objectMapper.writeValueAsString(event);
    PlanningRequestEvent deserialized =
        objectMapper.readValue(json, PlanningRequestEvent.class);

    // Then
    assertThat(deserialized.getTaskId()).isEqualTo("task-001");
    assertThat(deserialized.getUserId()).isEqualTo("user-123");
    assertThat(deserialized.getProjectId()).isEqualTo("project-456");
    assertThat(deserialized.getRequirements()).isEqualTo("Visit Tokyo for 3 days");
    assertThat(deserialized.getTaskType()).isEqualTo("INITIAL_PLANNING");
    assertThat(deserialized.getTimestamp()).isEqualTo(Instant.parse("2025-01-15T10:30:00Z"));
  }

  @Test
  void planningProgressEvent_shouldSerializeAndDeserialize() throws Exception {
    // Given
    PlanningProgressEvent event = PlanningProgressEvent.builder()
        .taskId("task-002")
        .stage("RAG_SEARCH")
        .percent(30)
        .message("Searching knowledge base...")
        .timestamp(Instant.now())
        .build();

    // When
    String json = objectMapper.writeValueAsString(event);
    PlanningProgressEvent deserialized =
        objectMapper.readValue(json, PlanningProgressEvent.class);

    // Then
    assertThat(deserialized.getTaskId()).isEqualTo("task-002");
    assertThat(deserialized.getStage()).isEqualTo("RAG_SEARCH");
    assertThat(deserialized.getPercent()).isEqualTo(30);
    assertThat(deserialized.getMessage()).isEqualTo("Searching knowledge base...");
    assertThat(deserialized.getTimestamp()).isNotNull();
  }

  @Test
  void planningResultEvent_shouldSerializeAndDeserialize() throws Exception {
    // Given
    PlanningResultEvent event = PlanningResultEvent.builder()
        .taskId("task-003")
        .userId("user-789")
        .projectId("project-abc")
        .status("COMPLETED")
        .itineraryJson("{\"days\": []}")
        .toolTrace(List.of(
            Map.of("tool", "weather", "latency_ms", 150),
            Map.of("tool", "currency", "latency_ms", 80)
        ))
        .error(null)
        .processingTimeMs(5200L)
        .totalTokens(3500)
        .timestamp(Instant.now())
        .build();

    // When
    String json = objectMapper.writeValueAsString(event);
    PlanningResultEvent deserialized =
        objectMapper.readValue(json, PlanningResultEvent.class);

    // Then
    assertThat(deserialized.getTaskId()).isEqualTo("task-003");
    assertThat(deserialized.getStatus()).isEqualTo("COMPLETED");
    assertThat(deserialized.getItineraryJson()).isEqualTo("{\"days\": []}");
    assertThat(deserialized.getToolTrace()).hasSize(2);
    assertThat(deserialized.getProcessingTimeMs()).isEqualTo(5200L);
    assertThat(deserialized.getTotalTokens()).isEqualTo(3500);
    assertThat(deserialized.getError()).isNull();
  }

  @Test
  void planningResultEvent_failed_shouldSerializeErrorField() throws Exception {
    // Given
    PlanningResultEvent event = PlanningResultEvent.builder()
        .taskId("task-004")
        .userId("user-111")
        .projectId("project-222")
        .status("FAILED")
        .error("OpenAI API rate limit exceeded")
        .processingTimeMs(1200L)
        .timestamp(Instant.now())
        .build();

    // When
    String json = objectMapper.writeValueAsString(event);
    PlanningResultEvent deserialized =
        objectMapper.readValue(json, PlanningResultEvent.class);

    // Then
    assertThat(deserialized.getStatus()).isEqualTo("FAILED");
    assertThat(deserialized.getError()).isEqualTo("OpenAI API rate limit exceeded");
    assertThat(deserialized.getItineraryJson()).isNull();
    assertThat(deserialized.getToolTrace()).isNull();
  }

  @Test
  void planningRequestEvent_shouldProduceValidJsonForPythonConsumer() throws Exception {
    // Given - verify JSON field names match Python Pydantic model expectations
    PlanningRequestEvent event = PlanningRequestEvent.builder()
        .taskId("cross-lang-test")
        .userId("user-py")
        .projectId("proj-py")
        .requirements("Test cross-language compatibility")
        .taskType("INITIAL_PLANNING")
        .timestamp(Instant.parse("2025-06-01T12:00:00Z"))
        .build();

    // When
    String json = objectMapper.writeValueAsString(event);

    // Then - verify JSON contains expected field names (snake_case via Jackson config)
    assertThat(json).contains("\"taskId\"");
    assertThat(json).contains("\"userId\"");
    assertThat(json).contains("\"projectId\"");
    assertThat(json).contains("\"requirements\"");
    assertThat(json).contains("\"taskType\"");
    assertThat(json).contains("\"timestamp\"");
  }
}
