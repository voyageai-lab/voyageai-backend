package com.voyageai.voyageaibackend.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link HealthController}.
 * Tests health check endpoints for MySQL, Redis, and NoSQL (MongoDB/DynamoDB).
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void healthCheck_success() throws Exception {
    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.service").value("voyageai-backend"))
        .andExpect(jsonPath("$.version").value("1.0.0"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.nosqlProvider").exists());
  }

  @Test
  void mysqlHealthCheck_shouldReturnUp() throws Exception {
    mockMvc.perform(get("/api/health/mysql"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.database").value("MySQL"))
        .andExpect(jsonPath("$.message").value("MySQL connection successful"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.details.connectionValid").value(true));
  }

  @Test
  void nosqlHealthCheck_shouldReturnStatus() throws Exception {
    // Tests /api/health/nosql, /api/health/mongodb, /api/health/dynamodb endpoints
    // These all map to the same NoSQL health check
    mockMvc.perform(get("/api/health/nosql"))
        .andExpect(jsonPath("$.database").exists())
        .andExpect(jsonPath("$.provider").exists())
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.status").exists());
  }

  @Test
  void allHealthCheck_shouldReturnCombinedStatus() throws Exception {
    mockMvc.perform(get("/api/health/all"))
        .andExpect(jsonPath("$.timestamp").exists())
        .andExpect(jsonPath("$.service").value("voyageai-backend"))
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.nosqlProvider").exists())
        .andExpect(jsonPath("$.databases.mysql").exists())
        .andExpect(jsonPath("$.databases.nosql").exists())
        .andExpect(jsonPath("$.databases.redis").exists())
        .andExpect(jsonPath("$.databases.mysql.status").exists())
        .andExpect(jsonPath("$.databases.nosql.status").exists())
        .andExpect(jsonPath("$.databases.nosql.provider").exists());
  }

  @Test
  void allHealthCheck_mysqlShouldBeUp() throws Exception {
    mockMvc.perform(get("/api/health/all"))
        .andExpect(jsonPath("$.databases.mysql.status").value("UP"))
        .andExpect(jsonPath("$.databases.mysql.message").value("Connected"));
  }
}
