package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.model.ConversationMessage;
import com.voyageai.voyageaibackend.domain.repo.ConversationMessageRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing conversation history with dual-storage strategy.
 * 
 * <p>This service implements a Redis + MySQL dual-write pattern:
 * <ul>
 *   <li><b>Redis (hot storage)</b>: Last 50 messages per project, TTL 7 days</li>
 *   <li><b>MySQL (cold storage)</b>: All messages, permanent retention</li>
 * </ul>
 * 
 * <p>Read strategy:
 * <ol>
 *   <li>Try Redis first (fast, in-memory)</li>
 *   <li>If Redis miss or partial data, fallback to MySQL</li>
 *   <li>Warm up Redis cache with MySQL data if needed</li>
 * </ol>
 * 
 * <p>This pattern provides:
 * - Fast reads for active conversations (Redis, &lt;1ms latency)
 * - Complete history preservation (MySQL)
 * - Resilience (MySQL backup if Redis fails)
 * - Cost efficiency (only recent data in expensive Redis)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationHistoryService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ConversationMessageRepository repository;

  @Value("${conversation.history.max.messages:50}")
  private int maxMessagesInRedis;

  @Value("${conversation.history.ttl.days:7}")
  private int redisTtlDays;

  /**
   * Adds a message to conversation history (dual-write to Redis and MySQL).
   * 
   * <p>Write flow:
   * <pre>
   * 1. Write to MySQL (durable, transactional)
   * 2. Write to Redis (fast access)
   * 3. Trim Redis list to max size
   * 4. Set Redis TTL
   * </pre>
   *
   * @param projectId Project ID
   * @param message Message to add
   * @return Saved message with generated ID
   */
  @Transactional
  public ConversationMessage addMessage(String projectId, ConversationMessage message) {
    // Generate message ID if not present
    if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
      message.setMessageId("msg-" + UUID.randomUUID());
    }

    // Set timestamp if not present
    if (message.getTimestamp() == null) {
      message.setTimestamp(Instant.now());
    }

    // Ensure projectId is set
    message.setProjectId(projectId);

    try {
      // 1. Write to MySQL (primary storage, transactional)
      com.voyageai.voyageaibackend.domain.entity.ConversationMessage entity =
          com.voyageai.voyageaibackend.domain.entity.ConversationMessage.fromDomainModel(message);
      com.voyageai.voyageaibackend.domain.entity.ConversationMessage saved = 
          repository.save(entity);
      ConversationMessage savedMessage = saved.toDomainModel();

      // 2. Write to Redis (fast access cache)
      String redisKey = getRedisKey(projectId);
      redisTemplate.opsForList().rightPush(redisKey, savedMessage);

      // 3. Trim Redis list to max size (keep only recent messages)
      Long size = redisTemplate.opsForList().size(redisKey);
      if (size != null && size > maxMessagesInRedis) {
        redisTemplate.opsForList().trim(redisKey, -maxMessagesInRedis, -1);
      }

      // 4. Set/refresh Redis TTL
      redisTemplate.expire(redisKey, Duration.ofDays(redisTtlDays));

      log.info("Added message {} to project {} (dual-write successful)", 
          savedMessage.getMessageId(), projectId);
      return savedMessage;

    } catch (Exception e) {
      log.error("Failed to add message to project {}: {}", projectId, e.getMessage(), e);
      throw new RuntimeException("Failed to save conversation message", e);
    }
  }

  /**
   * Retrieves conversation history for a project.
   * 
   * <p>Read strategy:
   * <ol>
   *   <li>Try Redis first (fast)</li>
   *   <li>If Redis has enough data, return it</li>
   *   <li>If Redis miss/partial, load from MySQL and warm up Redis</li>
   * </ol>
   *
   * @param projectId Project ID
   * @param limit Maximum number of messages to return
   * @return List of messages (oldest first)
   */
  public List<ConversationMessage> getHistory(String projectId, int limit) {
    try {
      // 1. Try Redis first
      String redisKey = getRedisKey(projectId);
      Long size = redisTemplate.opsForList().size(redisKey);

      if (size != null && size > 0) {
        // Redis has data, calculate range
        long start = Math.max(0, size - limit);
        List<Object> redisObjects = redisTemplate.opsForList().range(redisKey, start, -1);

        if (redisObjects != null && !redisObjects.isEmpty()) {
          List<ConversationMessage> messages = redisObjects.stream()
              .map(obj -> (ConversationMessage) obj)
              .collect(Collectors.toList());

          log.debug("Retrieved {} messages from Redis for project {}", messages.size(), projectId);
          return messages;
        }
      }

      // 2. Redis miss - fallback to MySQL
      log.info("Redis miss for project {}, loading from MySQL", projectId);
      List<com.voyageai.voyageaibackend.domain.entity.ConversationMessage> entities =
          repository.findRecentMessagesByProjectId(projectId, limit);

      // Convert to domain models (reverse to get chronological order)
      List<ConversationMessage> messages = entities.stream()
          .map(com.voyageai.voyageaibackend.domain.entity.ConversationMessage::toDomainModel)
          .collect(Collectors.toList());
      java.util.Collections.reverse(messages); // Oldest first

      // 3. Warm up Redis cache
      if (!messages.isEmpty()) {
        warmUpRedisCache(projectId, messages);
      }

      return messages;

    } catch (Exception e) {
      log.error("Error retrieving history for project {}: {}", projectId, e.getMessage(), e);
      // Return empty list instead of failing
      return List.of();
    }
  }

  /**
   * Builds AI context string from recent conversation history.
   * 
   * <p>This method formats recent messages into a context string for AI prompts:
   * <pre>
   * USER: I want to visit Tokyo for 5 days
   * ASSISTANT: What's your budget and accommodation preference?
   * USER: Around $2000, I prefer local guesthouses
   * </pre>
   * 
   * <p>The context helps AI:
   * - Remember previous user preferences
   * - Provide consistent recommendations
   * - Handle iterative refinements
   *
   * @param projectId Project ID
   * @return Formatted context string for AI
   */
  public String buildContextForAi(String projectId) {
    return buildContextForAi(projectId, 10);
  }

  /**
   * Builds AI context string with custom message limit.
   *
   * @param projectId Project ID
   * @param maxMessages Maximum number of recent messages to include
   * @return Formatted context string for AI
   */
  public String buildContextForAi(String projectId, int maxMessages) {
    List<ConversationMessage> recentMessages = getHistory(projectId, maxMessages);

    if (recentMessages.isEmpty()) {
      return "";
    }

    return recentMessages.stream()
        .filter(msg -> msg.getRole() != ConversationMessage.Role.SYSTEM) // Exclude system messages
        .map(msg -> String.format("%s: %s", msg.getRole(), msg.getContent()))
        .collect(Collectors.joining("\n"));
  }

  /**
   * Gets total message count for a project.
   *
   * @param projectId Project ID
   * @return Total message count
   */
  public long getMessageCount(String projectId) {
    return repository.countByProjectId(projectId);
  }

  /**
   * Deletes all messages for a project (GDPR compliance).
   * 
   * <p>Clears both Redis and MySQL storage.
   *
   * @param projectId Project ID
   */
  @Transactional
  public void deleteProjectHistory(String projectId) {
    try {
      // Delete from Redis
      String redisKey = getRedisKey(projectId);
      redisTemplate.delete(redisKey);

      // Delete from MySQL
      repository.deleteByProjectId(projectId);

      log.info("Deleted all conversation history for project {}", projectId);

    } catch (Exception e) {
      log.error("Error deleting history for project {}: {}", projectId, e.getMessage(), e);
      throw new RuntimeException("Failed to delete conversation history", e);
    }
  }

  /**
   * Warms up Redis cache with messages from MySQL.
   *
   * @param projectId Project ID
   * @param messages Messages to cache
   */
  private void warmUpRedisCache(String projectId, List<ConversationMessage> messages) {
    try {
      String redisKey = getRedisKey(projectId);
      
      // Clear existing Redis data
      redisTemplate.delete(redisKey);

      // Add all messages to Redis
      for (ConversationMessage message : messages) {
        redisTemplate.opsForList().rightPush(redisKey, message);
      }

      // Trim to max size
      if (messages.size() > maxMessagesInRedis) {
        redisTemplate.opsForList().trim(redisKey, -maxMessagesInRedis, -1);
      }

      // Set TTL
      redisTemplate.expire(redisKey, Duration.ofDays(redisTtlDays));

      log.info("Warmed up Redis cache for project {} with {} messages", 
          projectId, messages.size());

    } catch (Exception e) {
      log.warn("Failed to warm up Redis cache for project {}: {}", 
          projectId, e.getMessage());
      // Don't throw - cache warming is a best-effort optimization
    }
  }

  /**
   * Generates Redis key for project conversation history.
   *
   * @param projectId Project ID
   * @return Redis key
   */
  private String getRedisKey(String projectId) {
    return "conversation:" + projectId;
  }
}

