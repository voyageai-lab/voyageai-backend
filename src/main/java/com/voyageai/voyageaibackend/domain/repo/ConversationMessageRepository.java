package com.voyageai.voyageaibackend.domain.repo;

import com.voyageai.voyageaibackend.domain.entity.ConversationMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for conversation messages (MySQL storage).
 * 
 * <p>This repository provides persistent storage for all conversation history.
 * It works alongside Redis for a dual-storage strategy:
 * <ul>
 *   <li>Recent messages (hot data): Stored in Redis for fast access</li>
 *   <li>All messages (cold + hot data): Stored in MySQL for long-term retention</li>
 * </ul>
 */
@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

  /**
   * Finds all messages for a project, ordered by creation time (oldest first).
   * 
   * <p>Used for:
   * - Building complete conversation context
   * - Restoring conversation history if Redis cache is lost
   * - Analytics and debugging
   *
   * @param projectId Project ID
   * @return List of messages in chronological order
   */
  List<ConversationMessage> findByProjectIdOrderByCreatedAtAsc(String projectId);

  /**
   * Finds the most recent N messages for a project (newest first).
   * 
   * <p>Used for:
   * - Warming up Redis cache on startup
   * - Quick conversation context retrieval
   * - Fallback when Redis is unavailable
   * 
   * <p>Note: JPA doesn't support LIMIT directly, so we use a custom query.
   *
   * @param projectId Project ID
   * @param limit Maximum number of messages to return
   * @return List of recent messages (newest first)
   */
  @Query(value = "SELECT * FROM conversation_messages "
      + "WHERE project_id = :projectId "
      + "ORDER BY created_at DESC "
      + "LIMIT :limit",
      nativeQuery = true)
  List<ConversationMessage> findRecentMessagesByProjectId(
      @Param("projectId") String projectId,
      @Param("limit") int limit
  );

  /**
   * Counts total messages in a project.
   * 
   * <p>Used for analytics and monitoring.
   *
   * @param projectId Project ID
   * @return Total message count
   */
  long countByProjectId(String projectId);

  /**
   * Deletes all messages for a project (GDPR compliance).
   * 
   * <p>Called when a user deletes their project or account.
   *
   * @param projectId Project ID
   */
  void deleteByProjectId(String projectId);
}

