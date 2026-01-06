package com.voyageai.voyageaibackend.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TravelPlan} model.
 */
class TravelPlanTest {

  @Test
  void travelPlan_builderPattern_shouldCreateInstance() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    String title = "Tokyo Adventure";
    String destination = "Tokyo, Japan";
    String planData = "{\"days\": 5}";
    String status = "DRAFT";
    String startDate = "2025-04-01";
    String endDate = "2025-04-05";
    Instant now = Instant.now();

    // When
    TravelPlan plan = TravelPlan.builder()
        .userId(userId)
        .planId(planId)
        .title(title)
        .destination(destination)
        .planData(planData)
        .status(status)
        .startDate(startDate)
        .endDate(endDate)
        .createdAt(now)
        .updatedAt(now)
        .build();

    // Then
    assertNotNull(plan);
    assertEquals(userId, plan.getUserId());
    assertEquals(planId, plan.getPlanId());
    assertEquals(title, plan.getTitle());
    assertEquals(destination, plan.getDestination());
    assertEquals(planData, plan.getPlanData());
    assertEquals(status, plan.getStatus());
    assertEquals(startDate, plan.getStartDate());
    assertEquals(endDate, plan.getEndDate());
    assertEquals(now, plan.getCreatedAt());
    assertEquals(now, plan.getUpdatedAt());
  }

  @Test
  void travelPlan_noArgsConstructor_shouldCreateEmptyInstance() {
    // When
    TravelPlan plan = new TravelPlan();

    // Then
    assertNotNull(plan);
    assertNull(plan.getUserId());
    assertNull(plan.getPlanId());
    assertNull(plan.getTitle());
  }

  @Test
  void travelPlan_allArgsConstructor_shouldCreateInstance() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    String title = "Tokyo Adventure";
    String destination = "Tokyo, Japan";
    String planData = "{\"days\": 5}";
    String status = "DRAFT";
    String startDate = "2025-04-01";
    String endDate = "2025-04-05";
    Instant now = Instant.now();

    // When
    TravelPlan plan = TravelPlan.builder()
        .userId(userId)
        .planId(planId)
        .projectId("proj-123")
        .title(title)
        .destination(destination)
        .planData(planData)
        .status(status)
        .startDate(startDate)
        .endDate(endDate)
        .createdAt(now)
        .updatedAt(now)
        .build();

    // Then
    assertNotNull(plan);
    assertEquals(userId, plan.getUserId());
    assertEquals(planId, plan.getPlanId());
  }

  @Test
  void travelPlan_setters_shouldUpdateFields() {
    // Given
    TravelPlan plan = new TravelPlan();
    String userId = "user-999";
    String title = "Updated Title";

    // When
    plan.setUserId(userId);
    plan.setTitle(title);

    // Then
    assertEquals(userId, plan.getUserId());
    assertEquals(title, plan.getTitle());
  }

  @Test
  void travelPlan_partitionKey_shouldReturnUserId() {
    // Given
    TravelPlan plan = new TravelPlan();
    plan.setUserId("user-123");

    // When
    String partitionKey = plan.getUserId();

    // Then
    assertEquals("user-123", partitionKey);
  }

  @Test
  void travelPlan_sortKey_shouldReturnPlanId() {
    // Given
    TravelPlan plan = new TravelPlan();
    plan.setPlanId("plan-456");

    // When
    String sortKey = plan.getPlanId();

    // Then
    assertEquals("plan-456", sortKey);
  }

  @Test
  void travelPlan_jsonPlanData_shouldStoreFlexibleSchema() {
    // Given
    TravelPlan plan = new TravelPlan();
    String complexJson = """
        {
          "days": [
            {"day": 1, "activities": ["Visit Tokyo Tower"]},
            {"day": 2, "activities": ["Explore Shibuya"]}
          ],
          "budget": 2000,
          "currency": "USD"
        }
        """;

    // When
    plan.setPlanData(complexJson);

    // Then
    assertEquals(complexJson, plan.getPlanData());
  }

  @Test
  void travelPlan_timestamps_shouldBeSettable() {
    // Given
    TravelPlan plan = new TravelPlan();
    Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
    Instant updatedAt = Instant.parse("2025-01-02T00:00:00Z");

    // When
    plan.setCreatedAt(createdAt);
    plan.setUpdatedAt(updatedAt);

    // Then
    assertEquals(createdAt, plan.getCreatedAt());
    assertEquals(updatedAt, plan.getUpdatedAt());
  }
}

