package com.voyageai.voyageaibackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Tests for per-user rate limiting on planning endpoints.
 *
 * <p>Verifies:
 * - Requests within limit are not rate-limited (pass through to handler)
 * - Requests over limit return 429 Too Many Requests
 *
 * <p>Uses a very low limit (2/minute) to easily trigger the limiter.
 * The actual handler may return 400/404, but the key check is that
 * the 4th request returns 429 from the rate limiter interceptor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:ratelimittest;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.data.mongodb.uri=mongodb://localhost:27017/voyageai-ratelimit-test",
    "spring.data.redis.database=2",
    "planning.mode=async",
    "rate.limit.requests-per-minute=2",
    "rate.limit.timeout-ms=0",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class RateLimitConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "rate-limit-user-pass")
    void requestsWithinLimitShouldNotReturn429() throws Exception {
        // First request should NOT be rate limited (may return any status except 429)
        mockMvc.perform(post("/api/planning/generate")
                        .contentType("application/json")
                        .content("{\"requirements\":\"test\",\"projectId\":\"p1\"}"))
                .andExpect(result -> {
                    int responseStatus = result.getResponse().getStatus();
                    assert responseStatus != 429
                        : "Should not be rate limited on first request, got 429";
                });
    }

    @Test
    @WithMockUser(username = "rate-limit-flood-user")
    void requestsOverLimitShouldReturn429() throws Exception {
        // Exhaust the rate limit (2 requests per minute)
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/planning/generate")
                    .contentType("application/json")
                    .content("{\"requirements\":\"test" + i + "\",\"projectId\":\"p1\"}"));
        }

        // 3rd request should be rate limited
        mockMvc.perform(post("/api/planning/generate")
                        .contentType("application/json")
                        .content("{\"requirements\":\"test-extra\",\"projectId\":\"p1\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").exists());
    }
}
