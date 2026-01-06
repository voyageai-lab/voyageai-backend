package com.voyageai.voyageaibackend.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for asynchronous task execution.
 * 
 * <p>This configuration enables @Async annotation support and defines a custom thread pool
 * for executing async tasks. Proper thread pool configuration is critical for:
 * <ul>
 *   <li>Preventing thread exhaustion under high load</li>
 *   <li>Controlling concurrent task execution</li>
 *   <li>Managing system resources efficiently</li>
 * </ul>
 * 
 * <p>Thread Pool Configuration:
 * <ul>
 *   <li><b>Core Pool Size (5)</b>: Minimum number of threads to keep alive</li>
 *   <li><b>Max Pool Size (10)</b>: Maximum number of threads allowed</li>
 *   <li><b>Queue Capacity (100)</b>: Number of tasks to queue when all threads are busy</li>
 * </ul>
 * 
 * <p>When a new task arrives:
 * <ol>
 *   <li>If threads &lt; corePoolSize, create new thread</li>
 *   <li>If threads &gt;= corePoolSize, add to queue</li>
 *   <li>If queue is full and threads &lt; maxPoolSize, create new thread</li>
 *   <li>If queue is full and threads &gt;= maxPoolSize, reject task</li>
 * </ol>
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * Configures the task executor for @Async methods.
   * 
   * <p>This executor is used by all @Async annotated methods unless
   * a different executor is specified explicitly.
   *
   * @return Configured ThreadPoolTaskExecutor
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // Core pool size: minimum threads to keep alive
    executor.setCorePoolSize(5);

    // Max pool size: maximum threads allowed
    executor.setMaxPoolSize(10);

    // Queue capacity: tasks to queue when all threads are busy
    executor.setQueueCapacity(100);

    // Thread name prefix (helps with debugging)
    executor.setThreadNamePrefix("async-planning-");

    // Wait for tasks to complete on shutdown
    executor.setWaitForTasksToCompleteOnShutdown(true);

    // Timeout for shutdown (seconds)
    executor.setAwaitTerminationSeconds(60);

    // Initialize the executor
    executor.initialize();

    return executor;
  }
}

