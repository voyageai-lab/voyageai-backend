package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.model.TravelPlan;
import com.voyageai.voyageaibackend.domain.repo.TravelPlanStore;
import com.voyageai.voyageaibackend.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link TravelPlanService}.
 * Tests NoSQL operations (MongoDB/DynamoDB) and business logic.
 */
@ExtendWith(MockitoExtension.class)
class TravelPlanServiceTest {

  @Mock
  private TravelPlanStore travelPlanStore;

  private TravelPlanService travelPlanService;

  @BeforeEach
  void setUp() {
    travelPlanService = new TravelPlanService(travelPlanStore);
  }

  @Test
  void createPlan_shouldCreateAndSavePlan() {
    // Given
    String userId = "user-123";
    String projectId = "proj-456";
    String requirements = "Plan a 5-day trip to Tokyo for cherry blossom season";
    String aiResult = "Day 1: Arrive in Tokyo, visit Senso-ji Temple...";

    TravelPlan expectedPlan = TravelPlan.builder()
        .userId(userId)
        .planId("plan-abc123")
        .projectId(projectId)
        .title("Plan a 5-day trip to Tokyo for...")
        .destination("Tokyo")
        .planData(aiResult)
        .status("COMPLETED")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    when(travelPlanStore.save(any(TravelPlan.class))).thenReturn(expectedPlan);
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");

    // When
    TravelPlan result = travelPlanService.createPlan(userId, projectId, requirements, aiResult);

    // Then
    assertNotNull(result);
    assertEquals(userId, result.getUserId());
    assertEquals(projectId, result.getProjectId());
    assertEquals(aiResult, result.getPlanData());
    assertEquals("COMPLETED", result.getStatus());
    assertTrue(result.getTitle().contains("Tokyo"));
    assertEquals("Tokyo", result.getDestination());
    assertNotNull(result.getCreatedAt());
    assertNotNull(result.getUpdatedAt());

    verify(travelPlanStore).save(any(TravelPlan.class));
  }

  @Test
  void createPlan_shouldExtractTitleFromRequirements() {
    // Given
    String requirements = "Plan a wonderful 7-day adventure in beautiful Paris with museums and food";
    when(travelPlanStore.save(any(TravelPlan.class))).thenAnswer(invocation -> {
      TravelPlan plan = invocation.getArgument(0);
      return plan;
    });
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");

    // When
    TravelPlan result = travelPlanService.createPlan("user-123", "proj-456", requirements, "AI result");

    // Then
    assertEquals("Plan a wonderful 7-day adventure in beautiful P...", result.getTitle());
    assertEquals("Paris", result.getDestination());
  }

  @Test
  void createPlan_longRequirements_shouldTruncateTitle() {
    // Given
    String longRequirements = "Plan an extremely detailed and comprehensive 30-day journey through multiple countries in Europe including France, Germany, Italy, Spain, and many more destinations with extensive cultural experiences";
    when(travelPlanStore.save(any(TravelPlan.class))).thenAnswer(invocation -> {
      TravelPlan plan = invocation.getArgument(0);
      return plan;
    });
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");

    // When
    TravelPlan result = travelPlanService.createPlan("user-123", "proj-456", longRequirements, "AI result");

    // Then
    assertEquals(50, result.getTitle().length());
    assertTrue(result.getTitle().endsWith("..."));
  }

  @Test
  void getPlan_shouldReturnPlanWhenFound() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    TravelPlan expectedPlan = TravelPlan.builder()
        .userId(userId)
        .planId(planId)
        .projectId("proj-789")
        .title("Test Plan")
        .planData("Test data")
        .status("COMPLETED")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    when(travelPlanStore.findByUserIdAndPlanId(userId, planId))
        .thenReturn(Optional.of(expectedPlan));

    // When
    TravelPlan result = travelPlanService.getPlan(userId, planId);

    // Then
    assertEquals(expectedPlan, result);
    verify(travelPlanStore).findByUserIdAndPlanId(userId, planId);
  }

  @Test
  void getPlan_planNotFound_shouldThrowException() {
    // Given
    String userId = "user-123";
    String planId = "plan-nonexistent";
    when(travelPlanStore.findByUserIdAndPlanId(userId, planId))
        .thenReturn(Optional.empty());

    // When / Then
    assertThrows(ResourceNotFoundException.class,
        () -> travelPlanService.getPlan(userId, planId));
  }

  @Test
  void getUserPlans_shouldReturnAllPlansForUser() {
    // Given
    String userId = "user-123";
    List<TravelPlan> expectedPlans = List.of(
        TravelPlan.builder().planId("plan-1").userId(userId).build(),
        TravelPlan.builder().planId("plan-2").userId(userId).build()
    );

    when(travelPlanStore.findByUserId(userId)).thenReturn(expectedPlans);

    // When
    List<TravelPlan> result = travelPlanService.getUserPlans(userId);

    // Then
    assertEquals(expectedPlans, result);
    verify(travelPlanStore).findByUserId(userId);
  }

  @Test
  void getProjectPlans_shouldReturnPlansForProject() {
    // Given
    String userId = "user-123";
    String projectId = "proj-456";
    List<TravelPlan> expectedPlans = List.of(
        TravelPlan.builder().planId("plan-1").userId(userId).projectId(projectId).build(),
        TravelPlan.builder().planId("plan-2").userId(userId).projectId(projectId).build()
    );

    when(travelPlanStore.findByUserIdAndProjectId(userId, projectId))
        .thenReturn(expectedPlans);

    // When
    List<TravelPlan> result = travelPlanService.getProjectPlans(userId, projectId);

    // Then
    assertEquals(expectedPlans, result);
    verify(travelPlanStore).findByUserIdAndProjectId(userId, projectId);
  }

  @Test
  void getLatestPlan_shouldReturnFirstPlan() {
    // Given
    String userId = "user-123";
    String projectId = "proj-456";
    TravelPlan latestPlan = TravelPlan.builder()
        .planId("plan-latest")
        .userId(userId)
        .projectId(projectId)
        .createdAt(Instant.now())
        .build();
    TravelPlan olderPlan = TravelPlan.builder()
        .planId("plan-older")
        .userId(userId)
        .projectId(projectId)
        .createdAt(Instant.now().minusSeconds(3600))
        .build();

    when(travelPlanStore.findByUserIdAndProjectId(userId, projectId))
        .thenReturn(List.of(latestPlan, olderPlan));

    // When
    TravelPlan result = travelPlanService.getLatestPlan(userId, projectId);

    // Then
    assertEquals(latestPlan, result);
  }

  @Test
  void getLatestPlan_noPlans_shouldThrowException() {
    // Given
    String userId = "user-123";
    String projectId = "proj-empty";
    when(travelPlanStore.findByUserIdAndProjectId(userId, projectId))
        .thenReturn(List.of());

    // When / Then
    assertThrows(ResourceNotFoundException.class,
        () -> travelPlanService.getLatestPlan(userId, projectId));
  }

  @Test
  void updatePlanStatus_shouldUpdateAndSave() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    String newStatus = "ARCHIVED";
    TravelPlan plan = TravelPlan.builder()
        .userId(userId)
        .planId(planId)
        .status("COMPLETED")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();

    when(travelPlanStore.findByUserIdAndPlanId(userId, planId))
        .thenReturn(Optional.of(plan));
    when(travelPlanStore.save(any(TravelPlan.class))).thenReturn(plan);

    // When
    TravelPlan result = travelPlanService.updatePlanStatus(userId, planId, newStatus);

    // Then
    assertEquals(newStatus, result.getStatus());
    assertNotNull(result.getUpdatedAt());
    verify(travelPlanStore).save(plan);
  }

  @Test
  void deletePlan_shouldDeleteFromStore() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    TravelPlan plan = TravelPlan.builder()
        .userId(userId)
        .planId(planId)
        .build();

    when(travelPlanStore.findByUserIdAndPlanId(userId, planId))
        .thenReturn(Optional.of(plan));

    // When
    travelPlanService.deletePlan(userId, planId);

    // Then
    verify(travelPlanStore).delete(userId, planId);
  }

  @Test
  void countUserPlans_shouldReturnCount() {
    // Given
    String userId = "user-123";
    when(travelPlanStore.countByUserId(userId)).thenReturn(5L);

    // When
    long count = travelPlanService.countUserPlans(userId);

    // Then
    assertEquals(5L, count);
    verify(travelPlanStore).countByUserId(userId);
  }

  @Test
  void countProjectPlans_shouldReturnCount() {
    // Given
    String userId = "user-123";
    String projectId = "proj-456";
    when(travelPlanStore.countByUserIdAndProjectId(userId, projectId)).thenReturn(3L);

    // When
    long count = travelPlanService.countProjectPlans(userId, projectId);

    // Then
    assertEquals(3L, count);
    verify(travelPlanStore).countByUserIdAndProjectId(userId, projectId);
  }

  @Test
  void planExists_shouldCheckExistence() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    when(travelPlanStore.exists(userId, planId)).thenReturn(true);

    // When
    boolean exists = travelPlanService.planExists(userId, planId);

    // Then
    assertTrue(exists);
    verify(travelPlanStore).exists(userId, planId);
  }

  @Test
  void getStorageProvider_shouldReturnProviderName() {
    // Given
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");

    // When
    String provider = travelPlanService.getStorageProvider();

    // Then
    assertEquals("MongoDB", provider);
    verify(travelPlanStore).getProviderName();
  }
}
