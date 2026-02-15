package com.voyageai.voyageaibackend.config;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom health indicator for Kafka broker connectivity.
 *
 * <p>Registered with Spring Boot Actuator at /actuator/health.
 * Reports UP if the Kafka cluster metadata can be retrieved,
 * DOWN otherwise.
 *
 * <p>This is critical for K8s readiness probes: if Kafka is down,
 * the Java backend should not receive traffic because it can't
 * dispatch planning requests to the Python worker.
 */
@Component
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private static final int TIMEOUT_SECONDS = 3;

    @Override
    public Health health() {
        try (AdminClient admin = AdminClient.create(
                Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers))) {

            DescribeClusterResult cluster = admin.describeCluster();
            String clusterId = cluster.clusterId()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            int nodeCount = cluster.nodes()
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).size();

            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodeCount", nodeCount)
                    .withDetail("bootstrapServers", bootstrapServers)
                    .build();

        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("bootstrapServers", bootstrapServers)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
