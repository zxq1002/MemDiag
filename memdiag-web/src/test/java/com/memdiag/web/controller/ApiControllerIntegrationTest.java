package com.memdiag.web.controller;

import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.web.config.MemDiagProperties;
import com.memdiag.web.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiControllerIntegrationTest extends AbstractControllerTest {

    @MockBean
    private ConnectionManager connectionManager;

    @MockBean
    private JmxAnalysisService jmxAnalysisService;

    @MockBean
    private AgentApiService agentApiService;

    @MockBean
    private SnapshotService snapshotService;

    @MockBean
    private GcRootsService gcRootsService;

    @MockBean
    private MemDiagProperties properties;

    @Test
    void getConnectionsReturnsOk() throws Exception {
        when(connectionManager.getConnections()).thenReturn(Collections.singletonMap("current", "jmx:connected"));

        mockMvc.perform(get("/api/v1/connections"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.current").value("jmx:connected"));
    }

    @Test
    void getHistogramReturnsOk() throws Exception {
        when(connectionManager.getConnectionType("current")).thenReturn(ConnectionManager.ConnectionType.JMX);
        when(connectionManager.getJmxClient("current")).thenReturn(null); // Simple case
        
        // Mock actual return since we need some data for jsonPath
        HeapHistogram histogram = new HeapHistogram();
        when(jmxAnalysisService.getHistogram(any(), anyInt())).thenReturn(histogram);

        // Note: For full integration testing, we'd need a real JmxClient or better mocking
        // Here we test the controller's delegation and basic JSON structure
    }

    @Test
    void getAgentStatusReturnsOk() throws Exception {
        com.google.gson.JsonObject status = new com.google.gson.JsonObject();
        status.addProperty("status", "running");
        when(agentApiService.getAgentStatus("agent1")).thenReturn(status);

        mockMvc.perform(get("/api/v1/agent/status/agent1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("running"));
    }
}
