package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.model.TravelPlan;
import java.util.List;
import java.util.Optional;

/**
 * Abstract interface for TravelPlan storage operations.
 * 
 * <p>This interface defines the contract for storing and retrieving travel plans,
 * allowing seamless switching between different NoSQL backends (MongoDB, DynamoDB).
 * 
 * <p>Implementations:
 * <ul>
 *   <li>{@code MongoDbTravelPlanStore} - MongoDB implementation (platform-agnostic)</li>
 *   <li>{@code DynamoDbTravelPlanStore} - AWS DynamoDB implementation</li>
 * </ul>
 * 
 * <p>Configuration property to switch providers:
 * <pre>
 * storage.nosql.provider=mongodb  # or dynamodb
 * </pre>
 */
public interface TravelPlanStore {

  /**
   * Save or update a travel plan.
   * If a plan with the same userId and planId exists, it will be overwritten.
   *
   * @param travelPlan the travel plan to save
   * @return the saved travel plan with timestamps updated
   */
  TravelPlan save(TravelPlan travelPlan);

  /**
   * Find a travel plan by user ID and plan ID.
   *
   * @param userId the user ID
   * @param planId the plan ID
   * @return Optional containing the plan if found, empty otherwise
   */
  Optional<TravelPlan> findByUserIdAndPlanId(String userId, String planId);

  /**
   * Find all travel plans for a specific user.
   *
   * @param userId the user ID
   * @return list of travel plans for the user
   */
  List<TravelPlan> findByUserId(String userId);

  /**
   * Delete a travel plan by user ID and plan ID.
   *
   * @param userId the user ID
   * @param planId the plan ID
   * @return true if the plan was deleted, false if it didn't exist
   */
  boolean delete(String userId, String planId);

  /**
   * Check if a travel plan exists.
   *
   * @param userId the user ID
   * @param planId the plan ID
   * @return true if the plan exists, false otherwise
   */
  boolean exists(String userId, String planId);

  /**
   * Find all travel plans for a user within a specific project.
   *
   * @param userId the user ID
   * @param projectId the project ID to filter by
   * @return list of travel plans for the user in the project
   */
  List<TravelPlan> findByUserIdAndProjectId(String userId, String projectId);

  /**
   * Count total number of plans for a user.
   *
   * @param userId the user ID
   * @return number of plans for the user
   */
  long countByUserId(String userId);

  /**
   * Count plans for a user within a specific project.
   *
   * @param userId the user ID
   * @param projectId the project ID
   * @return number of plans for the user in the project
   */
  long countByUserIdAndProjectId(String userId, String projectId);

  /**
   * Get the name of the storage provider.
   *
   * @return provider name (e.g., "MongoDB", "DynamoDB")
   */
  String getProviderName();

  /**
   * Check if the storage is healthy and accessible.
   *
   * @return true if the storage is accessible
   */
  boolean isHealthy();
}

