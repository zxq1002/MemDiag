package com.memdiag.core.agent;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * Client for communicating with a running MemDiag agent via HTTP.
 */
public class AgentClient {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6789;
    private static final Gson gson = new Gson();

    private final String host;
    private final int port;

    public AgentClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public AgentClient(int port) {
        this(DEFAULT_HOST, port);
    }

    public AgentClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Check if the agent is reachable.
     *
     * @return true if the agent is reachable
     */
    public boolean isReachable() {
        try {
            URL url = URI.create(buildUrl("/api/v1/native/status")).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Get native memory status from the agent.
     *
     * @return The native status as a JsonObject, or null if request failed
     */
    public JsonObject getNativeStatus() {
        return get("/api/v1/native/status");
    }

    /**
     * Get native memory summary from the agent.
     *
     * @return The native summary as a JsonObject, or null if request failed
     */
    public JsonObject getNativeSummary() {
        return get("/api/v1/native/summary");
    }

    /**
     * Get native memory regions from the agent.
     *
     * @return The native regions as a JsonObject, or null if request failed
     */
    public JsonObject getNativeRegions() {
        return get("/api/v1/native/regions");
    }

    /**
     * Get native memory diagnosis from the agent.
     *
     * @return The native diagnosis as a JsonObject, or null if request failed
     */
    public JsonObject getNativeDiagnosis() {
        return get("/api/v1/native/diagnose");
    }

    /**
     * Request the agent to detach.
     *
     * @return true if the detach request was accepted
     */
    public boolean detach() {
        try {
            URL url = URI.create(buildUrl("/api/v1/detach")).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            return false;
        }
    }

    // ========== Phase 1: Agent API ==========

    /**
     * Get agent status.
     */
    public JsonObject getAgentStatus() {
        return get("/api/v1/agent/status");
    }

    /**
     * Get agent configuration.
     */
    public JsonObject getAgentConfig() {
        return get("/api/v1/agent/config");
    }

    /**
     * Get agent metrics.
     */
    public JsonObject getAgentMetrics() {
        return get("/api/v1/agent/metrics");
    }

    // ========== Phase 3: Allocations API ==========

    /**
     * Get recent allocation events.
     */
    public JsonObject getAllocationsRecent() {
        return get("/api/v1/allocations/recent");
    }

    /**
     * Get recent allocation events with limit.
     */
    public JsonObject getAllocationsRecent(int limit) {
        return get("/api/v1/allocations/recent?limit=" + limit);
    }

    /**
     * Get allocation statistics.
     */
    public JsonObject getAllocationsStats() {
        return get("/api/v1/allocations/stats");
    }

    /**
     * Get top allocation types.
     */
    public JsonObject getAllocationsTop() {
        return get("/api/v1/allocations/top");
    }

    /**
     * Get top allocation types with limit.
     */
    public JsonObject getAllocationsTop(int limit) {
        return get("/api/v1/allocations/top?limit=" + limit);
    }

    /**
     * Get allocation rate.
     */
    public JsonObject getAllocationsRate() {
        return get("/api/v1/allocations/rate");
    }

    /**
     * Get allocation summary.
     */
    public JsonObject getAllocationsSummary() {
        return get("/api/v1/allocations/summary");
    }

    /**
     * Get allocation summary as Map (for CLI compatibility).
     */
    public Map<String, Object> getAllocationsSummaryMap() {
        JsonObject json = getAllocationsSummary();
        if (json == null) {
            return null;
        }
        return gson.fromJson(json, Map.class);
    }

    // ========== Phase 3: Methods API ==========

    /**
     * Get method statistics.
     */
    public JsonObject getMethodsStats(int limit) {
        return get("/api/v1/methods/stats?limit=" + limit);
    }

    /**
     * Get method statistics as Map (for CLI compatibility).
     */
    public Map<String, Object> getMethodsStatsMap(int limit) {
        JsonObject json = getMethodsStats(limit);
        if (json == null) {
            return null;
        }
        return gson.fromJson(json, Map.class);
    }

    /**
     * Get slow methods.
     */
    public JsonObject getMethodsSlow(int limit, int thresholdMs) {
        return get("/api/v1/methods/slow?limit=" + limit + "&threshold=" + thresholdMs);
    }

    // ========== Phase 3: Instrumentation API ==========

    /**
     * Get instrumentation status.
     */
    public JsonObject getInstrumentationStatus() {
        return get("/api/v1/instrumentation/status");
    }

    /**
     * Enable allocation tracking.
     */
    public JsonObject enableAllocationTracking() {
        return post("/api/v1/instrumentation/allocation/enable");
    }

    /**
     * Disable allocation tracking.
     */
    public JsonObject disableAllocationTracking() {
        return post("/api/v1/instrumentation/allocation/disable");
    }

    /**
     * Enable method monitoring.
     */
    public JsonObject enableMethodMonitoring() {
        return post("/api/v1/instrumentation/methods/enable");
    }

    /**
     * Disable method monitoring.
     */
    public JsonObject disableMethodMonitoring() {
        return post("/api/v1/instrumentation/methods/disable");
    }

    // ========== Phase 4: JVMTI API ==========

    /**
     * Get JVMTI status.
     */
    public JsonObject getJvmtiStatus() {
        return get("/api/v1/jvmti/status");
    }

    /**
     * Get raw response as string.
     */
    public String getRaw(String path) {
        try {
            URL url = URI.create(buildUrl(path)).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Post raw data and get string response.
     */
    public String postRaw(String path, String body) {
        try {
            URL url = URI.create(buildUrl(path)).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (body != null && !body.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.getBytes("UTF-8"));
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } catch (IOException e) {
            return null;
        }
    }

    private JsonObject get(String path) {
        String raw = getRaw(path);
        if (raw == null) {
            return null;
        }
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private JsonObject post(String path) {
        String raw = postRaw(path, "");
        if (raw == null) {
            return null;
        }
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        try {
            URL url = URI.create(buildUrl(path)).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (body != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    String jsonBody = gson.toJson(body);
                    os.write(jsonBody.getBytes("UTF-8"));
                }
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return null;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return gson.fromJson(response.toString(), responseType);
            }
        } catch (IOException e) {
            return null;
        }
    }

    private String buildUrl(String path) {
        return "http://" + host + ":" + port + path;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    // ========== Legacy API compatibility ==========

    /**
     * Get heap histogram from agent (legacy endpoint).
     *
     * @param limit Maximum number of entries to return
     * @return HeapHistogram, or null if request failed
     */
    public HeapHistogram getHeapHistogram(int limit) {
        String raw = getRaw("/api/heap/histogram?limit=" + limit);
        if (raw == null) {
            return null;
        }
        try {
            return gson.fromJson(raw, HeapHistogram.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get heap histogram (alias for getHeapHistogram).
     *
     * @param limit Maximum number of entries to return
     * @return HeapHistogram, or null if request failed
     */
    public HeapHistogram getHistogram(int limit) {
        return getHeapHistogram(limit);
    }

    /**
     * Get thread dump from agent (legacy endpoint).
     *
     * @return ThreadDump, or null if request failed
     */
    public ThreadDump getThreadDump() {
        String raw = getRaw("/api/threads");
        if (raw == null) {
            return null;
        }
        try {
            return gson.fromJson(raw, ThreadDump.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get diagnosis from agent (legacy endpoint).
     *
     * @return DiagnosisResult, or null if request failed
     */
    public DiagnosisResult getDiagnosis() {
        String raw = getRaw("/api/diagnose");
        if (raw == null) {
            return null;
        }
        try {
            return gson.fromJson(raw, DiagnosisResult.class);
        } catch (Exception e) {
            return null;
        }
    }

}
