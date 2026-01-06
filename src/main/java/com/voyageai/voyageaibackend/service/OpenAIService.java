package com.voyageai.voyageaibackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Service for interacting with OpenAI Chat Completion API.
 * Uses WebClient for reactive HTTP communication.
 * 
 * <p>This service handles:
 * <ul>
 *   <li>Building OpenAI API requests with proper format</li>
 *   <li>Managing API authentication with Bearer token</li>
 *   <li>Parsing structured responses from the API</li>
 *   <li>Error handling for API failures</li>
 * </ul>
 */
@Service
@Slf4j
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class OpenAIService {

  private final WebClient webClient;
  private final String model;
  private final double temperature;
  private final int maxTokens;
  private final ObjectMapper objectMapper;

  /**
   * Constructor with dependency injection.
   *
   * @param webClientBuilder WebClient builder for HTTP communication
   * @param apiKey OpenAI API key from configuration
   * @param apiUrl OpenAI API endpoint URL
   * @param model OpenAI model to use (e.g., gpt-4)
   * @param temperature Temperature parameter for response randomness
   * @param maxTokens Maximum tokens in response
   */
  public OpenAIService(
      WebClient.Builder webClientBuilder,
      @Value("${openai.api.key}") String apiKey,
      @Value("${openai.api.url}") String apiUrl,
      @Value("${openai.model}") String model,
      @Value("${openai.temperature}") double temperature,
      @Value("${openai.max-tokens}") int maxTokens
  ) {
    this.webClient = webClientBuilder
        .baseUrl(apiUrl)
        .defaultHeader("Authorization", "Bearer " + apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
    this.model = model;
    this.temperature = temperature;
    this.maxTokens = maxTokens;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Generates a travel itinerary based on user requirements.
   * 
   * <p>This method constructs a prompt with system instructions and user requirements,
   * sends it to OpenAI API, and returns the generated itinerary text.
   *
   * @param userRequirements User's travel requirements (destination, duration, preferences)
   * @return Mono emitting the generated itinerary text
   */
  public Mono<String> generateItinerary(String userRequirements) {
    log.info("Generating itinerary for requirements: {}", userRequirements);

    // Build request body
    Map<String, Object> requestBody = buildRequestBody(userRequirements);

    // Make API call
    return webClient.post()
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .map(this::extractContent)
        .doOnSuccess(response -> log.info("Successfully generated itinerary"))
        .doOnError(error -> log.error("Error calling OpenAI API", error))
        .onErrorMap(WebClientResponseException.class, this::handleApiError);
  }

  /**
   * Builds the request body for OpenAI API call.
   *
   * @param userRequirements User's travel requirements
   * @return Request body as Map
   */
  private Map<String, Object> buildRequestBody(String userRequirements) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("model", model);
    requestBody.put("temperature", temperature);
    requestBody.put("max_tokens", maxTokens);

    // Build messages array
    List<Map<String, String>> messages = List.of(
        Map.of(
            "role", "system",
            "content", buildSystemPrompt()
        ),
        Map.of(
            "role", "user",
            "content", userRequirements
        )
    );
    requestBody.put("messages", messages);

    return requestBody;
  }

  /**
   * Builds the system prompt for the AI travel planner.
   * This prompt defines the AI's role and output format.
   *
   * @return System prompt string
   */
  private String buildSystemPrompt() {
    return """
        You are an expert travel planner AI assistant. Your task is to create detailed,
        personalized travel itineraries based on user requirements.
        
        When creating an itinerary, include:
        - Day-by-day schedule with specific activities and times
        - Recommended accommodations with brief descriptions
        - Dining suggestions (breakfast, lunch, dinner)
        - Transportation options between locations
        - Estimated budget breakdown
        - Practical tips and local insights
        
        Format your response as a structured itinerary with clear sections for each day.
        Be specific, practical, and considerate of travel logistics.
        """;
  }

  /**
   * Extracts the content from OpenAI API response.
   *
   * @param responseBody Raw response body from API
   * @return Extracted content text
   */
  private String extractContent(String responseBody) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      return root.path("choices")
          .get(0)
          .path("message")
          .path("content")
          .asText();
    } catch (Exception e) {
      log.error("Error parsing OpenAI response", e);
      throw new RuntimeException("Failed to parse OpenAI response", e);
    }
  }

  /**
   * Generates a structured itinerary with geographic coordinates for map integration.
   * 
   * <p>This method is specifically designed for Chapter 5+ structured itinerary generation.
   * It uses JSON mode to ensure the AI returns properly formatted data with:
   * - Day-by-day activities
   * - Geographic coordinates (latitude/longitude) for each location
   * - Activity IDs for frontend bidirectional linking
   * 
   * <p>The conversation context allows iterative refinement:
   * - First call: Generate initial itinerary
   * - Subsequent calls: Refine based on user feedback
   *
   * @param requirements User's travel requirements
   * @param conversationContext Previous conversation history for context
   * @return Mono emitting structured itinerary
   */
  public Mono<StructuredItinerary> generateStructuredItinerary(
      String requirements, 
      String conversationContext) {
    log.info("Generating structured itinerary with conversation context");

    // Build request body with structured output
    Map<String, Object> requestBody = buildStructuredRequestBody(requirements, conversationContext);

    // Make API call
    return webClient.post()
        .bodyValue(requestBody)
        .retrieve()
        .bodyToMono(String.class)
        .map(this::extractContent)
        .map(this::parseStructuredItinerary)
        .doOnSuccess(itinerary -> log.info(
            "Successfully generated structured itinerary with {} days", 
            itinerary.getDays() != null ? itinerary.getDays().size() : 0))
        .doOnError(error -> log.error("Error generating structured itinerary", error))
        .onErrorMap(WebClientResponseException.class, this::handleApiError);
  }

  /**
   * Builds request body for structured itinerary generation.
   *
   * @param requirements User requirements
   * @param conversationContext Previous conversation context
   * @return Request body as Map
   */
  private Map<String, Object> buildStructuredRequestBody(
      String requirements, 
      String conversationContext) {
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("model", model);
    requestBody.put("temperature", temperature);
    requestBody.put("max_tokens", maxTokens);

    // Use JSON mode for structured output (only supported by certain models)
    if (supportsJsonMode(model)) {
      requestBody.put("response_format", Map.of("type", "json_object"));
    }

    // Build messages array with context
    List<Map<String, String>> messages = List.of(
        Map.of(
            "role", "system",
            "content", buildStructuredSystemPrompt()
        ),
        Map.of(
            "role", "user",
            "content", buildStructuredUserPrompt(requirements, conversationContext)
        )
    );
    requestBody.put("messages", messages);

    return requestBody;
  }

  /**
   * Checks if the specified model supports JSON mode.
   * 
   *
   * @param modelName The model name to check
   * @return true if the model supports response_format: "json_object"
   */
  private boolean supportsJsonMode(String modelName) {
    return modelName != null && (
        modelName.startsWith("gpt-4-turbo")
        || modelName.startsWith("gpt-4o")
        || modelName.startsWith("gpt-3.5-turbo-1106")
        || modelName.equals("gpt-4-1106-preview")
        );
  }

  /**
   * Builds system prompt for structured itinerary generation.
   *
   * @return System prompt with JSON schema requirements
   */
  private String buildStructuredSystemPrompt() {
    return """
        You are an expert travel planner AI. Generate detailed itineraries in JSON format.
        
        CRITICAL REQUIREMENTS:
        1. ALWAYS include precise geographic coordinates (latitude, longitude) for EVERY location
        2. Use your knowledge or estimate coordinates as accurately as possible
        3. Generate activity IDs in format "act-day{dayNumber}-{sequenceNumber}"
        4. Ensure all JSON fields are properly formatted
        
        Response must be valid JSON matching this exact structure:
        {
          "metadata": {
            "destination": "string",
            "startDate": "YYYY-MM-DD",
            "endDate": "YYYY-MM-DD",
            "totalDays": number,
            "budget": "string",
            "interests": ["string"]
          },
          "days": [
            {
              "dayNumber": 1,
              "date": "YYYY-MM-DD",
              "theme": "string",
              "activities": [
                {
                  "activityId": "act-day1-001",
                  "time": "HH:MM-HH:MM",
                  "title": "string",
                  "description": "string",
                  "type": "SIGHTSEEING|DINING|ACCOMMODATION|TRANSPORTATION|SHOPPING|ENTERTAINMENT",
                  "location": {
                    "name": "string",
                    "address": "string",
                    "latitude": number,
                    "longitude": number,
                    "placeType": "string"
                  },
                  "tips": "string",
                  "estimatedCost": "string",
                  "durationMinutes": number
                }
              ],
              "summary": "string"
            }
          ]
        }
        
        COORDINATE GUIDELINES:
        - Use 6 decimal places for precision (e.g., 35.681236, 139.767125)
        - Ensure coordinates are within valid ranges (lat: -90 to 90, lng: -180 to 180)
        - For well-known places, use accurate real-world coordinates
        - Coordinates are REQUIRED - estimate if exact location unknown
        """;
  }

  /**
   * Builds user prompt with context.
   *
   * @param requirements User requirements
   * @param conversationContext Previous conversation
   * @return Formatted user prompt
   */
  private String buildStructuredUserPrompt(String requirements, String conversationContext) {
    StringBuilder prompt = new StringBuilder();

    if (conversationContext != null && !conversationContext.isEmpty()) {
      prompt.append("PREVIOUS CONVERSATION:\n");
      prompt.append(conversationContext);
      prompt.append("\n\n");
    }

    prompt.append("CURRENT REQUEST:\n");
    prompt.append(requirements);
    prompt.append("\n\nPlease generate a detailed travel itinerary in the specified JSON format.");
    prompt.append(" Remember to include precise geographic coordinates for ALL locations.");

    return prompt.toString();
  }

  /**
   * Parses JSON response into StructuredItinerary object.
   *
   * @param jsonContent JSON content from AI
   * @return Parsed StructuredItinerary
   */
  private StructuredItinerary parseStructuredItinerary(String jsonContent) {
    try {
      return objectMapper.readValue(jsonContent, StructuredItinerary.class);
    } catch (Exception e) {
      log.error("Error parsing structured itinerary JSON: {}", jsonContent, e);
      throw new RuntimeException("Failed to parse structured itinerary from AI response", e);
    }
  }

  /**
   * Handles API errors and converts them to meaningful exceptions.
   *
   * @param error WebClient exception
   * @return RuntimeException with error details
   */
  private RuntimeException handleApiError(WebClientResponseException error) {
    String errorMessage = String.format(
        "OpenAI API error: %d %s - %s",
        error.getStatusCode().value(),
        error.getStatusText(),
        error.getResponseBodyAsString()
    );
    log.error(errorMessage);
    return new RuntimeException(errorMessage, error);
  }
}


