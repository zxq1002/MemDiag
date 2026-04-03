package com.memdiag.web.service;

import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.util.JmxClient;
import com.memdiag.web.config.MemDiagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConnectionManagerTest {

    @Mock
    private MemDiagProperties properties;

    @Mock
    private JmxClient jmxClient;

    @Mock
    private AgentClient agentClient;

    private ConnectionManager connectionManager;

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager(properties);
    }

    @Test
    void canRegisterAndRetrieveJmxConnection() {
        connectionManager.registerJmxConnection("test-jmx", jmxClient);
        
        assertThat(connectionManager.getConnectionType("test-jmx")).isEqualTo(ConnectionManager.ConnectionType.JMX);
        assertThat(connectionManager.getJmxClient("test-jmx")).isEqualTo(jmxClient);
    }

    @Test
    void canRegisterAndRetrieveAgentConnection() {
        connectionManager.registerAgentConnection("test-agent", agentClient);
        
        assertThat(connectionManager.getConnectionType("test-agent")).isEqualTo(ConnectionManager.ConnectionType.AGENT);
        assertThat(connectionManager.getAgentClient("test-agent")).isEqualTo(agentClient);
    }

    @Test
    void canDisconnectConnection() {
        connectionManager.registerJmxConnection("test-jmx", jmxClient);
        assertThat(connectionManager.getJmxClient("test-jmx")).isNotNull();
        
        connectionManager.disconnect("test-jmx");
        
        assertThat(connectionManager.getJmxClient("test-jmx")).isNull();
        assertThat(connectionManager.getConnectionType("test-jmx")).isNull();
    }

    @Test
    void canGetConnections() {
        connectionManager.registerJmxConnection("c1", jmxClient);
        connectionManager.registerAgentConnection("c2", agentClient);
        
        Map<String, String> connections = connectionManager.getConnections();
        
        assertThat(connections).hasSize(2);
        assertThat(connections.get("c1")).isEqualTo("jmx:connected");
        assertThat(connections.get("c2")).isEqualTo("agent:connected");
    }

    @Test
    void canGetSnapshotManager() {
        assertThat(connectionManager.getSnapshotManager("test")).isNotNull();
        assertThat(connectionManager.getSnapshotManager("test")).isSameAs(connectionManager.getSnapshotManager("test"));
    }
}
