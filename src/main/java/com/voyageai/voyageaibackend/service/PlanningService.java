package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.model.ConversationMessage;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskType;
import com.voyageai.voyageaibackend.domain.model.StructuredItinerary;
import com.voyageai.voyageaibackend.kafka.KafkaProducerService;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for asynchronous travel plan generation.
 * 
 * <p>Supports two modes via {@code planning.mode} property:
 * <ul>
 *   <li>{@code kafka} (default) - Publishes request to Kafka topic, processed by Python AI Worker</li>
 *   <li>{@code async} - Direct @Async processing in Java (legacy fallback)</li>
 * </ul>
 * 
 * <p>Kafka mode workflow:
 * <ol>
 *   <li>Create task in Redis</li>
 *   <li>Publish PlanningRequestEvent to {@code planning.request} topic</li>
 *   <li>Python worker consumes, processes, publishes progress/result events</li>
 *   <li>Java KafkaConsumerService handles progress/result events</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class PlanningService {

  private final OpenAIService openAIService;
  private final RedisTaskService taskService;
  private final TravelPlanService travelPlanService;
  private final ConversationHistoryService conversationHistoryService;
  private final GeocodingService geocodingService;
  private final KafkaProducerService kafkaProducerService;

  @Value("${planning.mode:kafka}")
  private String planningMode;

  /**
   * Generates a travel plan asynchronously with structured data and progress tracking.
   * 
   * <p>This method is executed in a separate thread from the async executor pool.
   * The calling thread continues immediately after invoking this method.
   * 
   * <p>Enhanced workflow with progress tracking and structured output:
   * <pre>
   * 1. Mark task as PROCESSING (0%)
   * 2. Build conversation context from history (10%)
   * 3. Call OpenAI API for structured itinerary (40%)
   * 4. Enrich locations with Google Places API (60%)
   * 5. Save structured itinerary to DynamoDB (80%)
   * 6. Save conversation messages (90%)
   * 7. Mark task as COMPLETED (100%)
   * 8. If error occurs, mark task as FAILED
   * </pre>
   * 
   * <p>Real-time progress updates are sent via SSE to provide user feedback.
   *
   * @param taskId The task ID to update
   * @param projectId Project ID for conversation context
   * @param requirements User's travel requirements
   * @return CompletableFuture that completes when generation is done
   */
  @Async("taskExecutor")
  public CompletableFuture<StructuredItinerary> generatePlanAsync(
      String taskId, 
      String projectId,
      String requirements) {
    log.info("Starting async plan generation for task: {} in project: {} on thread: {}", 
        taskId, projectId, Thread.currentThread().getName());

    try {
      // Step 1: Mark task as PROCESSING
      taskService.markProcessing(taskId);
      taskService.updateProgress(taskId, "正在分析您的需求...", 10);

      // Save user message to conversation history
      ConversationMessage userMessage = ConversationMessage.builder()
          .messageId("msg-" + UUID.randomUUID())
          .projectId(projectId)
          .role(ConversationMessage.Role.USER)
          .messageType(ConversationMessage.MessageType.TEXT)
          .content(requirements)
          .timestamp(Instant.now())
          .build();
      conversationHistoryService.addMessage(projectId, userMessage);

      // Step 2: Build conversation context from history
      String conversationContext = conversationHistoryService.buildContextForAi(projectId);
      log.debug("Built conversation context with {} characters", conversationContext.length());

      // Step 3: Call OpenAI API for structured itinerary
      taskService.updateProgress(taskId, "调用大语言模型生成行程...", 40);
      StructuredItinerary itinerary = openAIService
          .generateStructuredItinerary(requirements, conversationContext)
          .block();  // Safe to block here because we're in async thread

      if (itinerary == null) {
        throw new IllegalStateException("AI returned null itinerary");
      }

      // Step 4: Enrich locations with Google Places API (optional, if API key configured)
      if (geocodingService.isGeocodingAvailable()) {
        taskService.updateProgress(taskId, "验证地理坐标...", 60);
        enrichItineraryLocations(itinerary);
      }

      // Step 5: Save structured itinerary to DynamoDB
      taskService.updateProgress(taskId, "保存行程数据...", 80);
      var task = taskService.getTask(taskId)
          .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));
      
      // Convert structured itinerary to JSON for storage
      String itineraryJson = new com.fasterxml.jackson.databind.ObjectMapper()
          .writeValueAsString(itinerary);
      
      travelPlanService.createPlan(
          task.getUserId(),
          task.getProjectId(),
          task.getRequirements(),
          itineraryJson
      );

      // Step 6: Save AI response to conversation history
      taskService.updateProgress(taskId, "更新对话历史...", 90);
      ConversationMessage aiMessage = ConversationMessage.builder()
          .messageId("msg-" + UUID.randomUUID())
          .projectId(projectId)
          .role(ConversationMessage.Role.ASSISTANT)
          .messageType(ConversationMessage.MessageType.ITINERARY)
          .content("Here's your personalized travel itinerary:")
          .structuredData(itineraryJson)
          .timestamp(Instant.now())
          .build();
      conversationHistoryService.addMessage(projectId, aiMessage);

      // Step 7: Mark task as COMPLETED with structured itinerary
      log.info("Marking task {} as COMPLETED", taskId);
      taskService.markCompleted(taskId, itinerary, itineraryJson);

      log.info("Successfully completed plan generation for task: {} (saved to DynamoDB)", taskId);
      return CompletableFuture.completedFuture(itinerary);

    } catch (Exception e) {
      // Update task with error
      String errorMessage = "Failed to generate plan: " + e.getMessage();
      taskService.markFailed(taskId, errorMessage);

      log.error("Failed to generate plan for task: {}", taskId, e);
      return CompletableFuture.failedFuture(e);
    }
  }

  /**
   * Enriches itinerary locations with Google Places API data.
   * 
   * <p>This method asynchronously validates and enhances location coordinates
   * for better map integration. Runs in parallel for efficiency.
   *
   * @param itinerary Itinerary to enrich
   */
  private void enrichItineraryLocations(StructuredItinerary itinerary) {
    if (itinerary.getDays() == null) {
      return;
    }

    for (var day : itinerary.getDays()) {
      if (day.getActivities() == null) {
        continue;
      }

      for (var activity : day.getActivities()) {
        if (activity.getLocation() == null || activity.getLocation().getName() == null) {
          continue;
        }

        try {
          // Enrich location with Google Places API
          var enrichedLocation = geocodingService
              .enrichLocation(
                  activity.getLocation().getName(),
                  itinerary.getMetadata() != null 
                      ? itinerary.getMetadata().getDestination() 
                      : ""
              )
              .block();

          // Update location if enrichment successful and has coordinates
          if (enrichedLocation != null && enrichedLocation.getLatitude() != null) {
            activity.setLocation(enrichedLocation);
          }
        } catch (Exception e) {
          log.warn("Failed to enrich location '{}': {}", 
              activity.getLocation().getName(), e.getMessage());
          // Continue with AI-generated coordinates
        }
      }
    }
  }

  /**
   * Submits a planning request and returns immediately with task ID.
   * 
   * <p>This is the main entry point for travel plan generation. It:
   * <ol>
   *   <li>Creates a new task in the project</li>
   *   <li>Determines task type (INITIAL_PLANNING or CONVERSATION_UPDATE)</li>
   *   <li>Triggers async generation with conversation context</li>
   *   <li>Returns task ID immediately (doesn't wait for completion)</li>
   * </ol>
   *
   * @param userId User ID submitting the request
   * @param projectId Project ID this task belongs to
   * @param requirements User's travel requirements
   * @return Task ID for status tracking
   */
  public String submitPlanningRequest(String userId, String projectId, String requirements) {
    // Determine task type based on conversation history
    long messageCount = conversationHistoryService.getMessageCount(projectId);
    TaskType taskType = messageCount > 0 
        ? TaskType.CONVERSATION_UPDATE 
        : TaskType.INITIAL_PLANNING;

    // Create task associated with the project
    var task = taskService.createTask(userId, projectId, requirements);
    task.setTaskType(taskType);
    String taskId = task.getTaskId();

    if ("kafka".equalsIgnoreCase(planningMode)) {
      // Build conversation context BEFORE saving the new user message
      // so the context reflects the history up to (but not including) this message.
      String conversationContext = null;
      if (taskType == TaskType.CONVERSATION_UPDATE) {
        try {
          conversationContext = conversationHistoryService.buildContextForAi(projectId);
          log.info("Built conversation context ({} chars) for project: {}",
              conversationContext.length(), projectId);
        } catch (Exception e) {
          log.warn("Failed to build conversation context: {}", e.getMessage());
        }
      }

      // Save user message to conversation history BEFORE sending to Kafka
      // (In @Async mode, generatePlanAsync handles this, but Kafka mode skips it)
      try {
        ConversationMessage userMessage = ConversationMessage.builder()
            .messageId("msg-" + UUID.randomUUID())
            .projectId(projectId)
            .role(ConversationMessage.Role.USER)
            .messageType(ConversationMessage.MessageType.TEXT)
            .content(requirements)
            .timestamp(Instant.now())
            .build();
        conversationHistoryService.addMessage(projectId, userMessage);
      } catch (Exception e) {
        log.warn("Failed to save user message to history: {}", e.getMessage());
        // Don't block the planning request if history save fails
      }

      // Kafka mode: publish to Kafka topic for Python worker (with conversation context)
      kafkaProducerService.sendPlanningRequest(
          taskId, userId, projectId, requirements, taskType.name(), conversationContext);
      log.info("Submitted {} request via Kafka with task ID: {} in project: {}",
          taskType, taskId, projectId);
    } else {
      // Legacy async mode: direct @Async processing in Java
      // (generatePlanAsync saves user message internally)
      generatePlanAsync(taskId, projectId, requirements);
      log.info("Submitted {} request via @Async with task ID: {} in project: {}",
          taskType, taskId, projectId);
    }

    return taskId;
  }
}

