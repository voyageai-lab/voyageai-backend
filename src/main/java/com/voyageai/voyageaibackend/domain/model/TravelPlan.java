package com.voyageai.voyageaibackend.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * TravelPlan model representing a travel plan stored in DynamoDB.
 * 
 * <p>This model uses DynamoDB Enhanced Client annotations for ORM-like functionality.
 * The table structure uses composite key design:
 * <ul>
 *   <li>Partition Key: userId (enables querying all plans for a user)</li>
 *   <li>Sort Key: planId (unique plan identifier)</li>
 * </ul>
 * 
 * <p>This design allows for efficient queries like:
 * <ul>
 *   <li>Get all plans for a user</li>
 *   <li>Get a specific plan by user and plan ID</li>
 *   <li>Query plans with filters (status, destination, etc.)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class TravelPlan {

  /**
   * User ID who owns this travel plan.
   * This is the partition key for DynamoDB.
   * All plans for a user will be co-located for efficient queries.
   */
  private String userId;

  /**
   * Unique plan identifier.
   * This is the sort key for DynamoDB.
   * Format: UUID or timestamp-based ID (e.g., "plan-2025-10-13-abc123").
   */
  private String planId;

  /**
   * Project ID that this plan belongs to.
   * Links to TravelProject in MySQL for version management and context.
   * Example: "proj-abc123"
   */
  private String projectId;

  /**
   * Travel plan title.
   * Example: "Japan Cherry Blossom Tour 2025"
   */
  private String title;

  /**
   * Main destination or destinations.
   * Example: "Tokyo, Kyoto, Osaka"
   */
  private String destination;

  /**
   * Detailed travel plan data stored as JSON string.
   * This is a flexible structure that can contain:
   * - Itinerary (day-by-day schedule)
   * - Budget breakdown
   * - Accommodation details
   * - Transportation information
   * - AI-generated recommendations
   * 
   * <p>Example structure:
   * <pre>
   * {
   *   "days": [
   *     {
   *       "day": 1,
   *       "activities": ["Visit Tokyo Tower", "Dinner at Shibuya"],
   *       "accommodation": "Hotel ABC",
   *       "budget": 200
   *     }
   *   ],
   *   "totalBudget": 2000,
   *   "aiRecommendations": ["Try ramen at XYZ"]
   * }
   * </pre>
   * 
   * <p>Store as JSON string to allow flexible schema evolution.
   * Use Jackson ObjectMapper to serialize/deserialize when needed.
   */
  private String planData;

  /**
   * Plan status.
   * Example values: "DRAFT", "ACTIVE", "COMPLETED", "ARCHIVED"
   */
  private String status;

  /**
   * Start date of the trip (ISO-8601 format).
   * Example: "2025-04-01T00:00:00Z"
   */
  private String startDate;

  /**
   * End date of the trip (ISO-8601 format).
   * Example: "2025-04-10T23:59:59Z"
   */
  private String endDate;

  /**
   * Timestamp when the plan was created.
   */
  private Instant createdAt;

  /**
   * Timestamp when the plan was last updated.
   */
  private Instant updatedAt;

  /**
   * DynamoDB partition key accessor.
   * Defines userId as the partition key for sharding.
   *
   * @return the user ID
   */
  @DynamoDbPartitionKey
  public String getUserId() {
    return userId;
  }

  /**
   * DynamoDB sort key accessor.
   * Defines planId as the sort key for ordering within a partition.
   *
   * @return the plan ID
   */
  @DynamoDbSortKey
  public String getPlanId() {
    return planId;
  }
}

