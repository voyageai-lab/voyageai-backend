package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Integration tests for {@link OpenAIService} using MockWebServer.
 * These tests cover the actual HTTP client behavior and response parsing.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
class OpenAIServiceIntegrationTest {

  private MockWebServer mockWebServer;
  private OpenAIService openAIService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    String baseUrl = mockWebServer.url("/v1/chat/completions").toString();
    
    openAIService = new OpenAIService(
        WebClient.builder(),
        "test-api-key",
        baseUrl,
        "gpt-4",
        0.7,
        2000
    );
  }

  @AfterEach
  void tearDown() throws IOException {
    mockWebServer.shutdown();
  }

  @Test
  void generateItinerary_successfulResponse_shouldReturnContent() throws Exception {
    // Given
    String userRequirements = "Plan a 5-day trip to Tokyo";
    String expectedContent = "{\"title\":\"Tokyo Adventure\",\"days\":5}";
    
    String responseJson = buildOpenAIResponse(expectedContent);
    mockWebServer.enqueue(new MockResponse()
        .setBody(responseJson)
        .addHeader("Content-Type", "application/json"));

    // When
    String result = openAIService.generateItinerary(userRequirements).block();

    // Then
    assertNotNull(result);
    assertEquals(expectedContent, result);
    
    // Verify request was made correctly
    RecordedRequest request = mockWebServer.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("/v1/chat/completions", request.getPath());
    assertTrue(request.getHeader("Authorization").contains("Bearer test-api-key"));
    
    // Verify request body contains correct fields
    String requestBody = request.getBody().readUtf8();
    assertTrue(requestBody.contains("gpt-4"));
    assertTrue(requestBody.contains("0.7"));
    assertTrue(requestBody.contains("2000"));
    assertTrue(requestBody.contains(userRequirements));
  }

  @Test
  void generateItinerary_apiError_shouldHandleError() {
    // Given
    String userRequirements = "Plan a trip";
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(500)
        .setBody("{\"error\":{\"message\":\"Internal server error\"}}"));

    // When / Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      openAIService.generateItinerary(userRequirements).block();
    });
    
    assertTrue(exception.getMessage().contains("OpenAI API error"));
    assertTrue(exception.getMessage().contains("500"));
  }

  @Test
  void generateItinerary_invalidJson_shouldHandleParseError() {
    // Given
    String userRequirements = "Plan a trip";
    mockWebServer.enqueue(new MockResponse()
        .setBody("invalid json response")
        .addHeader("Content-Type", "application/json"));

    // When / Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      openAIService.generateItinerary(userRequirements).block();
    });
    
    assertTrue(exception.getMessage().contains("Failed to parse OpenAI response"));
  }

  @Test
  void generateItinerary_missingContent_shouldHandleError() {
    // Given
    String userRequirements = "Plan a trip";
    String malformedResponse = "{\"choices\":[{\"message\":{}}]}";
    mockWebServer.enqueue(new MockResponse()
        .setBody(malformedResponse)
        .addHeader("Content-Type", "application/json"));

    // When
    String result = openAIService.generateItinerary(userRequirements).block();

    // Then
    assertNotNull(result);
    assertEquals("", result); // asText() returns empty string for missing nodes
  }

  @Test
  void generateItinerary_unauthorizedError_shouldHandleError() {
    // Given
    String userRequirements = "Plan a trip";
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(401)
        .setBody("{\"error\":{\"message\":\"Invalid API key\"}}"));

    // When / Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      openAIService.generateItinerary(userRequirements).block();
    });
    
    assertTrue(exception.getMessage().contains("OpenAI API error"));
    assertTrue(exception.getMessage().contains("401"));
  }

  @Test
  void generateItinerary_rateLimitError_shouldHandleError() {
    // Given
    String userRequirements = "Plan a trip";
    mockWebServer.enqueue(new MockResponse()
        .setResponseCode(429)
        .setBody("{\"error\":{\"message\":\"Rate limit exceeded\"}}"));

    // When / Then
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      openAIService.generateItinerary(userRequirements).block();
    });
    
    assertTrue(exception.getMessage().contains("OpenAI API error"));
    assertTrue(exception.getMessage().contains("429"));
  }

  @Test
  void generateItinerary_complexItinerary_shouldParseCorrectly() throws Exception {
    // Given
    String userRequirements = "Plan a detailed 7-day trip to Paris";
    String complexItinerary = "{"
        + "\"title\":\"Paris Week\","
        + "\"destination\":\"Paris, France\","
        + "\"duration_days\":7,"
        + "\"daily_plan\":["
        + "{\"day\":1,\"activities\":[\"Eiffel Tower\",\"Seine River Cruise\"]}"
        + "]"
        + "}";
    
    String responseJson = buildOpenAIResponse(complexItinerary);
    mockWebServer.enqueue(new MockResponse()
        .setBody(responseJson)
        .addHeader("Content-Type", "application/json"));

    // When
    String result = openAIService.generateItinerary(userRequirements).block();

    // Then
    assertNotNull(result);
    assertTrue(result.contains("Paris Week"));
    assertTrue(result.contains("Eiffel Tower"));
    assertTrue(result.contains("Seine River Cruise"));
  }

  /**
   * Helper method to build a valid OpenAI API response.
   *
   * @param content The content to include in the response
   * @return JSON string representing OpenAI response
   */
  private String buildOpenAIResponse(String content) throws JsonProcessingException {
    Map<String, Object> response = Map.of(
        "id", "chatcmpl-123",
        "object", "chat.completion",
        "created", 1677652288,
        "model", "gpt-4",
        "choices", java.util.List.of(
            Map.of(
                "index", 0,
                "message", Map.of(
                    "role", "assistant",
                    "content", content
                ),
                "finish_reason", "stop"
            )
        )
    );
    return objectMapper.writeValueAsString(response);
  }
}

