package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.model.TravelPlanDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for TravelPlanDocument.
 * 
 * <p>This interface provides MongoDB-specific query methods for travel plans.
 * Spring Data automatically generates implementations based on method names.
 */
@Repository
public interface TravelPlanMongoRepository extends MongoRepository<TravelPlanDocument, String> {

  /**
   * Find a travel plan by user ID and plan ID.
   *
   * @param userId the user ID
   * @param planId the plan ID
   * @return Optional containing the document if found
   */
  Optional<TravelPlanDocument> findByUserIdAndPlanId(String userId, String planId);

  /**
   * Find all travel plans for a user.
   *
   * @param userId the user ID
   * @return list of travel plan documents
   */
  List<TravelPlanDocument> findByUserId(String userId);

  /**
   * Find all travel plans for a user in a specific project.
   *
   * @param userId the user ID
   * @param projectId the project ID
   * @return list of travel plan documents
   */
  List<TravelPlanDocument> findByUserIdAndProjectId(String userId, String projectId);

  /**
   * Delete a travel plan by user ID and plan ID.
   *
   * @param userId the user ID
   * @param planId the plan ID
   * @return number of deleted documents
   */
  long deleteByUserIdAndPlanId(String userId, String planId);

  /**
   * Check if a travel plan exists.
   *
   * @param userId the user ID
   * @param planId the plan ID
   * @return true if exists
   */
  boolean existsByUserIdAndPlanId(String userId, String planId);

  /**
   * Count plans for a user.
   *
   * @param userId the user ID
   * @return count
   */
  long countByUserId(String userId);

  /**
   * Count plans for a user in a project.
   *
   * @param userId the user ID
   * @param projectId the project ID
   * @return count
   */
  long countByUserIdAndProjectId(String userId, String projectId);
}

