package com.voyageai.voyageaibackend.domain.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.model.TravelPlan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * Unit tests for {@link DynamoDbTravelPlanStore}.
 * Tests DynamoDB-specific operations and business logic.
 */
@ExtendWith(MockitoExtension.class)
class DynamoDbTravelPlanStoreTest {

  @Mock
  private DynamoDbEnhancedClient enhancedClient;

  @Mock
  private DynamoDbTable<TravelPlan> table;

  private DynamoDbTravelPlanStore store;

  @BeforeEach
  void setUp() {
    when(enhancedClient.table("TravelPlans", TableSchema.fromBean(TravelPlan.class)))
        .thenReturn(table);
    store = new DynamoDbTravelPlanStore(enhancedClient);
  }

  @Test
  void save_newPlan_shouldSetTimestampsAndSave() {
    // Given
    TravelPlan plan = createTestPlan("user-123", "plan-456");
    plan.setCreatedAt(null); // New plan

    // When
    TravelPlan result = store.save(plan);

    // Then
    assertNotNull(result.getCreatedAt());
    assertNotNull(result.getUpdatedAt());
    verify(table).putItem(plan);
  }

  @Test
  void save_existingPlan_shouldUpdateTimestamp() {
    // Given
    TravelPlan plan = createTestPlan("user-123", "plan-456");
    Instant originalCreatedAt = Instant.now().minusSeconds(3600);
    plan.setCreatedAt(originalCreatedAt);

    // When
    TravelPlan result = store.save(plan);

    // Then
    assertEquals(originalCreatedAt, result.getCreatedAt());
    assertNotNull(result.getUpdatedAt());
    assertTrue(result.getUpdatedAt().isAfter(originalCreatedAt));
    verify(table).putItem(plan);
  }

  @Test
  void findByUserIdAndPlanId_existingPlan_shouldReturnPlan() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    TravelPlan expectedPlan = createTestPlan(userId, planId);

    when(table.getItem(any(Key.class))).thenReturn(expectedPlan);

    // When
    Optional<TravelPlan> result = store.findByUserIdAndPlanId(userId, planId);

    // Then
    assertTrue(result.isPresent());
    assertEquals(expectedPlan, result.get());
    verify(table).getItem(any(Key.class));
  }

  @Test
  void findByUserIdAndPlanId_nonExistentPlan_shouldReturnEmpty() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";

    when(table.getItem(any(Key.class))).thenReturn(null);

    // When
    Optional<TravelPlan> result = store.findByUserIdAndPlanId(userId, planId);

    // Then
    assertFalse(result.isPresent());
    verify(table).getItem(any(Key.class));
  }

  @Test
  void findByUserId_existingPlans_shouldReturnAllPlans() {
    // Given
    String userId = "user-123";
    TravelPlan plan1 = createTestPlan(userId, "plan-1");
    TravelPlan plan2 = createTestPlan(userId, "plan-2");
    Page<TravelPlan> page = Page.create(List.of(plan1, plan2));

    PageIterable<TravelPlan> pageIterable = mock(PageIterable.class);
    when(pageIterable.stream()).thenReturn(java.util.stream.Stream.of(page));
    when(table.query(any(QueryEnhancedRequest.class))).thenReturn(pageIterable);

    // When
    List<TravelPlan> result = store.findByUserId(userId);

    // Then
    assertEquals(2, result.size());
    assertTrue(result.contains(plan1));
    assertTrue(result.contains(plan2));
    verify(table).query(any(QueryEnhancedRequest.class));
  }

  @Test
  void findByUserId_noPlans_shouldReturnEmptyList() {
    // Given
    String userId = "user-123";
    Page<TravelPlan> page = Page.create(List.of());

    PageIterable<TravelPlan> pageIterable = mock(PageIterable.class);
    when(pageIterable.stream()).thenReturn(java.util.stream.Stream.of(page));
    when(table.query(any(QueryEnhancedRequest.class))).thenReturn(pageIterable);

    // When
    List<TravelPlan> result = store.findByUserId(userId);

    // Then
    assertTrue(result.isEmpty());
    verify(table).query(any(QueryEnhancedRequest.class));
  }

  @Test
  void delete_existingPlan_shouldReturnTrue() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    TravelPlan deletedPlan = createTestPlan(userId, planId);

    when(table.deleteItem(any(Key.class))).thenReturn(deletedPlan);

    // When
    boolean result = store.delete(userId, planId);

    // Then
    assertTrue(result);
    verify(table).deleteItem(any(Key.class));
  }

  @Test
  void delete_nonExistentPlan_shouldReturnFalse() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";

    when(table.deleteItem(any(Key.class))).thenReturn(null);

    // When
    boolean result = store.delete(userId, planId);

    // Then
    assertFalse(result);
    verify(table).deleteItem(any(Key.class));
  }

  @Test
  void exists_existingPlan_shouldReturnTrue() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";
    TravelPlan plan = createTestPlan(userId, planId);

    when(table.getItem(any(Key.class))).thenReturn(plan);

    // When
    boolean result = store.exists(userId, planId);

    // Then
    assertTrue(result);
    verify(table).getItem(any(Key.class));
  }

  @Test
  void exists_nonExistentPlan_shouldReturnFalse() {
    // Given
    String userId = "user-123";
    String planId = "plan-456";

    when(table.getItem(any(Key.class))).thenReturn(null);

    // When
    boolean result = store.exists(userId, planId);

    // Then
    assertFalse(result);
    verify(table).getItem(any(Key.class));
  }

  @Test
  void findByUserIdAndProjectId_existingPlans_shouldReturnFilteredPlans() {
    // Given
    String userId = "user-123";
    String projectId = "project-456";
    TravelPlan plan1 = createTestPlan(userId, "plan-1");
    plan1.setProjectId(projectId);
    TravelPlan plan2 = createTestPlan(userId, "plan-2");
    plan2.setProjectId("different-project");
    TravelPlan plan3 = createTestPlan(userId, "plan-3");
    plan3.setProjectId(projectId);

    Page<TravelPlan> page = Page.create(List.of(plan1, plan2, plan3));

    PageIterable<TravelPlan> pageIterable = mock(PageIterable.class);
    when(pageIterable.stream()).thenReturn(java.util.stream.Stream.of(page));
    when(table.query(any(QueryEnhancedRequest.class))).thenReturn(pageIterable);

    // When
    List<TravelPlan> result = store.findByUserIdAndProjectId(userId, projectId);

    // Then
    assertEquals(2, result.size());
    assertTrue(result.contains(plan1));
    assertTrue(result.contains(plan3));
    assertFalse(result.contains(plan2));
    verify(table).query(any(QueryEnhancedRequest.class));
  }

  @Test
  void countByUserId_existingPlans_shouldReturnCount() {
    // Given
    String userId = "user-123";
    TravelPlan plan1 = createTestPlan(userId, "plan-1");
    TravelPlan plan2 = createTestPlan(userId, "plan-2");
    Page<TravelPlan> page = Page.create(List.of(plan1, plan2));

    PageIterable<TravelPlan> pageIterable = mock(PageIterable.class);
    when(pageIterable.stream()).thenReturn(java.util.stream.Stream.of(page));
    when(table.query(any(QueryEnhancedRequest.class))).thenReturn(pageIterable);

    // When
    long result = store.countByUserId(userId);

    // Then
    assertEquals(2, result);
    verify(table).query(any(QueryEnhancedRequest.class));
  }

  @Test
  void getProviderName_shouldReturnDynamoDB() {
    // When
    String provider = store.getProviderName();

    // Then
    assertEquals("DynamoDB", provider);
  }

  @Test
  void isHealthy_whenTableAccessible_shouldReturnTrue() {
    // Given
    when(table.tableName()).thenReturn("TravelPlans");

    // When
    boolean healthy = store.isHealthy();

    // Then
    assertTrue(healthy);
  }

  @Test
  void isHealthy_whenTableNotAccessible_shouldReturnFalse() {
    // Given
    when(table.tableName()).thenThrow(new RuntimeException("Connection failed"));

    // When
    boolean healthy = store.isHealthy();

    // Then
    assertFalse(healthy);
  }

  @Test
  void getTable_shouldReturnTable() {
    // When
    DynamoDbTable<TravelPlan> result = store.getTable();

    // Then
    assertEquals(table, result);
  }

  private TravelPlan createTestPlan(String userId, String planId) {
    TravelPlan plan = new TravelPlan();
    plan.setUserId(userId);
    plan.setPlanId(planId);
    plan.setProjectId("project-456");
    plan.setTitle("Test Plan");
    plan.setDestination("Test Destination");
    plan.setPlanData("Test Plan Data");
    plan.setStatus("DRAFT");
    plan.setStartDate("2024-01-01T00:00:00Z");
    plan.setEndDate("2024-01-03T23:59:59Z");
    plan.setCreatedAt(Instant.now());
    plan.setUpdatedAt(Instant.now());
    return plan;
  }
}

