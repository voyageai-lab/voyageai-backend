package com.voyageai.voyageaibackend.web.controller;

import com.voyageai.voyageaibackend.domain.repo.TravelPlanStore;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for health check endpoints.
 * Provides application health status information and database connectivity checks.
 * 
 * <p>Supports both MongoDB and DynamoDB as NoSQL providers, configured via:
 * <pre>
 * storage.nosql.provider=mongodb  # or dynamodb
 * </pre>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

  @PersistenceContext
  private EntityManager entityManager;

  private final TravelPlanStore travelPlanStore;
  private final RedisConnectionFactory redisConnectionFactory;
  
  @Value("${storage.nosql.provider:mongodb}")
  private String nosqlProvider;

  /**
   * Constructor for dependency injection.
   *
   * @param travelPlanStore the travel plan store (MongoDB or DynamoDB)
   * @param redisConnectionFactory the Redis connection factory
   */
  public HealthController(TravelPlanStore travelPlanStore, 
                         RedisConnectionFactory redisConnectionFactory) {
    this.travelPlanStore = travelPlanStore;
    this.redisConnectionFactory = redisConnectionFactory;
  }

  /**
   * Basic health check endpoint.
   * Returns application status and timestamp.
   *
   * @return health status response
   */
  @GetMapping
  public ResponseEntity<Map<String, Object>> healthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", Instant.now());
    response.put("service", "voyageai-backend");
    response.put("version", "1.0.0");
    response.put("nosqlProvider", travelPlanStore.getProviderName());
    
    return ResponseEntity.ok(response);
  }

  /**
   * Database health check endpoint (alias for MySQL).
   * Tests connectivity to MySQL database by executing a simple query.
   *
   * @return MySQL connection status
   */
  @GetMapping({"/db", "/mysql"})
  public ResponseEntity<Map<String, Object>> databaseHealthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", Instant.now());
    response.put("database", "MySQL");
    
    try {
      // Execute a simple native query to test connection
      Object result = entityManager
          .createNativeQuery("SELECT 1")
          .getSingleResult();
      
      response.put("status", "UP");
      response.put("message", "MySQL connection successful");
      response.put("details", Map.of(
          "queryResult", result,
          "connectionValid", true
      ));
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("status", "DOWN");
      response.put("message", "MySQL connection failed");
      response.put("error", e.getMessage());
      
      return ResponseEntity.status(503).body(response);
    }
  }

  /**
   * Redis health check endpoint.
   * Tests connectivity to Redis by executing a PING command.
   *
   * @return Redis connection status
   */
  @GetMapping("/redis")
  public ResponseEntity<Map<String, Object>> redisHealthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", Instant.now());
    response.put("database", "Redis");
    
    try {
      // Test Redis connection with PING command
      String pong = redisConnectionFactory.getConnection().ping();
      
      response.put("status", "UP");
      response.put("message", "Redis connection successful");
      response.put("details", Map.of(
          "pingResponse", pong,
          "connectionValid", true
      ));
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      response.put("status", "DOWN");
      response.put("message", "Redis connection failed");
      response.put("error", e.getMessage());
      
      return ResponseEntity.status(503).body(response);
    }
  }

  /**
   * NoSQL storage health check endpoint.
   * Tests connectivity to the configured NoSQL provider (MongoDB or DynamoDB).
   *
   * @return NoSQL storage connection status
   */
  @GetMapping({"/nosql", "/mongodb", "/dynamodb"})
  public ResponseEntity<Map<String, Object>> nosqlHealthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", Instant.now());
    response.put("database", travelPlanStore.getProviderName());
    response.put("provider", nosqlProvider);
    
    try {
      boolean healthy = travelPlanStore.isHealthy();
      
      if (healthy) {
        response.put("status", "UP");
        response.put("message", travelPlanStore.getProviderName() + " connection successful");
        response.put("details", Map.of(
            "provider", travelPlanStore.getProviderName(),
            "connectionValid", true
        ));
        return ResponseEntity.ok(response);
      } else {
        response.put("status", "DOWN");
        response.put("message", travelPlanStore.getProviderName() + " connection failed");
        return ResponseEntity.status(503).body(response);
      }
    } catch (Exception e) {
      response.put("status", "DOWN");
      response.put("message", travelPlanStore.getProviderName() + " connection failed");
      response.put("error", e.getMessage());
      
      return ResponseEntity.status(503).body(response);
    }
  }

  /**
   * Combined health check endpoint.
   * Tests connectivity to MySQL, Redis, and NoSQL storage (MongoDB or DynamoDB).
   *
   * @return combined health status for all databases
   */
  @GetMapping("/all")
  public ResponseEntity<Map<String, Object>> allHealthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", Instant.now());
    response.put("service", "voyageai-backend");
    response.put("nosqlProvider", travelPlanStore.getProviderName());
    
    // Check MySQL
    boolean mysqlUp = false;
    Map<String, Object> mysqlStatus = new HashMap<>();
    try {
      entityManager.createNativeQuery("SELECT 1").getSingleResult();
      mysqlUp = true;
      mysqlStatus.put("status", "UP");
      mysqlStatus.put("message", "Connected");
    } catch (Exception e) {
      mysqlStatus.put("status", "DOWN");
      mysqlStatus.put("error", e.getMessage());
    }
    
    // Check Redis
    boolean redisUp = false;
    Map<String, Object> redisStatus = new HashMap<>();
    try {
      redisConnectionFactory.getConnection().ping();
      redisUp = true;
      redisStatus.put("status", "UP");
      redisStatus.put("message", "Connected");
    } catch (Exception e) {
      redisStatus.put("status", "DOWN");
      redisStatus.put("error", e.getMessage());
    }
    
    // Check NoSQL (MongoDB or DynamoDB)
    boolean nosqlUp = false;
    Map<String, Object> nosqlStatus = new HashMap<>();
    try {
      nosqlUp = travelPlanStore.isHealthy();
      nosqlStatus.put("status", nosqlUp ? "UP" : "DOWN");
      nosqlStatus.put("message", nosqlUp ? "Connected" : "Connection failed");
      nosqlStatus.put("provider", travelPlanStore.getProviderName());
    } catch (Exception e) {
      nosqlStatus.put("status", "DOWN");
      nosqlStatus.put("error", e.getMessage());
      nosqlStatus.put("provider", travelPlanStore.getProviderName());
    }
    
    // Overall status
    boolean allUp = mysqlUp && redisUp && nosqlUp;
    response.put("status", allUp ? "UP" : "DEGRADED");
    response.put("databases", Map.of(
        "mysql", mysqlStatus,
        "redis", redisStatus,
        "nosql", nosqlStatus
    ));
    
    return allUp 
        ? ResponseEntity.ok(response) 
        : ResponseEntity.status(503).body(response);
  }
}
