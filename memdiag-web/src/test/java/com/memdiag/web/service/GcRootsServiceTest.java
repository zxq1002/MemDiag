package com.memdiag.web.service;

import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.heap.GcRootStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GcRootsServiceTest {

    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private AgentClient agentClient;

    private GcRootsService gcRootsService;

    @BeforeEach
    void setUp() {
        gcRootsService = new GcRootsService(connectionManager);
    }

    @Test
    void canGetGcRootStatsForAgent() {
        when(connectionManager.getConnectionType("agent-id")).thenReturn(ConnectionManager.ConnectionType.AGENT);
        when(connectionManager.getAgentClient("agent-id")).thenReturn(agentClient);
        GcRootStats stats = new GcRootStats(new EnumMap<>(com.memdiag.core.heap.GcRootType.class));
        when(agentClient.getGcRootStats()).thenReturn(stats);

        GcRootStats result = gcRootsService.getGcRootStats("agent-id");

        assertThat(result).isSameAs(stats);
        verify(agentClient).getGcRootStats();
    }

    @Test
    void throwsExceptionForJmxStats() {
        when(connectionManager.getConnectionType("jmx-id")).thenReturn(ConnectionManager.ConnectionType.JMX);

        assertThatThrownBy(() -> gcRootsService.getGcRootStats("jmx-id"))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("Agent mode");
    }

    @Test
    void canStartStopTrackingForAgent() {
        when(connectionManager.getConnectionType("agent-id")).thenReturn(ConnectionManager.ConnectionType.AGENT);
        when(connectionManager.getAgentClient("agent-id")).thenReturn(agentClient);
        when(agentClient.startGcRootTracking()).thenReturn(true);
        when(agentClient.stopGcRootTracking()).thenReturn(true);

        assertThat(gcRootsService.startGcRootTracking("agent-id")).isTrue();
        assertThat(gcRootsService.stopGcRootTracking("agent-id")).isTrue();
        
        verify(agentClient).startGcRootTracking();
        verify(agentClient).stopGcRootTracking();
    }
}
