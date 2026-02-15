package com.voyageai.voyageaibackend.config;

import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.voyageai.voyageaibackend.web.controller.TaskStreamController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Graceful shutdown handler for VoyageAI backend.
 *
 * <p>On application shutdown (SIGTERM, Ctrl-C):
 * <ol>
 *   <li>Close all active SSE connections to notify frontends</li>
 *   <li>Spring Boot's graceful shutdown waits for in-flight requests</li>
 *   <li>Kafka consumers stop consuming (Spring Kafka lifecycle)</li>
 *   <li>Redis connections close (Spring Data lifecycle)</li>
 * </ol>
 *
 * <p>This is triggered by Spring's {@code ContextClosedEvent}, which fires
 * before Spring Boot's graceful shutdown waits for in-flight requests.
 *
 * <p>Combined with {@code server.shutdown=graceful} in application.properties,
 * this ensures no request is dropped and all SSE clients are notified.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GracefulShutdownConfig {

    private final TaskStreamController taskStreamController;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        log.info("Graceful shutdown initiated...");

        // Close all SSE connections
        int connections = taskStreamController.getActiveConnectionCount();
        if (connections > 0) {
            log.info("Closing {} active SSE connections...", connections);
            taskStreamController.closeAllConnections();
        }

        log.info("Graceful shutdown complete. Goodbye!");
    }
}
