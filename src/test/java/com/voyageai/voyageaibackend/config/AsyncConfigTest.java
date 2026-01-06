package com.voyageai.voyageaibackend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Unit tests for {@link AsyncConfig}.
 */
class AsyncConfigTest {

  @Test
  void taskExecutor_shouldReturnConfiguredExecutor() {
    // Given
    AsyncConfig config = new AsyncConfig();

    // When
    Executor executor = config.taskExecutor();

    // Then
    assertNotNull(executor);
    assertTrue(executor instanceof ThreadPoolTaskExecutor);
    
    ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
    assertEquals(5, threadPoolExecutor.getCorePoolSize());
    assertEquals(10, threadPoolExecutor.getMaxPoolSize());
    assertEquals(100, threadPoolExecutor.getQueueCapacity());
    assertEquals("async-planning-", threadPoolExecutor.getThreadNamePrefix());
  }

  @Test
  void taskExecutor_shouldBeInitialized() {
    // Given
    AsyncConfig config = new AsyncConfig();

    // When
    Executor executor = config.taskExecutor();

    // Then
    assertNotNull(executor);
    assertTrue(executor instanceof ThreadPoolTaskExecutor);
    
    // Verify the executor is active and can accept tasks
    ThreadPoolTaskExecutor threadPoolExecutor = (ThreadPoolTaskExecutor) executor;
    assertNotNull(threadPoolExecutor.getThreadPoolExecutor());
  }
}

