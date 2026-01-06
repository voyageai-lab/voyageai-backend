package com.voyageai.voyageaibackend.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document model for TravelPlan.
 * 
 * <p>This is the MongoDB-specific representation of a travel plan.
 * It uses Spring Data MongoDB annotations for document mapping.
 * 
 * <p>Index design:
 * <ul>
 *   <li>Compound index on (userId, planId) for efficient lookups</li>
 *   <li>Index on userId for user-specific queries</li>
 *   <li>Index on projectId for project-based filtering</li>
 * </ul>
 * 
 * <p>Key differences from DynamoDB model:
 * <ul>
 *   <li>Uses _id (ObjectId or custom) instead of partition/sort key</li>
 *   <li>Supports flexible secondary indexes</li>
 *   <li>Native date types with auditing support</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "travel_plans")
@CompoundIndexes({
    @CompoundIndex(name = "user_plan_idx", def = "{'userId': 1, 'planId': 1}", unique = true)
})
public class TravelPlanDocument {

  /**
   * MongoDB document ID.
   * Using planId as the document ID for consistency with DynamoDB model.
   */
  @Id
  private String id;

  /**
   * User ID who owns this travel plan.
   * Indexed for efficient user-based queries.
   */
  @Indexed
  private String userId;

  /**
   * Unique plan identifier.
   * Combined with userId forms a unique constraint.
   */
  private String planId;

  /**
   * Project ID that this plan belongs to.
   * Links to TravelProject in MySQL.
   */
  @Indexed
  private String projectId;

  /**
   * Travel plan title.
   */
  private String title;

  /**
   * Main destination or destinations.
   */
  private String destination;

  /**
   * Detailed travel plan data stored as JSON string.
   * Contains itinerary, budget, AI recommendations, etc.
   */
  private String planData;

  /**
   * Plan status (DRAFT, ACTIVE, COMPLETED, ARCHIVED).
   */
  @Indexed
  private String status;

  /**
   * Start date of the trip (ISO-8601 format).
   */
  private String startDate;

  /**
   * End date of the trip (ISO-8601 format).
   */
  private String endDate;

  /**
   * Timestamp when the plan was created.
   * Automatically set by Spring Data auditing.
   */
  @CreatedDate
  private Instant createdAt;

  /**
   * Timestamp when the plan was last updated.
   * Automatically updated by Spring Data auditing.
   */
  @LastModifiedDate
  private Instant updatedAt;

  /**
   * Convert to the common TravelPlan model.
   *
   * @return TravelPlan instance
   */
  public TravelPlan toTravelPlan() {
    return TravelPlan.builder()
        .userId(userId)
        .planId(planId)
        .projectId(projectId)
        .title(title)
        .destination(destination)
        .planData(planData)
        .status(status)
        .startDate(startDate)
        .endDate(endDate)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }

  /**
   * Create from the common TravelPlan model.
   *
   * @param plan the TravelPlan to convert
   * @return TravelPlanDocument instance
   */
  public static TravelPlanDocument fromTravelPlan(TravelPlan plan) {
    return TravelPlanDocument.builder()
        .id(plan.getPlanId()) // Use planId as document ID
        .userId(plan.getUserId())
        .planId(plan.getPlanId())
        .projectId(plan.getProjectId())
        .title(plan.getTitle())
        .destination(plan.getDestination())
        .planData(plan.getPlanData())
        .status(plan.getStatus())
        .startDate(plan.getStartDate())
        .endDate(plan.getEndDate())
        .createdAt(plan.getCreatedAt())
        .updatedAt(plan.getUpdatedAt())
        .build();
  }
}

