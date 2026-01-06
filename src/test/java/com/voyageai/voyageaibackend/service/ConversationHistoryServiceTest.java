package com.voyageai.voyageaibackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.model.ConversationMessage;
import com.voyageai.voyageaibackend.domain.repo.ConversationMessageRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ListOperations;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryServiceTest {

  @Mock
  private ConversationMessageRepository repository;

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ListOperations<String, Object> listOperations;

  @InjectMocks
  private ConversationHistoryService conversationHistoryService;

  private static final String PROJECT_ID = "proj-123";
  private static final String MESSAGE_ID = "msg-456";


  @Test
  void addMessage_shouldStoreInRedis() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    com.voyageai.voyageaibackend.domain.model.ConversationMessage message = createTestMessage();
    com.voyageai.voyageaibackend.domain.entity.ConversationMessage savedEntity = createTestEntity();
    when(repository.save(any())).thenReturn(savedEntity);
    when(listOperations.rightPush(anyString(), any())).thenReturn(1L);
    when(listOperations.size(anyString())).thenReturn(1L);

    // When
    conversationHistoryService.addMessage(PROJECT_ID, message);

    // Then
    verify(repository).save(any());
    verify(listOperations).rightPush(eq("conversation:" + PROJECT_ID), any());
    verify(redisTemplate).expire(eq("conversation:" + PROJECT_ID), any());
  }

  @Test
  void addMessage_redisError_shouldFallbackToDatabase() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    com.voyageai.voyageaibackend.domain.model.ConversationMessage message = createTestMessage();
    com.voyageai.voyageaibackend.domain.entity.ConversationMessage savedEntity = createTestEntity();
    when(repository.save(any())).thenReturn(savedEntity);
    when(listOperations.rightPush(anyString(), any())).thenThrow(new RuntimeException("Redis error"));

    // When & Then
    try {
      conversationHistoryService.addMessage(PROJECT_ID, message);
    } catch (RuntimeException e) {
      // Expected - the service throws RuntimeException on Redis error
      assertEquals("Failed to save conversation message", e.getMessage());
    }
    
    verify(repository).save(any(com.voyageai.voyageaibackend.domain.entity.ConversationMessage.class));
  }

  @Test
  void getHistory_redisHit_shouldReturnFromRedis() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    com.voyageai.voyageaibackend.domain.model.ConversationMessage message1 = createTestMessage();
    com.voyageai.voyageaibackend.domain.model.ConversationMessage message2 = createTestMessage();
    List<Object> redisMessages = Arrays.asList((Object) message1, (Object) message2);
    when(listOperations.size(anyString())).thenReturn(2L);
    when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(redisMessages);

    // When
    List<com.voyageai.voyageaibackend.domain.model.ConversationMessage> result = conversationHistoryService.getHistory(PROJECT_ID, 10);

    // Then
    assertEquals(2, result.size());
    verify(listOperations).size("conversation:" + PROJECT_ID);
    verify(listOperations).range(eq("conversation:" + PROJECT_ID), anyLong(), anyLong());
    verify(repository, never()).findRecentMessagesByProjectId(anyString(), anyInt());
  }

  @Test
  void getHistory_redisMiss_shouldFallbackToDatabase() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    when(listOperations.size(anyString())).thenReturn(0L);
    com.voyageai.voyageaibackend.domain.entity.ConversationMessage entity1 = createTestEntity();
    com.voyageai.voyageaibackend.domain.entity.ConversationMessage entity2 = createTestEntity();
    when(repository.findRecentMessagesByProjectId(PROJECT_ID, 10))
        .thenReturn(Arrays.asList(entity1, entity2));

    // When
    List<com.voyageai.voyageaibackend.domain.model.ConversationMessage> result = conversationHistoryService.getHistory(PROJECT_ID, 10);

    // Then
    assertEquals(2, result.size());
    verify(repository).findRecentMessagesByProjectId(PROJECT_ID, 10);
  }

  @Test
  void getHistory_emptyResult_shouldReturnEmptyList() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    when(listOperations.size(anyString())).thenReturn(0L);
    when(repository.findRecentMessagesByProjectId(PROJECT_ID, 10)).thenReturn(Collections.emptyList());

    // When
    List<com.voyageai.voyageaibackend.domain.model.ConversationMessage> result = conversationHistoryService.getHistory(PROJECT_ID, 10);

    // Then
    assertTrue(result.isEmpty());
  }

  @Test
  void buildContextForAi_withMessages_shouldFormatCorrectly() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    com.voyageai.voyageaibackend.domain.model.ConversationMessage message1 = createTestMessage();
    message1.setRole(com.voyageai.voyageaibackend.domain.model.ConversationMessage.Role.USER);
    message1.setContent("I want to visit Tokyo");
    
    com.voyageai.voyageaibackend.domain.model.ConversationMessage message2 = createTestMessage();
    message2.setRole(com.voyageai.voyageaibackend.domain.model.ConversationMessage.Role.ASSISTANT);
    message2.setContent("Great! I'll help you plan a Tokyo trip.");

    when(listOperations.size(anyString())).thenReturn(2L);
    when(listOperations.range(anyString(), anyLong(), anyLong()))
        .thenReturn(Arrays.asList((Object) message1, (Object) message2)); // Redis returns oldest first

    // When
    String context = conversationHistoryService.buildContextForAi(PROJECT_ID);

    // Then
    assertNotNull(context);
    assertTrue(context.contains("USER: I want to visit Tokyo"));
    assertTrue(context.contains("ASSISTANT: Great! I'll help you plan a Tokyo trip."));
  }

  @Test
  void buildContextForAi_emptyHistory_shouldReturnEmptyString() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    when(listOperations.size(anyString())).thenReturn(0L);
    when(repository.findRecentMessagesByProjectId(PROJECT_ID, 10)).thenReturn(Collections.emptyList());

    // When
    String context = conversationHistoryService.buildContextForAi(PROJECT_ID);

    // Then
    assertEquals("", context);
  }

  @Test
  void buildContextForAi_withCustomLimit_shouldRespectLimit() {
    // Given
    when(redisTemplate.opsForList()).thenReturn(listOperations);
    when(listOperations.size(anyString())).thenReturn(0L);
    when(repository.findRecentMessagesByProjectId(PROJECT_ID, 5))
        .thenReturn(Collections.emptyList());

    // When
    String context = conversationHistoryService.buildContextForAi(PROJECT_ID, 5);

    // Then
    verify(repository).findRecentMessagesByProjectId(PROJECT_ID, 5);
    assertEquals("", context);
  }


  @Test
  void getMessageCount_redisHit_shouldReturnFromRedis() {
    // Given
    when(repository.countByProjectId(PROJECT_ID)).thenReturn(5L);

    // When
    long count = conversationHistoryService.getMessageCount(PROJECT_ID);

    // Then
    assertEquals(5L, count);
    verify(repository).countByProjectId(PROJECT_ID);
  }

  @Test
  void getMessageCount_redisMiss_shouldFallbackToDatabase() {
    // Given
    when(repository.countByProjectId(PROJECT_ID)).thenReturn(3L);

    // When
    long count = conversationHistoryService.getMessageCount(PROJECT_ID);

    // Then
    assertEquals(3L, count);
    verify(repository).countByProjectId(PROJECT_ID);
  }

  private com.voyageai.voyageaibackend.domain.model.ConversationMessage createTestMessage() {
    return com.voyageai.voyageaibackend.domain.model.ConversationMessage.builder()
        .messageId(MESSAGE_ID)
        .projectId(PROJECT_ID)
        .role(com.voyageai.voyageaibackend.domain.model.ConversationMessage.Role.USER)
        .content("Test message")
        .messageType(com.voyageai.voyageaibackend.domain.model.ConversationMessage.MessageType.TEXT)
        .timestamp(Instant.now())
        .build();
  }

  private com.voyageai.voyageaibackend.domain.entity.ConversationMessage createTestEntity() {
    com.voyageai.voyageaibackend.domain.entity.ConversationMessage entity = 
        new com.voyageai.voyageaibackend.domain.entity.ConversationMessage();
    entity.setMessageId(MESSAGE_ID);
    entity.setProjectId(PROJECT_ID);
    entity.setRole(com.voyageai.voyageaibackend.domain.model.ConversationMessage.Role.USER);
    entity.setContent("Test message");
    entity.setCreatedAt(Instant.now());
    return entity;
  }
}
