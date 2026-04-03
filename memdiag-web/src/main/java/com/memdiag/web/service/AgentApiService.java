package com.memdiag.web.service;

import com.google.gson.JsonObject;
import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.nmt.NmtSnapshot;
import com.memdiag.core.thread.ThreadDump;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AgentApiService {

    private final ConnectionManager connectionManager;

    public AgentApiService(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    private AgentClient getClient(String id) {
        AgentClient client = connectionManager.getAgentClient(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client;
    }

    // ========== Basic Agent API ==========

    public JsonObject getAgentStatus(String id) {
        return getClient(id).getAgentStatus();
    }

    public JsonObject getAgentConfig(String id) {
        return getClient(id).getAgentConfig();
    }

    public JsonObject getAgentMetrics(String id) {
        return getClient(id).getAgentMetrics();
    }

    public boolean detachAgent(String id) {
        return getClient(id).detach();
    }

    public JsonObject updateAgentConfig(String id, Map<String, Object> config) {
        // AgentClient currently doesn't have updateConfig, returning placeholder
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("error", "Config update not implemented yet");
        return result;
    }

    // ========== Native Memory API ==========

    public JsonObject getNativeStatus(String id) {
        return getClient(id).getNativeStatus();
    }

    public JsonObject getNativeSummary(String id) {
        return getClient(id).getNativeSummary();
    }

    public JsonObject getNativeRegions(String id) {
        return getClient(id).getNativeRegions();
    }

    public JsonObject getNativeDiagnosis(String id) {
        return getClient(id).getNativeDiagnosis();
    }

    // ========== Allocations API ==========

    public JsonObject getAllocationsRecent(String id, int limit) {
        return getClient(id).getAllocationsRecent(limit);
    }

    public JsonObject getAllocationsStats(String id) {
        return getClient(id).getAllocationsStats();
    }

    public JsonObject getAllocationsTop(String id, int limit) {
        return getClient(id).getAllocationsTop(limit);
    }

    public JsonObject getAllocationsRate(String id) {
        return getClient(id).getAllocationsRate();
    }

    public JsonObject getAllocationsSummary(String id) {
        return getClient(id).getAllocationsSummary();
    }

    // ========== Methods API ==========

    public JsonObject getMethodsStats(String id, int limit) {
        return getClient(id).getMethodsStats(limit);
    }

    public JsonObject getMethodsSlow(String id, int limit, int thresholdMs) {
        return getClient(id).getMethodsSlow(limit, thresholdMs);
    }

    // ========== Instrumentation API ==========

    public JsonObject getInstrumentationStatus(String id) {
        return getClient(id).getInstrumentationStatus();
    }

    public JsonObject enableAllocationTracking(String id) {
        return getClient(id).enableAllocationTracking();
    }

    public JsonObject disableAllocationTracking(String id) {
        return getClient(id).disableAllocationTracking();
    }

    public JsonObject enableMethodMonitoring(String id) {
        return getClient(id).enableMethodMonitoring();
    }

    public JsonObject disableMethodMonitoring(String id) {
        return getClient(id).disableMethodMonitoring();
    }

    // ========== JVMTI API ==========

    public JsonObject getJvmtiStatus(String id) {
        return getClient(id).getJvmtiStatus();
    }

    // ========== Core Analysis (Agent Implementation) ==========

    public HeapHistogram getHistogram(String id, int limit) {
        return getClient(id).getHistogram(limit);
    }

    public ThreadDump getThreadDump(String id) {
        return getClient(id).getThreadDump();
    }

    public DiagnosisResult getDiagnosis(String id) {
        return getClient(id).getDiagnosis();
    }

    public NmtSnapshot getNmtSnapshot(String id, boolean detail) {
        AgentClient client = getClient(id);
        JsonObject response = client.getNativeSummary();
        if (response == null) {
            return NmtSnapshot.builder().build();
        }

        JsonObject data = response;
        if (response.has("success") && response.has("data")) {
            data = response.getAsJsonObject("data");
        }

        // Implementation of mapping NativeMemorySummary to NmtSnapshot
        // For now returning empty/placeholder as per previous AnalysisService logic
        return NmtSnapshot.builder().build();
    }
}
