package com.memdiag.core.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client for communicating with a running MemDiag agent via HTTP.
 */
public class AgentClient {

    /**
     * TypeAdapter for java.time.Instant to work with Gson.
     */
    private static class InstantTypeAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return Instant.parse(json.getAsString());
        }
    }

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6789;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .create();

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
            // Try parsing as wrapped format first {success: true, data: {...}}
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("success") && json.has("data")) {
                JsonElement data = json.get("data");
                return parseHeapHistogramFromMap(data);
            }
            // Fall back to direct format
            return gson.fromJson(raw, HeapHistogram.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse HeapHistogram from a JSON object that represents a map.
     */
    private HeapHistogram parseHeapHistogramFromMap(JsonElement data) {
        HeapHistogram histogram = new HeapHistogram();
        if (data.isJsonObject()) {
            JsonObject obj = data.getAsJsonObject();
            if (obj.has("classStats") && obj.get("classStats").isJsonArray()) {
                for (JsonElement elem : obj.get("classStats").getAsJsonArray()) {
                    if (elem.isJsonObject()) {
                        JsonObject statObj = elem.getAsJsonObject();
                        String className = statObj.has("className") ? statObj.get("className").getAsString() : null;
                        long objectCount = statObj.has("objectCount") ? statObj.get("objectCount").getAsLong() : 0;
                        long shallowBytes = statObj.has("shallowBytes") ? statObj.get("shallowBytes").getAsLong() : 0;
                        histogram.add(new com.memdiag.core.heap.ClassStats(className, objectCount, shallowBytes));
                    }
                }
            }
        }
        return histogram;
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
            // Try parsing as wrapped format first {success: true, data: {...}}
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("success") && json.has("data")) {
                JsonElement data = json.get("data");
                return parseThreadDumpFromMap(data);
            }
            // Fall back to direct format
            return gson.fromJson(raw, ThreadDump.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse ThreadDump from a JSON object that represents a map.
     */
    private ThreadDump parseThreadDumpFromMap(JsonElement data) {
        ThreadDump dump = new ThreadDump();
        if (data.isJsonObject()) {
            JsonObject obj = data.getAsJsonObject();
            if (obj.has("timestamp")) {
                dump.setTimestamp(Instant.parse(obj.get("timestamp").getAsString()));
            }
            if (obj.has("threadStats") && obj.get("threadStats").isJsonArray()) {
                for (JsonElement elem : obj.get("threadStats").getAsJsonArray()) {
                    if (elem.isJsonObject()) {
                        JsonObject statObj = elem.getAsJsonObject();
                        ThreadStats stats = new ThreadStats();
                        if (statObj.has("threadId")) stats.setThreadId(statObj.get("threadId").getAsLong());
                        if (statObj.has("threadName")) stats.setThreadName(statObj.get("threadName").getAsString());
                        if (statObj.has("state")) {
                            try {
                                stats.setState(com.memdiag.core.thread.ThreadState.valueOf(statObj.get("state").getAsString()));
                            } catch (Exception e) {}
                        }
                        if (statObj.has("blockedCount")) stats.setBlockedCount(statObj.get("blockedCount").getAsLong());
                        if (statObj.has("blockedTime")) stats.setBlockedTime(statObj.get("blockedTime").getAsLong());
                        if (statObj.has("waitedCount")) stats.setWaitedCount(statObj.get("waitedCount").getAsLong());
                        if (statObj.has("waitedTime")) stats.setWaitedTime(statObj.get("waitedTime").getAsLong());
                        if (statObj.has("stackTrace") && statObj.get("stackTrace").isJsonArray()) {
                            List<com.memdiag.core.thread.StackFrame> frames = new ArrayList<>();
                            for (JsonElement frameElem : statObj.get("stackTrace").getAsJsonArray()) {
                                if (frameElem.isJsonObject()) {
                                    JsonObject frameObj = frameElem.getAsJsonObject();
                                    com.memdiag.core.thread.StackFrame frame = new com.memdiag.core.thread.StackFrame();
                                    if (frameObj.has("className")) frame.setClassName(frameObj.get("className").getAsString());
                                    if (frameObj.has("methodName")) frame.setMethodName(frameObj.get("methodName").getAsString());
                                    if (frameObj.has("fileName")) frame.setFileName(frameObj.get("fileName").getAsString());
                                    if (frameObj.has("lineNumber")) frame.setLineNumber(frameObj.get("lineNumber").getAsInt());
                                    if (frameObj.has("nativeMethod")) frame.setNativeMethod(frameObj.get("nativeMethod").getAsBoolean());
                                    frames.add(frame);
                                }
                            }
                            stats.setStackTrace(frames);
                        }
                        dump.addThreadStats(stats);
                    }
                }
            }
        }
        return dump;
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
            // Try parsing as wrapped format first {success: true, data: {...}}
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            if (json.has("success") && json.has("data")) {
                JsonElement data = json.get("data");
                return parseDiagnosisFromMap(data);
            }
            // Fall back to direct format
            return gson.fromJson(raw, DiagnosisResult.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse DiagnosisResult from a JSON object that represents a map.
     */
    private DiagnosisResult parseDiagnosisFromMap(JsonElement data) {
        DiagnosisResult.Builder builder = DiagnosisResult.builder();
        if (data.isJsonObject()) {
            JsonObject obj = data.getAsJsonObject();
            if (obj.has("timestamp")) {
                builder.timestamp(Instant.parse(obj.get("timestamp").getAsString()));
            }
            if (obj.has("totalHeapUsed")) {
                builder.totalHeapUsed(obj.get("totalHeapUsed").getAsLong());
            }
            if (obj.has("totalHeapCommitted")) {
                builder.totalHeapCommitted(obj.get("totalHeapCommitted").getAsLong());
            }
            if (obj.has("threadCount")) {
                builder.threadCount(obj.get("threadCount").getAsInt());
            }
            if (obj.has("summary")) {
                builder.summary(obj.get("summary").getAsString());
            }
            if (obj.has("issues") && obj.get("issues").isJsonArray()) {
                for (JsonElement issueElem : obj.get("issues").getAsJsonArray()) {
                    if (issueElem.isJsonObject()) {
                        JsonObject issueObj = issueElem.getAsJsonObject();
                        com.memdiag.core.diagnose.Issue.Builder issueBuilder = com.memdiag.core.diagnose.Issue.builder();
                        if (issueObj.has("severity")) {
                            try {
                                issueBuilder.severity(com.memdiag.core.diagnose.Severity.valueOf(issueObj.get("severity").getAsString()));
                            } catch (Exception e) {}
                        }
                        if (issueObj.has("type")) issueBuilder.type(issueObj.get("type").getAsString());
                        if (issueObj.has("title")) issueBuilder.title(issueObj.get("title").getAsString());
                        if (issueObj.has("description")) issueBuilder.description(issueObj.get("description").getAsString());
                        if (issueObj.has("affectedClassName")) issueBuilder.affectedClassName(issueObj.get("affectedClassName").getAsString());
                        if (issueObj.has("affectedObjectCount") && !issueObj.get("affectedObjectCount").isJsonNull()) {
                            issueBuilder.affectedObjectCount(issueObj.get("affectedObjectCount").getAsLong());
                        }
                        if (issueObj.has("affectedBytes") && !issueObj.get("affectedBytes").isJsonNull()) {
                            issueBuilder.affectedBytes(issueObj.get("affectedBytes").getAsLong());
                        }
                        if (issueObj.has("recommendations") && issueObj.get("recommendations").isJsonArray()) {
                            for (JsonElement recElem : issueObj.get("recommendations").getAsJsonArray()) {
                                if (recElem.isJsonObject()) {
                                    JsonObject recObj = recElem.getAsJsonObject();
                                    com.memdiag.core.diagnose.Recommendation.Builder recBuilder = com.memdiag.core.diagnose.Recommendation.builder();
                                    if (recObj.has("priority")) recBuilder.priority(recObj.get("priority").getAsString());
                                    if (recObj.has("title")) recBuilder.title(recObj.get("title").getAsString());
                                    if (recObj.has("description")) recBuilder.description(recObj.get("description").getAsString());
                                    if (recObj.has("action")) recBuilder.action(recObj.get("action").getAsString());
                                    issueBuilder.addRecommendation(recBuilder.build());
                                }
                            }
                        }
                        builder.addIssue(issueBuilder.build());
                    }
                }
            }
        }
        return builder.build();
    }

}
