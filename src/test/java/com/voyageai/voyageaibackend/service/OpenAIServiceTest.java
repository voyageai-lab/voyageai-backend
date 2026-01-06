package com.voyageai.voyageaibackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private OpenAIService openAIService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Mock WebClient.Builder
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        
        openAIService = new OpenAIService(
                webClientBuilder,
                "test-api-key",
                "https://api.openai.com/v1",
                "gpt-4",
                0.7,
                2000
        );
    }

  @Test
    void generateItinerary_success_shouldReturnItinerary() {
    // Given
        String userRequirements = "Visit Tokyo for 3 days";
        String mockResponse = createMockOpenAIResponse("Generated itinerary for Tokyo");
        
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockResponse));

    // When
        Mono<String> result = openAIService.generateItinerary(userRequirements);

    // Then
        StepVerifier.create(result)
                .expectNext("Generated itinerary for Tokyo")
                .verifyComplete();

        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any(Map.class));
        verify(requestBodySpec).retrieve();
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void generateItinerary_apiError_shouldPropagateError() {
        // Given
        String userRequirements = "Visit Tokyo for 3 days";
        WebClientResponseException apiError = WebClientResponseException.create(
                400, "Bad Request", null, "Invalid request".getBytes(), null);
        
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(apiError));

        // When
        Mono<String> result = openAIService.generateItinerary(userRequirements);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any(Map.class));
    }

    @Test
    void generateStructuredItinerary_success_shouldReturnStructuredItinerary() {
        // Given
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation context";
        String mockItineraryJson = createMockStructuredItineraryJson();
        String mockJsonResponse = createMockOpenAIResponse(mockItineraryJson);
        
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        // When
        Mono<StructuredItinerary> result = openAIService.generateStructuredItinerary(requirements, conversationContext);

        // Then
        StepVerifier.create(result)
                .assertNext(itinerary -> {
                    assertNotNull(itinerary);
                    assertNotNull(itinerary.getMetadata());
                    assertEquals("Tokyo, Japan", itinerary.getMetadata().getDestination());
                    assertEquals(3, itinerary.getMetadata().getTotalDays());
                })
                .verifyComplete();

        verify(webClient).post();
        verify(requestBodyUriSpec).bodyValue(any(Map.class));
    }

    @Test
    void generateStructuredItinerary_invalidJson_shouldThrowException() {
        // Given
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation context";
        String invalidJson = "Invalid JSON response";
        
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(invalidJson));

        // When
        Mono<StructuredItinerary> result = openAIService.generateStructuredItinerary(requirements, conversationContext);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(webClient).post();
    }

    @Test
    void generateStructuredItinerary_apiError_shouldPropagateError() {
        // Given
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation context";
        WebClientResponseException apiError = WebClientResponseException.create(
                500, "Internal Server Error", null, "Server error".getBytes(), null);
        
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(apiError));

        // When
        Mono<StructuredItinerary> result = openAIService.generateStructuredItinerary(requirements, conversationContext);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(webClient).post();
    }

    @Test
    void generateStructuredItinerary_withoutContext_shouldWork() {
        // Given
        String requirements = "Visit Tokyo for 3 days";
        String mockItineraryJson = createMockStructuredItineraryJson();
        String mockJsonResponse = createMockOpenAIResponse(mockItineraryJson);
        
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        // When
        Mono<StructuredItinerary> result = openAIService.generateStructuredItinerary(requirements, null);

        // Then
        StepVerifier.create(result)
                .assertNext(itinerary -> {
                    assertNotNull(itinerary);
                    assertNotNull(itinerary.getMetadata());
                })
                .verifyComplete();

        verify(webClient).post();
    }

    @Test
    void generateStructuredItinerary_emptyContext_shouldWork() {
        // Given
        String requirements = "Visit Tokyo for 3 days";
        String mockItineraryJson = createMockStructuredItineraryJson();
        String mockJsonResponse = createMockOpenAIResponse(mockItineraryJson);
        
        setupWebClientMocks();
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        // When
        Mono<StructuredItinerary> result = openAIService.generateStructuredItinerary(requirements, "");

        // Then
        StepVerifier.create(result)
                .assertNext(itinerary -> {
                    assertNotNull(itinerary);
                    assertNotNull(itinerary.getMetadata());
                })
                .verifyComplete();

        verify(webClient).post();
    }

    @Test
    void buildRequestBody_shouldCreateCorrectStructure() {
        // Given
        String userRequirements = "Visit Tokyo for 3 days";

        // When
        Map<String, Object> requestBody = callBuildRequestBody(userRequirements);

        // Then
        assertNotNull(requestBody);
        assertEquals("gpt-4", requestBody.get("model"));
        assertEquals(0.7, requestBody.get("temperature"));
        assertEquals(2000, requestBody.get("max_tokens"));
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> messages = (java.util.List<Map<String, String>>) requestBody.get("messages");
        assertNotNull(messages);
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals(userRequirements, messages.get(1).get("content"));
    }

    @Test
    void buildStructuredRequestBody_shouldCreateCorrectStructure() {
        // Given
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous context";

        // When
        Map<String, Object> requestBody = callBuildStructuredRequestBody(requirements, conversationContext);

        // Then
        assertNotNull(requestBody);
        assertEquals("gpt-4", requestBody.get("model"));
        assertEquals(0.7, requestBody.get("temperature"));
        assertEquals(2000, requestBody.get("max_tokens"));
        
        // response_format is only added for models that support JSON mode
        // gpt-4 doesn't support JSON mode, so response_format should be null
        assertNull(requestBody.get("response_format"));
        
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, String>> messages = (java.util.List<Map<String, String>>) requestBody.get("messages");
        assertNotNull(messages);
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("user", messages.get(1).get("role"));
    }

    @Test
    void buildSystemPrompt_shouldReturnValidPrompt() {
        // When
        String prompt = callBuildSystemPrompt();

        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("expert travel planner"));
        assertTrue(prompt.contains("detailed"));
        assertTrue(prompt.contains("personalized"));
    }

    @Test
    void buildStructuredSystemPrompt_shouldReturnValidPrompt() {
        // When
        String prompt = callBuildStructuredSystemPrompt();

        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("JSON format"));
        assertTrue(prompt.contains("geographic coordinates"));
        assertTrue(prompt.contains("activityId"));
    }

    @Test
    void buildStructuredUserPrompt_withContext_shouldIncludeContext() {
        // Given
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation context";

        // When
        String prompt = callBuildStructuredUserPrompt(requirements, conversationContext);

        // Then
        assertNotNull(prompt);
        assertTrue(prompt.contains("PREVIOUS CONVERSATION:"));
        assertTrue(prompt.contains(conversationContext));
        assertTrue(prompt.contains("CURRENT REQUEST:"));
        assertTrue(prompt.contains(requirements));
    }

    @Test
    void buildStructuredUserPrompt_withoutContext_shouldNotIncludeContext() {
        // Given
        String requirements = "Visit Tokyo for 3 days";

        // When
        String prompt = callBuildStructuredUserPrompt(requirements, null);

        // Then
        assertNotNull(prompt);
        assertFalse(prompt.contains("PREVIOUS CONVERSATION:"));
        assertTrue(prompt.contains("CURRENT REQUEST:"));
        assertTrue(prompt.contains(requirements));
    }

    @Test
    void extractContent_validResponse_shouldExtractContent() {
        // Given
        String responseBody = createMockOpenAIResponse("Generated itinerary content");

        // When
        String content = callExtractContent(responseBody);

        // Then
        assertEquals("Generated itinerary content", content);
    }

    @Test
    void extractContent_invalidResponse_shouldThrowException() {
        // Given
        String invalidResponse = "Invalid JSON";

        // When & Then
        assertThrows(RuntimeException.class, () -> callExtractContent(invalidResponse));
    }

    @Test
    void parseStructuredItinerary_validJson_shouldParseCorrectly() {
        // Given
        String jsonContent = createMockStructuredItineraryJson();

        // When
        StructuredItinerary itinerary = callParseStructuredItinerary(jsonContent);

        // Then
        assertNotNull(itinerary);
        assertNotNull(itinerary.getMetadata());
        assertEquals("Tokyo, Japan", itinerary.getMetadata().getDestination());
        assertEquals(3, itinerary.getMetadata().getTotalDays());
    }

    @Test
    void parseStructuredItinerary_invalidJson_shouldThrowException() {
        // Given
        String invalidJson = "Invalid JSON";

        // When & Then
        assertThrows(RuntimeException.class, () -> callParseStructuredItinerary(invalidJson));
    }

    @Test
    void handleApiError_shouldCreateMeaningfulException() {
        // Given
        WebClientResponseException error = WebClientResponseException.create(
                400, "Bad Request", null, "Invalid request".getBytes(), null);

        // When
        RuntimeException result = callHandleApiError(error);

        // Then
        assertNotNull(result);
        assertTrue(result.getMessage().contains("OpenAI API error"));
        assertTrue(result.getMessage().contains("400"));
        assertTrue(result.getMessage().contains("Bad Request"));
        assertEquals(error, result.getCause());
    }

    private void setupWebClientMocks() {
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        doReturn(requestBodySpec).when(requestBodyUriSpec).bodyValue(any());
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        // Note: responseSpec.bodyToMono() is intentionally not mocked here
        // Individual tests should set up their own response
    }

    private String createMockOpenAIResponse(String content) {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, String> message = new HashMap<>();
            message.put("content", content);
            choice.put("message", message);
            response.put("choices", java.util.List.of(choice));
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createMockStructuredItineraryJson() {
        try {
            Map<String, Object> itinerary = new HashMap<>();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("destination", "Tokyo, Japan");
            metadata.put("totalDays", 3);
            metadata.put("startDate", "2024-01-01");
            metadata.put("endDate", "2024-01-03");
            metadata.put("budget", "Medium");
            metadata.put("interests", java.util.List.of("Culture", "Food"));
            
            itinerary.put("metadata", metadata);
            itinerary.put("days", java.util.List.of());
            
            return objectMapper.writeValueAsString(itinerary);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Helper methods to access private methods using reflection
    @SuppressWarnings("unchecked")
    private Map<String, Object> callBuildRequestBody(String userRequirements) {
        try {
            return (Map<String, Object>) ReflectionTestUtils.invokeMethod(openAIService, "buildRequestBody", userRequirements);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callBuildStructuredRequestBody(String requirements, String conversationContext) {
        try {
            return (Map<String, Object>) ReflectionTestUtils.invokeMethod(openAIService, "buildStructuredRequestBody", requirements, conversationContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callBuildSystemPrompt() {
        try {
            return (String) ReflectionTestUtils.invokeMethod(openAIService, "buildSystemPrompt");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callBuildStructuredSystemPrompt() {
        try {
            return (String) ReflectionTestUtils.invokeMethod(openAIService, "buildStructuredSystemPrompt");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callBuildStructuredUserPrompt(String requirements, String conversationContext) {
        try {
            return (String) ReflectionTestUtils.invokeMethod(openAIService, "buildStructuredUserPrompt", requirements, conversationContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String callExtractContent(String responseBody) {
        try {
            return (String) ReflectionTestUtils.invokeMethod(openAIService, "extractContent", responseBody);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private StructuredItinerary callParseStructuredItinerary(String jsonContent) {
        try {
            return (StructuredItinerary) ReflectionTestUtils.invokeMethod(openAIService, "parseStructuredItinerary", jsonContent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RuntimeException callHandleApiError(WebClientResponseException error) {
        try {
            return (RuntimeException) ReflectionTestUtils.invokeMethod(openAIService, "handleApiError", error);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}