package com.voyageai.voyageaibackend.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.voyageai.voyageaibackend.domain.model.PlanningTask;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for distributed task storage.
 * 
 * <p>This configuration sets up Redis for storing PlanningTask objects
 * with proper serialization and TTL support.
 * 
 * <p>Key features:
 * <ul>
 *   <li>JSON serialization for PlanningTask objects</li>
 *   <li>String keys for Redis operations</li>
 *   <li>Configurable TTL for automatic cleanup</li>
 *   <li>Connection pooling for performance</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

  @Value("${task.ttl.hours:24}")
  private int taskTtlHours;

  /**
   * Creates a custom ObjectMapper with JSR310 support for Java 8 time types.
   * This ObjectMapper is used for HTTP requests and general JSON processing.
   * 
   *
   * @return configured ObjectMapper
   */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  /**
   * Creates a Redis-specific ObjectMapper with type information for proper deserialization.
   * This ObjectMapper is only used for Redis serialization/deserialization.
   * 
   * 
   */
  @Bean("redisObjectMapper")
  public ObjectMapper redisObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    // Enable type information for proper deserialization in Redis only
    mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), 
        ObjectMapper.DefaultTyping.NON_FINAL, 
        JsonTypeInfo.As.PROPERTY);
    return mapper;
  }

  /**
   * Configures RedisTemplate for PlanningTask operations.
   * 
   * <p>Serialization strategy:
   * <ul>
   *   <li>Keys: String serializer (human-readable)</li>
   *   <li>Values: JSON serializer (supports complex objects)</li>
   * </ul>
   * 
   *
   * @param connectionFactory Redis connection factory
   * @return configured RedisTemplate
   */
  @Bean
  public RedisTemplate<String, PlanningTask> planningTaskRedisTemplate(
      RedisConnectionFactory connectionFactory,
      ObjectMapper redisObjectMapper) {
    
    RedisTemplate<String, PlanningTask> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    // Key serializer: String (for human-readable keys like "task:abc123")
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    
    // Value serializer: JSON with JSR310 support (for complex PlanningTask objects)
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
    
    // Enable transaction support
    template.setEnableTransactionSupport(true);
    
    template.afterPropertiesSet();
    return template;
  }

  /**
   * Configures RedisTemplate for generic object operations (conversation history, etc.).
   * 
   * <p>This template uses JSON serialization for flexibility with different data types.
   *
   * @param connectionFactory Redis connection factory
   * @return configured RedisTemplate for generic objects
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory connectionFactory,
      ObjectMapper redisObjectMapper) {
    
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    // Key serializer: String (for human-readable keys)
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    
    // Value serializer: JSON with JSR310 support (for flexible object storage)
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
    
    // Enable transaction support
    template.setEnableTransactionSupport(true);
    
    template.afterPropertiesSet();
    return template;
  }

  /**
   * Gets the configured task TTL duration.
   * 
   *
   * @return TTL duration in hours
   */
  public Duration getTaskTtlDuration() {
    return Duration.ofHours(taskTtlHours);
  }

  /**
   * Gets the task TTL in hours.
   * 
   *
   * @return TTL in hours
   */
  public int getTaskTtlHours() {
    return taskTtlHours;
  }
}
