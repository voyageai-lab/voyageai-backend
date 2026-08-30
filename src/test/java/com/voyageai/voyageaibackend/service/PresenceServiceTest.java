package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * Unit tests for {@link PresenceService}.
 */
@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ZSetOperations<String, String> zSetOperations;

  @InjectMocks
  private PresenceService presenceService;

  @Test
  void heartbeat_shouldAddToSortedSet() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

    presenceService.heartbeat("proj-123", 42L, "Alice", "https://avatar.url");

    verify(zSetOperations).add(
        eq("presence:proj-123"),
        eq("42|Alice|https://avatar.url"),
        anyDouble());
    verify(redisTemplate).expire(eq("presence:proj-123"), any(Duration.class));
  }

  @Test
  void heartbeat_nullAvatar_shouldEncodeEmpty() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

    presenceService.heartbeat("proj-123", 42L, "Alice", null);

    verify(zSetOperations).add(
        eq("presence:proj-123"),
        eq("42|Alice|"),
        anyDouble());
  }

  @Test
  void getOnlineUsers_shouldReturnDecodedUsers() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    // Simulate members in the sorted set
    Set<String> members = Set.of("42|Alice|https://avatar.url", "43|Bob|");
    when(zSetOperations.range("presence:proj-123", 0, -1)).thenReturn(members);
    // Simulate scores
    when(zSetOperations.score("presence:proj-123", "42|Alice|https://avatar.url"))
        .thenReturn((double) Instant.now().toEpochMilli());
    when(zSetOperations.score("presence:proj-123", "43|Bob|"))
        .thenReturn((double) Instant.now().toEpochMilli());

    List<Map<String, Object>> result = presenceService.getOnlineUsers("proj-123");

    assertEquals(2, result.size());
    // Verify cleanup of stale entries was called
    verify(zSetOperations).removeRangeByScore(eq("presence:proj-123"), eq(0.0), anyDouble());
  }

  @Test
  void getOnlineUsers_emptySet_shouldReturnEmpty() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    when(zSetOperations.range("presence:proj-123", 0, -1)).thenReturn(Set.of());

    List<Map<String, Object>> result = presenceService.getOnlineUsers("proj-123");

    assertTrue(result.isEmpty());
  }

  @Test
  void removeUser_shouldRemoveMatchingEntries() {
    when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    Set<String> members = Set.of("42|Alice|https://avatar.url", "43|Bob|");
    when(zSetOperations.range("presence:proj-123", 0, -1)).thenReturn(members);

    presenceService.removeUser("proj-123", 42L);

    verify(zSetOperations).remove("presence:proj-123", "42|Alice|https://avatar.url");
  }
}
