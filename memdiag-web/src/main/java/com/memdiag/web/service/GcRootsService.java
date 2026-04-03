package com.memdiag.web.service;

import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.heap.GcRootStats;
import org.springframework.stereotype.Service;

@Service
public class GcRootsService {

    private final ConnectionManager connectionManager;

    public GcRootsService(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public GcRootStats getGcRootStats(String id) {
        ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
        if (type == ConnectionManager.ConnectionType.AGENT) {
            AgentClient client = connectionManager.getAgentClient(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.getGcRootStats();
        } else {
            throw new UnsupportedOperationException("GC Roots analysis requires Agent mode");
        }
    }

    public boolean startGcRootTracking(String id) {
        ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
        if (type == ConnectionManager.ConnectionType.AGENT) {
            AgentClient client = connectionManager.getAgentClient(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.startGcRootTracking();
        } else {
            throw new UnsupportedOperationException("GC Roots tracking requires Agent mode");
        }
    }

    public boolean stopGcRootTracking(String id) {
        ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
        if (type == ConnectionManager.ConnectionType.AGENT) {
            AgentClient client = connectionManager.getAgentClient(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.stopGcRootTracking();
        } else {
            throw new UnsupportedOperationException("GC Roots tracking requires Agent mode");
        }
    }
}
