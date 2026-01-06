package com.voyageai.voyageaibackend.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.voyageai.voyageaibackend.domain.repo.TravelPlanStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link HealthController} focusing on error scenarios.
 * These tests use mocks to simulate database failures.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerUnitTest {

  @Mock
  private EntityManager entityManager;

  @Mock
  private TravelPlanStore travelPlanStore;

  @Mock
  private Query query;

  @Mock
  private org.springframework.data.redis.connection.RedisConnectionFactory redisConnectionFactory;

  private HealthController healthController;

  @BeforeEach
  void setUp() {
    healthController = new HealthController(travelPlanStore, redisConnectionFactory);
    // Inject the mocked EntityManager and nosqlProvider
    try {
      java.lang.reflect.Field entityManagerField = HealthController.class
          .getDeclaredField("entityManager");
      entityManagerField.setAccessible(true);
      entityManagerField.set(healthController, entityManager);
      
      java.lang.reflect.Field nosqlProviderField = HealthController.class
          .getDeclaredField("nosqlProvider");
      nosqlProviderField.setAccessible(true);
      nosqlProviderField.set(healthController, "mongodb");
    } catch (Exception e) {
      throw new RuntimeException("Failed to inject fields", e);
    }
  }

  @Test
  void healthCheck_shouldReturnUpStatus() {
    // Given
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");
    
    // When
    ResponseEntity<Map<String, Object>> response = healthController.healthCheck();

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UP", response.getBody().get("status"));
    assertEquals("voyageai-backend", response.getBody().get("service"));
    assertEquals("1.0.0", response.getBody().get("version"));
    assertEquals("MongoDB", response.getBody().get("nosqlProvider"));
  }

  @Test
  void databaseHealthCheck_shouldReturnUp_whenConnectionSuccessful() {
    // Given
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(1);

    // When
    ResponseEntity<Map<String, Object>> response = healthController.databaseHealthCheck();

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UP", response.getBody().get("status"));
    assertEquals("MySQL", response.getBody().get("database"));
    assertEquals("MySQL connection successful", response.getBody().get("message"));
  }

  @Test
  void databaseHealthCheck_shouldReturnDown_whenConnectionFails() {
    // Given
    when(entityManager.createNativeQuery(anyString()))
        .thenThrow(new RuntimeException("Connection refused"));

    // When
    ResponseEntity<Map<String, Object>> response = healthController.databaseHealthCheck();

    // Then
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("DOWN", response.getBody().get("status"));
    assertEquals("MySQL connection failed", response.getBody().get("message"));
    assertEquals("Connection refused", response.getBody().get("error"));
  }

  @Test
  void nosqlHealthCheck_shouldReturnUp_whenConnectionSuccessful() {
    // Given
    when(travelPlanStore.isHealthy()).thenReturn(true);
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");

    // When
    ResponseEntity<Map<String, Object>> response = healthController.nosqlHealthCheck();

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UP", response.getBody().get("status"));
    assertEquals("MongoDB", response.getBody().get("database"));
  }

  @Test
  void nosqlHealthCheck_shouldReturnDown_whenConnectionFails() {
    // Given
    when(travelPlanStore.isHealthy()).thenReturn(false);
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");

    // When
    ResponseEntity<Map<String, Object>> response = healthController.nosqlHealthCheck();

    // Then
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("DOWN", response.getBody().get("status"));
  }

  @Test
  void nosqlHealthCheck_shouldReturnDown_whenExceptionThrown() {
    // Given
    when(travelPlanStore.isHealthy()).thenThrow(new RuntimeException("Connection timeout"));
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");

    // When
    ResponseEntity<Map<String, Object>> response = healthController.nosqlHealthCheck();

    // Then
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("DOWN", response.getBody().get("status"));
    assertEquals("Connection timeout", response.getBody().get("error"));
  }

  @Test
  void allHealthCheck_shouldReturnUp_whenAllDatabasesUp() {
    // Given
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(1);
    when(travelPlanStore.isHealthy()).thenReturn(true);
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");
    
    // Mock Redis connection
    org.springframework.data.redis.connection.RedisConnection mockRedisConnection = 
        org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
    when(redisConnectionFactory.getConnection()).thenReturn(mockRedisConnection);
    when(mockRedisConnection.ping()).thenReturn("PONG");

    // When
    ResponseEntity<Map<String, Object>> response = healthController.allHealthCheck();

    // Then
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("UP", response.getBody().get("status"));
    assertEquals("MongoDB", response.getBody().get("nosqlProvider"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> databases = (Map<String, Object>) response.getBody().get("databases");
    assertNotNull(databases);
    
    @SuppressWarnings("unchecked")
    Map<String, Object> mysqlStatus = (Map<String, Object>) databases.get("mysql");
    assertEquals("UP", mysqlStatus.get("status"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> nosqlStatus = (Map<String, Object>) databases.get("nosql");
    assertEquals("UP", nosqlStatus.get("status"));
    assertEquals("MongoDB", nosqlStatus.get("provider"));
  }

  @Test
  void allHealthCheck_shouldReturnDegraded_whenMySQLDown() {
    // Given
    when(entityManager.createNativeQuery(anyString()))
        .thenThrow(new RuntimeException("MySQL down"));
    when(travelPlanStore.isHealthy()).thenReturn(true);
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");
    
    // Mock Redis connection
    org.springframework.data.redis.connection.RedisConnection mockRedisConnection = 
        org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
    when(redisConnectionFactory.getConnection()).thenReturn(mockRedisConnection);
    when(mockRedisConnection.ping()).thenReturn("PONG");

    // When
    ResponseEntity<Map<String, Object>> response = healthController.allHealthCheck();

    // Then
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("DEGRADED", response.getBody().get("status"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> databases = (Map<String, Object>) response.getBody().get("databases");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> mysqlStatus = (Map<String, Object>) databases.get("mysql");
    assertEquals("DOWN", mysqlStatus.get("status"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> nosqlStatus = (Map<String, Object>) databases.get("nosql");
    assertEquals("UP", nosqlStatus.get("status"));
  }

  @Test
  void allHealthCheck_shouldReturnDegraded_whenNoSQLDown() {
    // Given
    when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(1);
    when(travelPlanStore.isHealthy()).thenReturn(false);
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");
    
    // Mock Redis connection
    org.springframework.data.redis.connection.RedisConnection mockRedisConnection = 
        org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
    when(redisConnectionFactory.getConnection()).thenReturn(mockRedisConnection);
    when(mockRedisConnection.ping()).thenReturn("PONG");

    // When
    ResponseEntity<Map<String, Object>> response = healthController.allHealthCheck();

    // Then
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("DEGRADED", response.getBody().get("status"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> databases = (Map<String, Object>) response.getBody().get("databases");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> mysqlStatus = (Map<String, Object>) databases.get("mysql");
    assertEquals("UP", mysqlStatus.get("status"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> nosqlStatus = (Map<String, Object>) databases.get("nosql");
    assertEquals("DOWN", nosqlStatus.get("status"));
  }

  @Test
  void allHealthCheck_shouldReturnDegraded_whenBothDatabasesDown() {
    // Given
    when(entityManager.createNativeQuery(anyString()))
        .thenThrow(new RuntimeException("MySQL down"));
    when(travelPlanStore.isHealthy()).thenReturn(false);
    when(travelPlanStore.getProviderName()).thenReturn("MongoDB");
    
    // Mock Redis connection
    org.springframework.data.redis.connection.RedisConnection mockRedisConnection = 
        org.mockito.Mockito.mock(org.springframework.data.redis.connection.RedisConnection.class);
    when(redisConnectionFactory.getConnection()).thenReturn(mockRedisConnection);
    when(mockRedisConnection.ping()).thenReturn("PONG");

    // When
    ResponseEntity<Map<String, Object>> response = healthController.allHealthCheck();

    // Then
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("DEGRADED", response.getBody().get("status"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> databases = (Map<String, Object>) response.getBody().get("databases");
    
    @SuppressWarnings("unchecked")
    Map<String, Object> mysqlStatus = (Map<String, Object>) databases.get("mysql");
    assertEquals("DOWN", mysqlStatus.get("status"));
    
    @SuppressWarnings("unchecked")
    Map<String, Object> nosqlStatus = (Map<String, Object>) databases.get("nosql");
    assertEquals("DOWN", nosqlStatus.get("status"));
  }
}
