package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.entity.TravelProject;
import com.voyageai.voyageaibackend.domain.entity.User;
import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.repo.UserRepository;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import com.voyageai.voyageaibackend.service.ConversationHistoryService;
import com.voyageai.voyageaibackend.service.PlanningService;
import com.voyageai.voyageaibackend.service.RedisTaskService;
import com.voyageai.voyageaibackend.service.TravelProjectService;
import com.voyageai.voyageaibackend.web.dto.ConversationHistoryResponse;
import com.voyageai.voyageaibackend.web.dto.PlanningRequest;
import com.voyageai.voyageaibackend.web.dto.PlanningResponse;
import com.voyageai.voyageaibackend.web.dto.TaskStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AI-powered travel planning.
 * 
 * <p>This controller implements an asynchronous API pattern:
 * <ol>
 *   <li>POST /generate: Submit request, get task ID immediately</li>
 *   <li>GET /status/{taskId}: Poll for task status and result</li>
 * </ol>
 * 
 * <p>This pattern is essential for long-running operations that may take
 * several seconds or minutes to complete (AI API calls, complex computations).
 * 
 * <p><b>Authentication Required:</b> All endpoints in this controller require
 * the user to be authenticated with a valid JWT token.
 */
@RestController
@RequestMapping("/api/planning")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Planning", 
     description = "AI-powered travel planning endpoints (authentication required)")
public class PlanningController {

  private final PlanningService planningService;
  private final RedisTaskService taskService;
  private final TravelProjectService projectService;
  private final UserRepository userRepository;
  private final ConversationHistoryService conversationHistoryService;

  /**
   * Submits a travel planning request.
   * 
   * <p>This endpoint returns immediately with a task ID. The actual AI generation
   * happens asynchronously in the background.
   * 
   * <p>Workflow:
   * <pre>
   * 1. Client sends requirements
   * 2. Server creates task and returns task ID (200 OK)
   * 3. Server processes request asynchronously
   * 4. Client polls /status/{taskId} for results
   * </pre>
   *
   * @param request Planning request with user requirements
   * @param principal Authenticated user
   * @return Response with task ID and status URL
   */
  @PostMapping("/generate")
  @Operation(summary = "Submit planning request", 
             description = "Creates async task for AI travel plan generation. "
                 + "Auto-creates a project if none exists.")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<PlanningResponse> generatePlan(
      @Valid @RequestBody PlanningRequest request,
      Principal principal
  ) {
    // Extract user from Spring Security context
    User user;
    if (principal instanceof org.springframework.security.core.Authentication) {
      Object principalObj = ((org.springframework.security.core.Authentication) principal)
          .getPrincipal();
      if (principalObj instanceof User) {
        user = (User) principalObj;
      } else {
        // Fallback for Spring Security User or testing scenarios
        String email = principal.getName();
        user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
      }
    } else {
      // Fallback for other scenarios
      String email = principal.getName();
      user = userRepository.findByEmail(email)
          .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }
    
    log.info("Received planning request from user: {}", user.getEmail());

    // Auto-create a project with title from first few words of requirements
    String title = extractTitle(request.getRequirements());
    TravelProject project = projectService.createProject(user.getId(), title, null);

    // Submit async request with projectId
    String taskId = planningService.submitPlanningRequest(
        user.getId().toString(), 
        project.getProjectId(), 
        request.getRequirements()
    );

    // Build response
    PlanningResponse response = PlanningResponse.builder()
        .taskId(taskId)
        .message("Planning request submitted. Use taskId to check status.")
        .statusUrl("/api/planning/status/" + taskId)
        .build();

    return ResponseEntity.ok(response);
  }
  
  /**
   * Extracts a title from requirements (first few words).
   *
   * @param requirements User requirements
   * @return Extracted title
   */
  private String extractTitle(String requirements) {
    String[] words = requirements.split("\\s+");
    int maxWords = Math.min(6, words.length);
    String title = String.join(" ", java.util.Arrays.copyOfRange(words, 0, maxWords));
    if (words.length > maxWords) {
      title += "...";
    }
    return title;
  }

  /**
   * Retrieves the status and result of a planning task.
   * 
   * <p>Clients should poll this endpoint periodically to check task progress.
   * Recommended polling interval: 2-5 seconds.
   * 
   * <p>Status responses:
   * <ul>
   *   <li>PENDING: Task is queued, waiting to be processed</li>
   *   <li>PROCESSING: AI is generating the plan</li>
   *   <li>COMPLETED: Plan is ready, result field contains itinerary</li>
   *   <li>FAILED: An error occurred, errorMessage field explains what happened</li>
   * </ul>
   *
   * @param taskId Task ID from /generate endpoint
   * @return Task status and result (if completed)
   */
  @GetMapping("/status/{taskId}")
  @Operation(summary = "Get task status", 
             description = "Retrieves status and result of planning task")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
    log.debug("Status check for task: {}", taskId);

    PlanningTask task = taskService.getTask(taskId)
        .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

    TaskStatusResponse response = TaskStatusResponse.builder()
        .taskId(task.getTaskId())
        .taskType(task.getTaskType())
        .status(task.getStatus())
        .progressMessage(task.getProgressMessage())
        .progressPercent(task.getProgressPercent())
        .requirements(task.getRequirements())
        .structuredItinerary(task.getStructuredItinerary())
        .result(task.getResult())
        .errorMessage(task.getErrorMessage())
        .createdAt(task.getCreatedAt())
        .updatedAt(task.getUpdatedAt())
        .completedAt(task.getCompletedAt())
        .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Retrieves conversation history for a project.
   * 
   * <p>Returns chronological list of all messages in the conversation,
   * including user messages, AI responses, itinerary results, and tool call results.
   * 
   * <p>Frontend uses this to:
   * <ul>
   *   <li>Render complete chat history when user returns to a project</li>
   *   <li>Display previous itineraries and modifications</li>
   *   <li>Show tool call results (hotels, restaurants, etc.)</li>
   * </ul>
   * 
   * <p>Data source: Redis (fast, recent messages) with MySQL fallback (complete history).
   *
   * @param projectId Project ID to get history for
   * @param limit Maximum number of messages to return (default: 50)
   * @return Conversation history response
   */
  @GetMapping("/projects/{projectId}/history")
  @Operation(summary = "Get conversation history", 
             description = "Retrieves full conversation history for a travel planning project")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<ConversationHistoryResponse> getConversationHistory(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "50") int limit
  ) {
    log.debug("Retrieving conversation history for project: {} (limit: {})", projectId, limit);

    // Get messages from service (Redis + MySQL dual storage)
    List<com.voyageai.voyageaibackend.domain.model.ConversationMessage> messages =
        conversationHistoryService.getHistory(projectId, limit);

    // Get total count
    long totalCount = conversationHistoryService.getMessageCount(projectId);

    ConversationHistoryResponse response = ConversationHistoryResponse.builder()
        .projectId(projectId)
        .messages(messages)
        .totalCount(totalCount)
        .build();

    return ResponseEntity.ok(response);
  }

  /**
   * Cancels a planning task.
   * 
   * <p>This endpoint allows users to cancel tasks that are no longer needed.
   * Only tasks in PENDING or PROCESSING states can be cancelled.
   * 
   * <p>Use cases:
   * <ul>
   *   <li>User navigates away before task completes</li>
   *   <li>User decides to regenerate with different requirements</li>
   *   <li>User accidentally submitted wrong request</li>
   * </ul>
   *
   * @param taskId Task ID to cancel
   * @return 204 No Content if cancelled successfully
   */
  @DeleteMapping("/tasks/{taskId}")
  @Operation(summary = "Cancel a planning task", 
             description = "Cancels a task that is PENDING or PROCESSING")
  @SecurityRequirement(name = "bearer-jwt")
  public ResponseEntity<Void> cancelTask(@PathVariable String taskId) {
    log.info("Cancelling task: {}", taskId);

    boolean cancelled = taskService.cancelTask(taskId);

    if (cancelled) {
      return ResponseEntity.noContent().build();
    } else {
      // Task not found or already completed/failed
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }
}

