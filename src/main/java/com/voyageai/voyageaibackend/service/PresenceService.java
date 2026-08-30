package com.voyageai.voyageaibackend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for tracking user presence on projects using Redis.
 *
 * <p>Uses Redis sorted sets where the score is the last-seen timestamp.
 * Users are considered "online" if their last heartbeat was within 60 seconds.
 *
 * <p>Key format: {@code presence:{projectId}}
 * Member format: {@code {userId}:{displayName}:{avatarUrl}}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

  private static final Duration PRESENCE_TTL = Duration.ofSeconds(60);
  private static final String KEY_PREFIX = "presence:";

  private final StringRedisTemplate redisTemplate;

  /**
   * Records a heartbeat for a user on a project.
   * The user is considered "online" for PRESENCE_TTL seconds.
   *
   * @param projectId project UUID
   * @param userId    user ID
   * @param displayName user display name
   * @param avatarUrl user avatar URL (nullable)
   */
  public void heartbeat(String projectId, Long userId, String displayName, String avatarUrl) {
    String key = KEY_PREFIX + projectId;
    String member = encodeMember(userId, displayName, avatarUrl);
    double score = Instant.now().toEpochMilli();

    redisTemplate.opsForZSet().add(key, member, score);
    // Expire the entire key after 5 minutes of inactivity (cleanup)
    redisTemplate.expire(key, Duration.ofMinutes(5));

    log.debug("Heartbeat: user {} on project {}", userId, projectId);
  }

  /**
   * Gets all online users for a project (heartbeat within PRESENCE_TTL).
   *
   * @param projectId project UUID
   * @return list of online user info maps
   */
  public List<Map<String, Object>> getOnlineUsers(String projectId) {
    String key = KEY_PREFIX + projectId;
    double cutoff = Instant.now().minus(PRESENCE_TTL).toEpochMilli();

    // Remove stale entries
    redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);

    // Get remaining (online) members
    Set<String> members = redisTemplate.opsForZSet().range(key, 0, -1);
    if (members == null || members.isEmpty()) {
      return List.of();
    }

    List<Map<String, Object>> result = new ArrayList<>();
    for (String member : members) {
      Map<String, Object> user = decodeMember(member);
      if (user != null) {
        Double score = redisTemplate.opsForZSet().score(key, member);
        if (score != null) {
          user.put("lastSeen", Instant.ofEpochMilli(score.longValue()).toString());
        }
        result.add(user);
      }
    }

    return result;
  }

  /**
   * Removes a user from presence tracking (e.g., on disconnect).
   */
  public void removeUser(String projectId, Long userId) {
    String key = KEY_PREFIX + projectId;
    // Remove all entries for this user (pattern match)
    Set<String> members = redisTemplate.opsForZSet().range(key, 0, -1);
    if (members != null) {
      for (String member : members) {
        if (member.startsWith(userId + "|")) {
          redisTemplate.opsForZSet().remove(key, member);
        }
      }
    }
  }

  private String encodeMember(Long userId, String displayName, String avatarUrl) {
    // Use | as separator (safe for URLs and display names)
    return userId + "|" + (displayName != null ? displayName : "")
        + "|" + (avatarUrl != null ? avatarUrl : "");
  }

  private Map<String, Object> decodeMember(String member) {
    String[] parts = member.split("\\|", 3);
    if (parts.length < 1) return null;

    try {
      Long userId = Long.parseLong(parts[0]);
      String displayName = parts.length > 1 ? parts[1] : "";
      String avatarUrl = parts.length > 2 && !parts[2].isEmpty() ? parts[2] : null;

      Map<String, Object> result = new java.util.HashMap<>();
      result.put("userId", userId);
      result.put("displayName", displayName);
      result.put("avatarUrl", avatarUrl);
      return result;
    } catch (NumberFormatException e) {
      log.warn("Invalid presence member format: {}", member);
      return null;
    }
  }
}
