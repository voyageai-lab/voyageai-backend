package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.model.TravelPlan;
import com.voyageai.voyageaibackend.domain.model.TravelPlanDocument;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * MongoDB implementation of TravelPlanStore.
 * 
 * <p>This implementation provides platform-agnostic NoSQL storage for travel plans
 * using MongoDB. It is activated when {@code storage.nosql.provider=mongodb}.
 * 
 * <p>Benefits over DynamoDB:
 * <ul>
 *   <li>No AWS lock-in - runs on any infrastructure</li>
 *   <li>Rich query language with aggregation pipeline</li>
 *   <li>Flexible indexing (compound, text, geospatial)</li>
 *   <li>Native support in Chinese cloud providers (Aliyun, Tencent)</li>
 * </ul>
 * 
 * <p>Collection: {@code travel_plans}
 * <p>Index: compound index on (userId, planId) for efficient lookups
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "storage.nosql.provider", havingValue = "mongodb", matchIfMissing = true)
public class MongoDbTravelPlanStore implements TravelPlanStore {

  private final TravelPlanMongoRepository mongoRepository;
  private final MongoTemplate mongoTemplate;

  @Override
  public TravelPlan save(TravelPlan travelPlan) {
    log.debug("Saving travel plan to MongoDB: userId={}, planId={}", 
        travelPlan.getUserId(), travelPlan.getPlanId());
    
    // Set timestamps
    travelPlan.setUpdatedAt(Instant.now());
    if (travelPlan.getCreatedAt() == null) {
      travelPlan.setCreatedAt(Instant.now());
    }
    
    TravelPlanDocument document = TravelPlanDocument.fromTravelPlan(travelPlan);
    TravelPlanDocument saved = mongoRepository.save(document);
    return saved.toTravelPlan();
  }

  @Override
  public Optional<TravelPlan> findByUserIdAndPlanId(String userId, String planId) {
    log.debug("Finding travel plan in MongoDB: userId={}, planId={}", userId, planId);
    return mongoRepository.findByUserIdAndPlanId(userId, planId)
        .map(TravelPlanDocument::toTravelPlan);
  }

  @Override
  public List<TravelPlan> findByUserId(String userId) {
    log.debug("Finding all travel plans for user in MongoDB: userId={}", userId);
    return mongoRepository.findByUserId(userId).stream()
        .map(TravelPlanDocument::toTravelPlan)
        .toList();
  }

  @Override
  public boolean delete(String userId, String planId) {
    log.debug("Deleting travel plan from MongoDB: userId={}, planId={}", userId, planId);
    long deleted = mongoRepository.deleteByUserIdAndPlanId(userId, planId);
    return deleted > 0;
  }

  @Override
  public boolean exists(String userId, String planId) {
    return mongoRepository.existsByUserIdAndPlanId(userId, planId);
  }

  @Override
  public List<TravelPlan> findByUserIdAndProjectId(String userId, String projectId) {
    log.debug("Finding travel plans by project in MongoDB: userId={}, projectId={}", 
        userId, projectId);
    return mongoRepository.findByUserIdAndProjectId(userId, projectId).stream()
        .map(TravelPlanDocument::toTravelPlan)
        .toList();
  }

  @Override
  public long countByUserId(String userId) {
    return mongoRepository.countByUserId(userId);
  }

  @Override
  public long countByUserIdAndProjectId(String userId, String projectId) {
    return mongoRepository.countByUserIdAndProjectId(userId, projectId);
  }

  @Override
  public String getProviderName() {
    return "MongoDB";
  }

  @Override
  public boolean isHealthy() {
    try {
      // Check MongoDB connection by listing collection names
      mongoTemplate.getCollectionNames();
      return true;
    } catch (Exception e) {
      log.error("MongoDB health check failed", e);
      return false;
    }
  }
}

