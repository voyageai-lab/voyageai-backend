package com.voyageai.voyageaibackend.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Health", description = "Application health check endpoints")
public class HealthController {

  /**
   * Basic health check endpoint.
   * Returns application status and timestamp.
   *
   * @return health status response
   */
  @GetMapping
  @Operation(
      summary = "Health check",
      description = "Returns the application health status and current timestamp"
  )
  public ResponseEntity<Map<String, Object>> healthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "UP");
    response.put("timestamp", Instant.now());
    response.put("service", "voyageai-backend");
    response.put("version", "1.0.0");
    
    return ResponseEntity.ok(response);
  }
}

