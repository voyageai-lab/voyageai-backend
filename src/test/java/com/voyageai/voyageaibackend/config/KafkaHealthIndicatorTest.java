package com.voyageai.voyageaibackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for KafkaHealthIndicator.
 *
 * <p>Tests that the health indicator correctly reports:
 * - DOWN when Kafka is unreachable (no broker running in tests)
 * - Error details are included in the health response
 */
class KafkaHealthIndicatorTest {

    @Test
    void healthReturnsDownWhenKafkaUnavailable() {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator();
        ReflectionTestUtils.setField(indicator, "bootstrapServers", "localhost:19999");

        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertNotNull(health.getDetails().get("error"));
    }

    @Test
    void healthDetailsIncludeBootstrapServers() {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator();
        ReflectionTestUtils.setField(indicator, "bootstrapServers", "localhost:19998");

        Health health = indicator.health();

        // Will be DOWN because no Kafka running, but should still have details
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("localhost:19998", health.getDetails().get("bootstrapServers"));
    }
}
