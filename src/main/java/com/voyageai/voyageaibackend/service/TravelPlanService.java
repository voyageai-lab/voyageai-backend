package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.model.TravelPlan;
import com.voyageai.voyageaibackend.domain.repo.TravelPlanStore;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for managing travel plans stored in NoSQL (MongoDB or DynamoDB).
 * 
 * <p>This service handles the persistence layer for AI-generated travel plans,
 * providing operations for creating, retrieving, and managing travel plans
 * with their associated projects.
 * 
 * <p>The underlying storage is abstracted via {@link TravelPlanStore} interface,
 * allowing seamless switching between MongoDB and DynamoDB via configuration:
 * <pre>
 * storage.nosql.provider=mongodb   # or dynamodb
 * </pre>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Plan creation with project association</li>
 *   <li>Version management within projects</li>
 *   <li>User-specific plan queries</li>
 *   <li>Project-based plan retrieval</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TravelPlanService {

  private final TravelPlanStore travelPlanStore;

  /**
   * Creates a new travel plan from a completed AI task.
   * 
   *
   * @param userId User ID who owns the plan
   * @param projectId Project ID this plan belongs to
   * @param requirements Original user requirements
   * @param aiResult AI-generated travel itinerary
   * @return Created travel plan
   */
  public TravelPlan createPlan(String userId, String projectId, String requirements, 
      String aiResult) {
    String planId = "plan-" + UUID.randomUUID().toString().substring(0, 8);
    
    // Extract title from requirements (first 50 characters)
    String title = extractTitleFromRequirements(requirements);
    
    // Extract destination from requirements (simple heuristic)
    String destination = extractDestinationFromRequirements(requirements);
    
    TravelPlan plan = TravelPlan.builder()
        .userId(userId)
        .planId(planId)
        .projectId(projectId)
        .title(title)
        .destination(destination)
        .planData(aiResult)
        .status("COMPLETED")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    
    TravelPlan savedPlan = travelPlanStore.save(plan);
    
    log.info("Created travel plan {} for project {} (user: {}, provider: {})", 
        planId, projectId, userId, travelPlanStore.getProviderName());
    
    return savedPlan;
  }

  /**
   * Retrieves a travel plan by user ID and plan ID.
   * 
   *
   * @param userId User ID
   * @param planId Plan ID
   * @return Travel plan if found
   * @throws ResourceNotFoundException if plan not found
   */
  public TravelPlan getPlan(String userId, String planId) {
    Optional<TravelPlan> plan = travelPlanStore.findByUserIdAndPlanId(userId, planId);
    
    if (plan.isEmpty()) {
      throw new ResourceNotFoundException("Travel plan not found: " + planId);
    }
    
    return plan.get();
  }

  /**
   * Retrieves all travel plans for a user.
   * 
   *
   * @param userId User ID
   * @return List of travel plans ordered by creation date (newest first)
   */
  public List<TravelPlan> getUserPlans(String userId) {
    return travelPlanStore.findByUserId(userId);
  }

  /**
   * Retrieves all travel plans for a specific project.
   * 
   *
   * @param userId User ID (for security)
   * @param projectId Project ID
   * @return List of travel plans for the project ordered by creation date
   */
  public List<TravelPlan> getProjectPlans(String userId, String projectId) {
    return travelPlanStore.findByUserIdAndProjectId(userId, projectId);
  }

  /**
   * Gets the latest plan for a project.
   * 
   *
   * @param userId User ID
   * @param projectId Project ID
   * @return Latest travel plan for the project
   * @throws ResourceNotFoundException if no plans found
   */
  public TravelPlan getLatestPlan(String userId, String projectId) {
    List<TravelPlan> plans = getProjectPlans(userId, projectId);
    
    if (plans.isEmpty()) {
      throw new ResourceNotFoundException("No plans found for project: " + projectId);
    }
    
    // Plans are already ordered by createdAt desc from repository
    return plans.get(0);
  }

  /**
   * Updates a travel plan's status.
   * 
   *
   * @param userId User ID
   * @param planId Plan ID
   * @param status New status
   * @return Updated travel plan
   */
  public TravelPlan updatePlanStatus(String userId, String planId, String status) {
    TravelPlan plan = getPlan(userId, planId);
    plan.setStatus(status);
    plan.setUpdatedAt(Instant.now());
    
    return travelPlanStore.save(plan);
  }

  /**
   * Deletes a travel plan.
   * 
   *
   * @param userId User ID
   * @param planId Plan ID
   */
  public void deletePlan(String userId, String planId) {
    TravelPlan plan = getPlan(userId, planId);
    travelPlanStore.delete(userId, planId);
    
    log.info("Deleted travel plan {} for user {}", planId, userId);
  }

  /**
   * Counts travel plans for a user.
   * 
   *
   * @param userId User ID
   * @return Number of plans
   */
  public long countUserPlans(String userId) {
    return travelPlanStore.countByUserId(userId);
  }

  /**
   * Counts travel plans for a project.
   * 
   *
   * @param userId User ID
   * @param projectId Project ID
   * @return Number of plans in the project
   */
  public long countProjectPlans(String userId, String projectId) {
    return travelPlanStore.countByUserIdAndProjectId(userId, projectId);
  }

  /**
   * Checks if a plan exists for a user.
   * 
   *
   * @param userId User ID
   * @param planId Plan ID
   * @return True if plan exists
   */
  public boolean planExists(String userId, String planId) {
    return travelPlanStore.exists(userId, planId);
  }

  /**
   * Get the current storage provider name.
   *
   * @return provider name (MongoDB or DynamoDB)
   */
  public String getStorageProvider() {
    return travelPlanStore.getProviderName();
  }

  /**
   * Extracts a title from user requirements.
   * 
   *
   * @param requirements User requirements
   * @return Extracted title (max 50 characters)
   */
  private String extractTitleFromRequirements(String requirements) {
    if (requirements == null || requirements.trim().isEmpty()) {
      return "Untitled Plan";
    }
    
    String title = requirements.trim();
    if (title.length() > 50) {
      title = title.substring(0, 47) + "...";
    }
    
    return title;
  }

  /**
   * Extracts destination from user requirements using simple heuristics.
   * 
   *
   * @param requirements User requirements
   * @return Extracted destination or "Unknown"
   */
  private String extractDestinationFromRequirements(String requirements) {
    if (requirements == null || requirements.trim().isEmpty()) {
      return "Unknown";
    }
    
    String lower = requirements.toLowerCase();
    
    // Common destination keywords
    String[] destinations = {
        "tokyo", "paris", "london", "new york", "rome", "barcelona", "amsterdam",
        "berlin", "prague", "vienna", "madrid", "lisbon", "athens", "istanbul",
        "dubai", "singapore", "bangkok", "seoul", "hong kong", "sydney",
        "melbourne", "toronto", "vancouver", "montreal", "mexico city",
        "rio de janeiro", "buenos aires", "lima", "santiago"
    };
    
    for (String dest : destinations) {
      if (lower.contains(dest)) {
        return capitalizeWords(dest);
      }
    }
    
    // Try to extract city names (simple pattern)
    String[] words = requirements.split("\\s+");
    for (String word : words) {
      if (word.length() > 3 && Character.isUpperCase(word.charAt(0))) {
        return word;
      }
    }
    
    return "Unknown";
  }

  /**
   * Capitalizes words in a string.
   * 
   *
   * @param text Text to capitalize
   * @return Capitalized text
   */
  private String capitalizeWords(String text) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    
    String[] words = text.split("\\s+");
    StringBuilder result = new StringBuilder();
    
    for (int i = 0; i < words.length; i++) {
      if (i > 0) {
        result.append(" ");
      }
      if (!words[i].isEmpty()) {
        result.append(Character.toUpperCase(words[i].charAt(0)))
              .append(words[i].substring(1).toLowerCase());
      }
    }
    
    return result.toString();
  }
}
