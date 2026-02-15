package com.voyageai.voyageaibackend.config;

import com.voyageai.voyageaibackend.web.controller.TaskStreamController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

/**
 * Unit tests for graceful shutdown handler.
 *
 * <p>Verifies:
 * - Active SSE connections are closed on shutdown
 * - No-op when no active connections exist
 */
@ExtendWith(MockitoExtension.class)
class GracefulShutdownConfigTest {

    @Mock
    private TaskStreamController taskStreamController;

    @InjectMocks
    private GracefulShutdownConfig shutdownConfig;

    @Test
    void shutdownClosesActiveSseConnections() {
        when(taskStreamController.getActiveConnectionCount()).thenReturn(5);

        shutdownConfig.onShutdown();

        verify(taskStreamController).closeAllConnections();
    }

    @Test
    void shutdownSkipsCloseWhenNoActiveConnections() {
        when(taskStreamController.getActiveConnectionCount()).thenReturn(0);

        shutdownConfig.onShutdown();

        verify(taskStreamController, never()).closeAllConnections();
    }
}
