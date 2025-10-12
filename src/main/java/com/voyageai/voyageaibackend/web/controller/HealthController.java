package com.voyageai.voyageaibackend.web.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for health check endpoints.
 * Provides application health status information.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

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
    
    return ResponseEntity.ok(response);
  }
}

