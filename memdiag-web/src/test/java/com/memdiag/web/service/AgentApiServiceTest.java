package com.memdiag.web.service;

import com.google.gson.JsonObject;
import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentApiServiceTest {

    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private AgentClient agentClient;

    private AgentApiService agentApiService;

    @BeforeEach
    void setUp() {
        agentApiService = new AgentApiService(connectionManager);
    }

    private void setupAgentMock() {
        when(connectionManager.getAgentClient("test-id")).thenReturn(agentClient);
    }

    @Test
    void throwsExceptionWhenAgentNotFound() {
        when(connectionManager.getAgentClient("unknown")).thenReturn(null);
        
        assertThatThrownBy(() -> agentApiService.getAgentStatus("unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No agent connection found");
    }

    @Test
    void canGetAgentStatus() {
        setupAgentMock();
        JsonObject status = new JsonObject();
        when(agentClient.getAgentStatus()).thenReturn(status);
        
        assertThat(agentApiService.getAgentStatus("test-id")).isEqualTo(status);
        verify(agentClient).getAgentStatus();
    }

    @Test
    void canEnableAllocationTracking() {
        setupAgentMock();
        JsonObject result = new JsonObject();
        when(agentClient.enableAllocationTracking()).thenReturn(result);
        
        assertThat(agentApiService.enableAllocationTracking("test-id")).isEqualTo(result);
        verify(agentClient).enableAllocationTracking();
    }

    @Test
    void canGetHistogram() {
        setupAgentMock();
        HeapHistogram histogram = new HeapHistogram();
        when(agentClient.getHistogram(10)).thenReturn(histogram);
        
        assertThat(agentApiService.getHistogram("test-id", 10)).isEqualTo(histogram);
        verify(agentClient).getHistogram(10);
    }

    @Test
    void canGetThreadDump() {
        setupAgentMock();
        ThreadDump dump = new ThreadDump();
        when(agentClient.getThreadDump()).thenReturn(dump);
        
        assertThat(agentApiService.getThreadDump("test-id")).isEqualTo(dump);
        verify(agentClient).getThreadDump();
    }

    @Test
    void canGetDiagnosis() {
        setupAgentMock();
        DiagnosisResult result = DiagnosisResult.builder().build();
        when(agentClient.getDiagnosis()).thenReturn(result);
        
        assertThat(agentApiService.getDiagnosis("test-id")).isEqualTo(result);
        verify(agentClient).getDiagnosis();
    }
}
