package com.voyageai.voyageaibackend.service;

import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import com.voyageai.voyageaibackend.domain.model.PlanningTask.TaskStatus;
import com.voyageai.voyageaibackend.web.controller.TaskStreamController;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Production-ready task service using Redis for distributed storage.
 * 
 * <p>This service replaces the in-memory TaskService to support:
 * <ul>
 *   <li>Horizontal scaling (multiple application instances)</li>
 *   <li>Application restarts without data loss</li>
 *   <li>Automatic task expiration (TTL)</li>
 *   <li>High availability and persistence</li>
 * </ul>
 * 
 * <p>Key design decisions:
 * <ul>
 *   <li>Redis key format: "task:{taskId}"</li>
 *   <li>TTL: 24 hours (configurable)</li>
 *   <li>Fallback to in-memory storage if Redis unavailable</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 */
@Service
@Slf4j
public class RedisTaskService {

  private final RedisTemplate<String, PlanningTask> redisTemplate;
  private final TaskStreamController taskStreamController;
  
  @Value("${task.ttl.hours:24}")
  private int taskTtlHours;
  
  // Fallback in-memory storage for development/testing
  private final ConcurrentMap<String, PlanningTask> fallbackStore = new ConcurrentHashMap<>();

  /**
   * Constructor with lazy injection of TaskStreamController to avoid circular dependency.
   *
   * @param redisTemplate Redis template
   * @param taskStreamController Task stream controller (lazy-loaded)
   */
  public RedisTaskService(
      RedisTemplate<String, PlanningTask> redisTemplate,
      @Lazy TaskStreamController taskStreamController) {
    this.redisTemplate = redisTemplate;
    this.taskStreamController = taskStreamController;
  }
  
  /**
   * Creates a new planning task and stores it in Redis.
   * 
   *
   * @param userId User ID who submitted the task
   * @param projectId Project ID this task belongs to
   * @param requirements User's travel requirements
   * @return Created task with generated task ID
   */
  public PlanningTask createTask(String userId, String projectId, String requirements) {
    String taskId = "task-" + UUID.randomUUID();
    
    PlanningTask task = PlanningTask.builder()
        .taskId(taskId)
        .userId(userId)
        .projectId(projectId)
        .requirements(requirements)
        .status(TaskStatus.PENDING)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
    
    try {
      // Store in Redis with TTL
      String redisKey = "task:" + taskId;
      redisTemplate.opsForValue().set(redisKey, task, Duration.ofHours(taskTtlHours));
      
      log.info("Created task {} in Redis (TTL: {}h)", taskId, taskTtlHours);
      return task;
      
    } catch (Exception e) {
      log.warn("Redis unavailable, using fallback storage for task {}", taskId, e);
      
      // Fallback to in-memory storage
      fallbackStore.put(taskId, task);
      return task;
    }
  }
  
  /**
   * Retrieves a task by its ID.
   * 
   *
   * @param taskId Task ID to retrieve
   * @return Optional containing the task if found
   */
  public Optional<PlanningTask> getTask(String taskId) {
    try {
      // Try Redis first
      String redisKey = "task:" + taskId;
      PlanningTask task = redisTemplate.opsForValue().get(redisKey);
      
      if (task != null) {
        log.debug("Retrieved task {} from Redis", taskId);
        return Optional.of(task);
      }
      
      // Fallback to in-memory storage
      PlanningTask fallbackTask = fallbackStore.get(taskId);
      if (fallbackTask != null) {
        log.debug("Retrieved task {} from fallback storage", taskId);
        return Optional.of(fallbackTask);
      }
      
      log.debug("Task {} not found in Redis or fallback storage", taskId);
      return Optional.empty();
      
    } catch (Exception e) {
      log.warn("Redis error, checking fallback storage for task {}", taskId, e);
      
      // Fallback to in-memory storage
      PlanningTask fallbackTask = fallbackStore.get(taskId);
      return Optional.ofNullable(fallbackTask);
    }
  }
  
  /**
   * Updates task status to PROCESSING.
   * 
   *
   * @param taskId Task ID to update
   */
  public void markProcessing(String taskId) {
    updateTask(taskId, task -> {
      task.setStatus(TaskStatus.PROCESSING);
      task.setUpdatedAt(Instant.now());
    });
  }

  /**
   * Updates task progress with message and percentage.
   * 
   * <p>This method is used during PROCESSING state to provide real-time
   * progress feedback to users through SSE streaming.
   * 
   * <p>Example usage:
   * <pre>
   * updateProgress(taskId, "正在分析您的需求...", 10);
   * updateProgress(taskId, "调用大语言模型生成行程...", 40);
   * updateProgress(taskId, "保存行程数据...", 80);
   * </pre>
   *
   * @param taskId Task ID to update
   * @param message Progress message for UI display
   * @param percent Progress percentage (0-100)
   */
  public void updateProgress(String taskId, String message, int percent) {
    updateTask(taskId, task -> {
      task.setProgressMessage(message);
      task.setProgressPercent(Math.min(100, Math.max(0, percent))); // Clamp to 0-100
      task.setUpdatedAt(Instant.now());
    });
  }
  
  /**
   * Marks task as completed with result.
   * 
   *
   * @param taskId Task ID to update
   * @param result Generated travel itinerary
   */
  public void markCompleted(String taskId, String result) {
    updateTask(taskId, task -> {
      task.setStatus(TaskStatus.COMPLETED);
      task.setResult(result);
      task.setProgressPercent(100);
      task.setCompletedAt(Instant.now());
      task.setUpdatedAt(Instant.now());
    });
  }

  /**
   * Marks task as completed with structured itinerary.
   * 
   * <p>This overloaded method is used for Chapter 5+ structured itinerary generation.
   *
   * @param taskId Task ID to update
   * @param structuredItinerary Structured itinerary with geographic coordinates
   * @param result JSON representation of itinerary (for legacy/fallback)
   */
  public void markCompleted(
      String taskId, 
      com.voyageai.voyageaibackend.domain.model.StructuredItinerary structuredItinerary,
      String result) {
    updateTask(taskId, task -> {
      task.setStatus(TaskStatus.COMPLETED);
      task.setStructuredItinerary(structuredItinerary);
      task.setResult(result);
      task.setProgressPercent(100);
      task.setProgressMessage("行程生成完成！");
      task.setCompletedAt(Instant.now());
      task.setUpdatedAt(Instant.now());
    });
  }
  
  /**
   * Marks task as failed with error message.
   * 
   *
   * @param taskId Task ID to update
   * @param errorMessage Error description
   */
  public void markFailed(String taskId, String errorMessage) {
    updateTask(taskId, task -> {
      task.setStatus(TaskStatus.FAILED);
      task.setErrorMessage(errorMessage);
      task.setCompletedAt(Instant.now());
      task.setUpdatedAt(Instant.now());
    });
  }

  /**
   * Cancels a task (only if it's PENDING or PROCESSING).
   * 
   * <p>This method allows users to cancel tasks they no longer need.
   * Cancelled tasks are marked with CANCELLED status and have a shorter TTL (1 hour).
   * 
   * <p>Only tasks in PENDING or PROCESSING states can be cancelled.
   * Completed or failed tasks cannot be cancelled.
   *
   * @param taskId Task ID to cancel
   * @return true if task was cancelled, false if task cannot be cancelled
   */
  public boolean cancelTask(String taskId) {
    try {
      String redisKey = "task:" + taskId;
      PlanningTask task = redisTemplate.opsForValue().get(redisKey);

      if (task == null) {
        task = fallbackStore.get(taskId);
      }

      if (task == null) {
        log.warn("Cannot cancel task {}: not found", taskId);
        return false;
      }

      // Only allow cancellation of PENDING or PROCESSING tasks
      if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.PROCESSING) {
        log.warn("Cannot cancel task {}: status is {}", taskId, task.getStatus());
        return false;
      }

      // Update task to CANCELLED
      updateTask(taskId, t -> {
        t.setStatus(TaskStatus.CANCELLED);
        t.setProgressMessage("任务已取消");
        t.setCompletedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
      });

      // Set shorter TTL for cancelled tasks (1 hour)
      redisTemplate.expire(redisKey, Duration.ofHours(1));

      log.info("Cancelled task: {}", taskId);
      return true;

    } catch (Exception e) {
      log.error("Error cancelling task {}: {}", taskId, e.getMessage(), e);
      return false;
    }
  }
  
  /**
   * Generic task update method that handles both Redis and fallback storage.
   * 
   * <p>After updating the task, this method triggers SSE notification
   * to push real-time updates to connected frontend clients.
   *
   * @param taskId Task ID to update
   * @param updater Function to modify the task
   */
  private void updateTask(String taskId, java.util.function.Consumer<PlanningTask> updater) {
    PlanningTask updatedTask = null;

    try {
      // Try Redis first
      String redisKey = "task:" + taskId;
      PlanningTask task = redisTemplate.opsForValue().get(redisKey);
      
      if (task != null) {
        updater.accept(task);
        redisTemplate.opsForValue().set(redisKey, task, Duration.ofHours(taskTtlHours));
        updatedTask = task;
        log.debug("Updated task {} in Redis", taskId);
      } else {
        // Fallback to in-memory storage
        PlanningTask fallbackTask = fallbackStore.get(taskId);
        if (fallbackTask != null) {
          updater.accept(fallbackTask);
          fallbackStore.put(taskId, fallbackTask);
          updatedTask = fallbackTask;
          log.debug("Updated task {} in fallback storage", taskId);
        } else {
          log.warn("Task {} not found for update", taskId);
        }
      }
      
    } catch (Exception e) {
      log.error("Failed to update task {}", taskId, e);
      
      // Fallback to in-memory storage
      PlanningTask fallbackTask = fallbackStore.get(taskId);
      if (fallbackTask != null) {
        updater.accept(fallbackTask);
        fallbackStore.put(taskId, fallbackTask);
        updatedTask = fallbackTask;
        log.debug("Updated task {} in fallback storage after Redis error", taskId);
      }
    }

    // Notify SSE clients of the update
    if (updatedTask != null) {
      try {
        log.info("Calling notifyTaskUpdate for task: {} with status: {}", 
            taskId, updatedTask.getStatus());
        taskStreamController.notifyTaskUpdate(taskId, updatedTask);
        log.info("notifyTaskUpdate completed for task: {}", taskId);
      } catch (Exception e) {
        log.error("Failed to send SSE notification for task {}: {}", taskId, e.getMessage(), e);
        // Don't fail the update if SSE notification fails
      }
    } else {
      log.warn("No updated task to notify for taskId: {}", taskId);
    }
  }
  
  /**
   * Gets the total number of tasks (for monitoring).
   * 
   *
   * @return Task count
   */
  public int getTaskCount() {
    try {
      // Count Redis keys
      long redisCount = redisTemplate.keys("task:*").size();
      int fallbackCount = fallbackStore.size();
      
      log.debug("Task count - Redis: {}, Fallback: {}", redisCount, fallbackCount);
      return (int) redisCount + fallbackCount;
      
    } catch (Exception e) {
      log.warn("Error counting tasks, returning fallback count only", e);
      return fallbackStore.size();
    }
  }
  
  /**
   * Removes a task (for testing/cleanup).
   * 
   *
   * @param taskId Task ID to remove
   */
  public void removeTask(String taskId) {
    try {
      // Remove from Redis
      String redisKey = "task:" + taskId;
      redisTemplate.delete(redisKey);
      
      // Remove from fallback storage
      fallbackStore.remove(taskId);
      
      log.debug("Removed task {}", taskId);
      
    } catch (Exception e) {
      log.warn("Error removing task {}, removing from fallback only", taskId, e);
      fallbackStore.remove(taskId);
    }
  }
  
  /**
   * Clears all tasks (for testing).
   */
  public void clearAllTasks() {
    try {
      // Clear Redis keys
      redisTemplate.delete(redisTemplate.keys("task:*"));
      
      // Clear fallback storage
      fallbackStore.clear();
      
      log.info("Cleared all tasks from Redis and fallback storage");
      
    } catch (Exception e) {
      log.warn("Error clearing tasks, clearing fallback only", e);
      fallbackStore.clear();
    }
  }
}
