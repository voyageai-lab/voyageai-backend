package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.model.TravelPlan;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

/**
 * DynamoDB implementation of TravelPlanStore.
 * 
 * <p>This implementation provides AWS DynamoDB storage for travel plans.
 * It is activated when {@code storage.nosql.provider=dynamodb}.
 * 
 * <p>Key design (composite primary key):
 * <ul>
 *   <li>Partition Key: userId - enables querying all plans for a user</li>
 *   <li>Sort Key: planId - unique plan identifier within partition</li>
 * </ul>
 * 
 * <p>Development: Uses LocalStack (http://localhost:4566)
 * <p>Production: Uses AWS DynamoDB
 * 
 * <p>Trade-offs vs MongoDB:
 * <ul>
 *   <li>Pro: Serverless, auto-scaling, AWS ecosystem integration</li>
 *   <li>Con: AWS lock-in, limited query flexibility, partition key design required</li>
 * </ul>
 */
@Repository
@Slf4j
@ConditionalOnProperty(name = "storage.nosql.provider", havingValue = "dynamodb")
public class DynamoDbTravelPlanStore implements TravelPlanStore {

  private final DynamoDbTable<TravelPlan> table;

  /**
   * Constructor that initializes the DynamoDB table reference.
   *
   * @param enhancedClient the DynamoDB enhanced client
   */
  public DynamoDbTravelPlanStore(DynamoDbEnhancedClient enhancedClient) {
    this.table = enhancedClient.table("TravelPlans", TableSchema.fromBean(TravelPlan.class));
    log.info("DynamoDbTravelPlanStore initialized with table: TravelPlans");
  }

  @Override
  public TravelPlan save(TravelPlan travelPlan) {
    log.debug("Saving travel plan to DynamoDB: userId={}, planId={}", 
        travelPlan.getUserId(), travelPlan.getPlanId());
    
    // Set timestamps
    travelPlan.setUpdatedAt(Instant.now());
    if (travelPlan.getCreatedAt() == null) {
      travelPlan.setCreatedAt(Instant.now());
    }
    
    table.putItem(travelPlan);
    return travelPlan;
  }

  @Override
  public Optional<TravelPlan> findByUserIdAndPlanId(String userId, String planId) {
    log.debug("Finding travel plan in DynamoDB: userId={}, planId={}", userId, planId);
    Key key = Key.builder()
        .partitionValue(userId)
        .sortValue(planId)
        .build();
    
    TravelPlan plan = table.getItem(key);
    return Optional.ofNullable(plan);
  }

  @Override
  public List<TravelPlan> findByUserId(String userId) {
    log.debug("Finding all travel plans for user in DynamoDB: userId={}", userId);
    QueryConditional queryConditional = QueryConditional
        .keyEqualTo(Key.builder().partitionValue(userId).build());
    
    QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
        .queryConditional(queryConditional)
        .build();
    
    return table.query(queryRequest)
        .stream()
        .flatMap(page -> page.items().stream())
        .toList();
  }

  @Override
  public boolean delete(String userId, String planId) {
    log.debug("Deleting travel plan from DynamoDB: userId={}, planId={}", userId, planId);
    Key key = Key.builder()
        .partitionValue(userId)
        .sortValue(planId)
        .build();
    
    TravelPlan deletedPlan = table.deleteItem(key);
    return deletedPlan != null;
  }

  @Override
  public boolean exists(String userId, String planId) {
    return findByUserIdAndPlanId(userId, planId).isPresent();
  }

  @Override
  public List<TravelPlan> findByUserIdAndProjectId(String userId, String projectId) {
    log.debug("Finding travel plans by project in DynamoDB: userId={}, projectId={}", 
        userId, projectId);
    // DynamoDB doesn't have native secondary index on projectId
    // Filter in memory after partition query
    return findByUserId(userId).stream()
        .filter(plan -> projectId.equals(plan.getProjectId()))
        .toList();
  }

  @Override
  public long countByUserId(String userId) {
    return findByUserId(userId).size();
  }

  @Override
  public long countByUserIdAndProjectId(String userId, String projectId) {
    return findByUserIdAndProjectId(userId, projectId).size();
  }

  @Override
  public String getProviderName() {
    return "DynamoDB";
  }

  @Override
  public boolean isHealthy() {
    try {
      // Check DynamoDB connection by getting table name
      table.tableName();
      return true;
    } catch (Exception e) {
      log.error("DynamoDB health check failed", e);
      return false;
    }
  }

  /**
   * Get the DynamoDB table reference.
   * This can be used for advanced operations not covered by standard methods.
   *
   * @return the DynamoDB table
   */
  public DynamoDbTable<TravelPlan> getTable() {
    return table;
  }
}

