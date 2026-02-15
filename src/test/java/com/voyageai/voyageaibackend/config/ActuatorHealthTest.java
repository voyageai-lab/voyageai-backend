package com.voyageai.voyageaibackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for Spring Boot Actuator health endpoint.
 *
 * <p>Verifies:
 * - /actuator/health endpoint is accessible (returns 200 or 503)
 * - Returns a status field
 * - Custom Kafka health indicator is included
 *
 * <p>Note: In test environment, some dependencies (Kafka, Redis) may
 * be unavailable, so health may return 503 (Service Unavailable).
 * The key assertion is that the endpoint responds with valid JSON.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:actuatortest;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.data.mongodb.uri=mongodb://localhost:27017/voyageai-actuator-test",
    "spring.data.redis.database=3",
    "planning.mode=async",
    "management.endpoints.web.exposure.include=health,info",
    "management.endpoint.health.show-details=always",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class ActuatorHealthTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsStatusField() throws Exception {
        // Health endpoint may return 200 (UP) or 503 (DOWN) depending
        // on whether dependencies are available. Either is acceptable.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 503
                        : "Expected 200 or 503 but got " + status;
                })
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void healthEndpointIncludesKafkaComponent() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 200 || status == 503
                        : "Expected 200 or 503 but got " + status;
                })
                .andExpect(jsonPath("$.components.kafka").exists());
    }
}
