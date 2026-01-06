package com.voyageai.voyageaibackend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyageai.voyageaibackend.domain.model.ConversationMessage;
import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskType;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningServiceTest {

    @Mock
    private OpenAIService openAIService;

    @Mock
    private RedisTaskService taskService;

    @Mock
    private TravelPlanService travelPlanService;

    @Mock
    private ConversationHistoryService conversationHistoryService;

    @Mock
    private GeocodingService geocodingService;

    private PlanningService planningService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        planningService = new PlanningService(
                openAIService,
                taskService,
                travelPlanService,
                conversationHistoryService,
                geocodingService
        );
        objectMapper = new ObjectMapper();
    }

    @Test
    void generatePlanAsync_success_shouldCompleteSuccessfully() throws Exception {
        // Given
        String taskId = "task-123";
        String projectId = "project-456";
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation";
        StructuredItinerary mockItinerary = createMockStructuredItinerary();
        PlanningTask mockTask = createMockTask(taskId);

        when(conversationHistoryService.buildContextForAi(projectId)).thenReturn(conversationContext);
        when(openAIService.generateStructuredItinerary(requirements, conversationContext))
                .thenReturn(Mono.just(mockItinerary));
        when(geocodingService.isGeocodingAvailable()).thenReturn(false);
        when(taskService.getTask(taskId)).thenReturn(Optional.of(mockTask));

        // When
        CompletableFuture<StructuredItinerary> result = planningService.generatePlanAsync(taskId, projectId, requirements);

        // Then
        StructuredItinerary completedItinerary = result.get();
        assertNotNull(completedItinerary);
        assertEquals("Tokyo, Japan", completedItinerary.getMetadata().getDestination());

        verify(taskService).markProcessing(taskId);
        verify(taskService).updateProgress(eq(taskId), eq("正在分析您的需求..."), eq(10));
        verify(taskService).updateProgress(eq(taskId), eq("调用大语言模型生成行程..."), eq(40));
        verify(taskService).updateProgress(eq(taskId), eq("保存行程数据..."), eq(80));
        verify(taskService).updateProgress(eq(taskId), eq("更新对话历史..."), eq(90));
        verify(taskService).markCompleted(eq(taskId), eq(mockItinerary), anyString());
        verify(conversationHistoryService, times(2)).addMessage(eq(projectId), any(ConversationMessage.class));
        verify(travelPlanService).createPlan(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void generatePlanAsync_withGeocoding_shouldEnrichLocations() throws Exception {
        // Given
        String taskId = "task-123";
        String projectId = "project-456";
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation";
        StructuredItinerary mockItinerary = createMockStructuredItinerary();
        PlanningTask mockTask = createMockTask(taskId);

        when(conversationHistoryService.buildContextForAi(projectId)).thenReturn(conversationContext);
        when(openAIService.generateStructuredItinerary(requirements, conversationContext))
                .thenReturn(Mono.just(mockItinerary));
        when(geocodingService.isGeocodingAvailable()).thenReturn(true);
        when(geocodingService.enrichLocation(anyString(), anyString()))
                .thenReturn(Mono.just(createMockEnrichedLocation()));
        when(taskService.getTask(taskId)).thenReturn(Optional.of(mockTask));

        // When
        CompletableFuture<StructuredItinerary> result = planningService.generatePlanAsync(taskId, projectId, requirements);

        // Then
        StructuredItinerary completedItinerary = result.get();
        assertNotNull(completedItinerary);

        verify(taskService).updateProgress(eq(taskId), eq("验证地理坐标..."), eq(60));
        verify(geocodingService).enrichLocation(anyString(), anyString());
    }

    @Test
    void generatePlanAsync_aiReturnsNull_shouldThrowException() {
        // Given
        String taskId = "task-123";
        String projectId = "project-456";
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation";

        when(conversationHistoryService.buildContextForAi(projectId)).thenReturn(conversationContext);
        when(openAIService.generateStructuredItinerary(requirements, conversationContext))
                .thenReturn(Mono.empty());

        // When
        CompletableFuture<StructuredItinerary> result = planningService.generatePlanAsync(taskId, projectId, requirements);

        // Then
        assertThrows(Exception.class, result::get);
        verify(taskService).markFailed(eq(taskId), contains("AI returned null itinerary"));
    }

    @Test
    void generatePlanAsync_openaiError_shouldMarkTaskFailed() {
        // Given
        String taskId = "task-123";
        String projectId = "project-456";
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation";
        RuntimeException openaiError = new RuntimeException("OpenAI API error");

        when(conversationHistoryService.buildContextForAi(projectId)).thenReturn(conversationContext);
        when(openAIService.generateStructuredItinerary(requirements, conversationContext))
                .thenReturn(Mono.error(openaiError));

        // When
        CompletableFuture<StructuredItinerary> result = planningService.generatePlanAsync(taskId, projectId, requirements);

        // Then
        assertThrows(Exception.class, result::get);
        verify(taskService).markFailed(eq(taskId), contains("Failed to generate plan"));
    }

    @Test
    void generatePlanAsync_taskNotFound_shouldMarkTaskFailed() throws Exception {
        // Given
        String taskId = "task-123";
        String projectId = "project-456";
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation";
        StructuredItinerary mockItinerary = createMockStructuredItinerary();

        when(conversationHistoryService.buildContextForAi(projectId)).thenReturn(conversationContext);
        when(openAIService.generateStructuredItinerary(requirements, conversationContext))
                .thenReturn(Mono.just(mockItinerary));
        when(geocodingService.isGeocodingAvailable()).thenReturn(false);
        when(taskService.getTask(taskId)).thenReturn(Optional.empty());

        // When
        CompletableFuture<StructuredItinerary> result = planningService.generatePlanAsync(taskId, projectId, requirements);

        // Then
        assertThrows(Exception.class, result::get);
        verify(taskService).markFailed(eq(taskId), contains("Task not found"));
    }

    @Test
    void generatePlanAsync_geocodingError_shouldContinueWithOriginalCoordinates() throws Exception {
        // Given
        String taskId = "task-123";
        String projectId = "project-456";
        String requirements = "Visit Tokyo for 3 days";
        String conversationContext = "Previous conversation";
        StructuredItinerary mockItinerary = createMockStructuredItinerary();
        PlanningTask mockTask = createMockTask(taskId);

        when(conversationHistoryService.buildContextForAi(projectId)).thenReturn(conversationContext);
        when(openAIService.generateStructuredItinerary(requirements, conversationContext))
                .thenReturn(Mono.just(mockItinerary));
        when(geocodingService.isGeocodingAvailable()).thenReturn(true);
        when(geocodingService.enrichLocation(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Geocoding error")));
        when(taskService.getTask(taskId)).thenReturn(Optional.of(mockTask));

        // When
        CompletableFuture<StructuredItinerary> result = planningService.generatePlanAsync(taskId, projectId, requirements);

        // Then
        StructuredItinerary completedItinerary = result.get();
        assertNotNull(completedItinerary);
        verify(geocodingService).enrichLocation(anyString(), anyString());
    }

    @Test
    void submitPlanningRequest_initialPlanning_shouldCreateTaskAndTriggerGeneration() {
        // Given
        String userId = "user-123";
        String projectId = "project-456";
        String requirements = "Visit Tokyo for 3 days";
        PlanningTask mockTask = createMockTask("task-123");

        when(conversationHistoryService.getMessageCount(projectId)).thenReturn(0L);
        when(taskService.createTask(userId, projectId, requirements)).thenReturn(mockTask);

        // When
        String taskId = planningService.submitPlanningRequest(userId, projectId, requirements);

        // Then
        assertEquals("task-123", taskId);
        verify(taskService).createTask(userId, projectId, requirements);
        verify(conversationHistoryService).getMessageCount(projectId);
    }

    @Test
    void submitPlanningRequest_conversationUpdate_shouldCreateTaskWithCorrectType() {
        // Given
        String userId = "user-123";
        String projectId = "project-456";
        String requirements = "Modify the itinerary";
        PlanningTask mockTask = createMockTask("task-123");

        when(conversationHistoryService.getMessageCount(projectId)).thenReturn(5L);
        when(taskService.createTask(userId, projectId, requirements)).thenReturn(mockTask);

        // When
        String taskId = planningService.submitPlanningRequest(userId, projectId, requirements);

        // Then
        assertEquals("task-123", taskId);
        verify(taskService).createTask(userId, projectId, requirements);
        verify(conversationHistoryService).getMessageCount(projectId);
    }

    @Test
    void enrichItineraryLocations_withValidItinerary_shouldEnrichLocations() {
        // Given
        StructuredItinerary itinerary = createMockStructuredItinerary();
        when(geocodingService.enrichLocation(anyString(), anyString()))
                .thenReturn(Mono.just(createMockEnrichedLocation()));

        // When
        callEnrichItineraryLocations(itinerary);

        // Then
        verify(geocodingService).enrichLocation(anyString(), anyString());
    }

    @Test
    void enrichItineraryLocations_withNullDays_shouldNotProcess() {
        // Given
        StructuredItinerary itinerary = StructuredItinerary.builder()
                .metadata(StructuredItinerary.ItineraryMetadata.builder()
                        .destination("Tokyo, Japan")
                        .totalDays(3)
                        .build())
                .days(null)
                .build();

        // When
        callEnrichItineraryLocations(itinerary);

        // Then
        verify(geocodingService, never()).enrichLocation(anyString(), anyString());
    }

    @Test
    void enrichItineraryLocations_withNullActivities_shouldNotProcess() {
        // Given
        StructuredItinerary itinerary = StructuredItinerary.builder()
                .metadata(StructuredItinerary.ItineraryMetadata.builder()
                        .destination("Tokyo, Japan")
                        .totalDays(3)
                        .build())
                .days(java.util.List.of(
                        StructuredItinerary.DailyItinerary.builder()
                                .dayNumber(1)
                                .activities(null)
                                .build()
                ))
                .build();

        // When
        callEnrichItineraryLocations(itinerary);

        // Then
        verify(geocodingService, never()).enrichLocation(anyString(), anyString());
    }

    @Test
    void enrichItineraryLocations_withNullLocation_shouldNotProcess() {
        // Given
        StructuredItinerary itinerary = StructuredItinerary.builder()
                .metadata(StructuredItinerary.ItineraryMetadata.builder()
                        .destination("Tokyo, Japan")
                        .totalDays(3)
                        .build())
                .days(java.util.List.of(
                        StructuredItinerary.DailyItinerary.builder()
                                .dayNumber(1)
                                .activities(java.util.List.of(
                                        StructuredItinerary.Activity.builder()
                                                .title("Test Activity")
                                                .location(null)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        // When
        callEnrichItineraryLocations(itinerary);

        // Then
        verify(geocodingService, never()).enrichLocation(anyString(), anyString());
    }

    @Test
    void enrichItineraryLocations_withEnrichmentError_shouldContinue() {
        // Given
        StructuredItinerary itinerary = createMockStructuredItinerary();
        when(geocodingService.enrichLocation(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Geocoding error")));

        // When
        callEnrichItineraryLocations(itinerary);

        // Then
        verify(geocodingService).enrichLocation(anyString(), anyString());
    }

    private StructuredItinerary createMockStructuredItinerary() {
        return StructuredItinerary.builder()
                .metadata(StructuredItinerary.ItineraryMetadata.builder()
                        .destination("Tokyo, Japan")
                        .totalDays(3)
                        .startDate("2024-01-01")
                        .endDate("2024-01-03")
                        .budget("Medium")
                        .interests(java.util.List.of("Culture", "Food"))
                        .build())
                .days(java.util.List.of(
                        StructuredItinerary.DailyItinerary.builder()
                                .dayNumber(1)
                                .date("2024-01-01")
                                .theme("Tokyo Introduction")
                                .activities(java.util.List.of(
                                        StructuredItinerary.Activity.builder()
                                                .activityId("act-day1-001")
                                                .time("09:00-10:00")
                                                .title("Visit Senso-ji Temple")
                                                .description("Explore Tokyo's oldest temple")
                                                .type(StructuredItinerary.ActivityType.SIGHTSEEING)
                                                .location(StructuredItinerary.Location.builder()
                                                        .name("Senso-ji Temple")
                                                        .address("2-3-1 Asakusa, Taito City, Tokyo")
                                                        .latitude(35.7148)
                                                        .longitude(139.7967)
                                                        .placeType("Temple")
                                                        .build())
                                                .tips("Arrive early to avoid crowds")
                                                .estimatedCost("Free")
                                                .durationMinutes(60)
                                                .build()
                                ))
                                .summary("First day exploring Tokyo")
                                .build()
                ))
                .build();
    }

    private StructuredItinerary.Location createMockEnrichedLocation() {
        return StructuredItinerary.Location.builder()
                .name("Senso-ji Temple")
                .address("2-3-1 Asakusa, Taito City, Tokyo")
                .latitude(35.7148)
                .longitude(139.7967)
                .placeId("ChIJ51cu8IcbXWARiRtXIothAS4")
                .placeType("Temple")
                .build();
    }

    private PlanningTask createMockTask(String taskId) {
        return PlanningTask.builder()
                .taskId(taskId)
                .userId("user-123")
                .projectId("project-456")
                .requirements("Test requirements")
                .status(PlanningTask.TaskStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private void callEnrichItineraryLocations(StructuredItinerary itinerary) {
        try {
            ReflectionTestUtils.invokeMethod(planningService, "enrichItineraryLocations", itinerary);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}