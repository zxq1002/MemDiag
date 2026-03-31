package com.memdiag.agent;

import com.memdiag.agent.collect.AllocationEvent;
import com.memdiag.agent.collect.DataCollector;
import com.memdiag.agent.collect.StatsAggregator;
import com.memdiag.agent.instrument.InstrumentManager;
import com.memdiag.agent.instrument.MethodMonitorTransformer;
import com.memdiag.agent.jvmti.AgentJVMTILoader;
import com.memdiag.agent.jvmti.GcRootTracker;
import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.RuleRegistry;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.GcRootStats;
import com.memdiag.core.heap.GcRootType;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.nativeapi.MemoryRegion;
import com.memdiag.core.nativeapi.NativeDiagnosis;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzer;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzerFactory;
import com.memdiag.core.nativeapi.NativeMemorySummary;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;
import com.memdiag.core.util.JmxClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.StringReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent HTTP server - uses simple JSON generation to avoid Gson conflicts.
 */
public class AgentServer {

    private final String host;
    private final int port;
    private final Instrumentation instrumentation;
    private final NativeMemoryAnalyzer nativeAnalyzer;
    private final ThreadMXBean threadMXBean;

    private HttpServer server;
    private ExecutorService executor;
    private volatile boolean running = false;

    public AgentServer(String host, int port, Instrumentation instrumentation) {
        this.host = host;
        this.port = port;
        this.instrumentation = instrumentation;
        this.nativeAnalyzer = NativeMemoryAnalyzerFactory.getInstance();
        this.threadMXBean = ManagementFactory.getThreadMXBean();

        if (threadMXBean.isThreadCpuTimeSupported()) {
            threadMXBean.setThreadCpuTimeEnabled(true);
        }
        if (threadMXBean.isThreadContentionMonitoringSupported()) {
            threadMXBean.setThreadContentionMonitoringEnabled(true);
        }
    }

    public void start() throws Exception {
        if (running) {
            return;
        }

        executor = Executors.newFixedThreadPool(4);
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.setExecutor(executor);

        // Legacy endpoints for CLI AgentClient compatibility
        server.createContext("/api/heap/histogram", new HeapHistogramHandler());
        server.createContext("/api/threads", new ThreadsHandler());
        server.createContext("/api/diagnose", new DiagnoseHandler());

        // v1 API endpoints
        server.createContext("/api/v1/histogram", new HeapHistogramHandler());
        server.createContext("/api/v1/threads", new ThreadsHandler());
        server.createContext("/api/v1/diagnose", new DiagnoseHandler());
        server.createContext("/api/v1/gc-roots/stats", new GcRootsStatsHandler());
        server.createContext("/api/v1/snapshot", new SimpleHandler("snapshot"));
        server.createContext("/api/v1/detach", new DetachHandler());

        server.createContext("/api/v1/native/status", new NativeStatusHandler());
        server.createContext("/api/v1/native/summary", new NativeSummaryHandler());
        server.createContext("/api/v1/native/regions", new NativeRegionsHandler());
        server.createContext("/api/v1/native/diagnose", new NativeDiagnoseHandler());

        // New Phase 1 endpoints
        server.createContext("/api/v1/agent/status", new AgentStatusHandler());
        server.createContext("/status", new AgentStatusHandler()); // Alias
        server.createContext("/api/v1/agent/config", new AgentConfigHandler());
        server.createContext("/api/v1/agent/metrics", new AgentMetricsHandler());

        // New Phase 3 endpoints - allocation tracking
        server.createContext("/api/v1/allocations/recent", new AllocationsRecentHandler());
        server.createContext("/api/v1/allocations/stats", new AllocationsStatsHandler());
        server.createContext("/allocations", new AllocationsStatsHandler()); // Alias
        server.createContext("/api/v1/allocations/top", new AllocationsTopHandler());
        server.createContext("/api/v1/allocations/rate", new AllocationsRateHandler());
        server.createContext("/api/v1/allocations/summary", new AllocationsSummaryHandler());

        // New Phase 4 endpoints - JVMTI
        server.createContext("/api/v1/jvmti/status", new JVMTIStatusHandler());
        server.createContext("/api/v1/gc-roots/stats", new GcRootsStatsHandler());
        server.createContext("/api/v1/gc-roots/track/start", new StartGcRootTrackingHandler());
        server.createContext("/api/v1/gc-roots/track/stop", new StopGcRootTrackingHandler());

        // New Phase 3 endpoints - method monitoring
        server.createContext("/api/v1/methods/stats", new MethodsStatsHandler());
        server.createContext("/methods", new MethodsStatsHandler()); // Alias
        server.createContext("/api/v1/methods/slow", new MethodsSlowHandler());
        server.createContext("/api/v1/instrumentation/status", new InstrumentationStatusHandler());
        server.createContext("/api/v1/instrumentation/allocation/enable", new EnableAllocationTrackingHandler());
        server.createContext("/api/v1/instrumentation/allocation/disable", new DisableAllocationTrackingHandler());
        server.createContext("/api/v1/instrumentation/methods/enable", new EnableMethodMonitoringHandler());
        server.createContext("/api/v1/instrumentation/methods/disable", new DisableMethodMonitoringHandler());

        server.start();
        running = true;
        System.out.printf("[MemDiag] Agent server started on %s:%d%n", host, port);
    }

    public void stop() throws Exception {
        if (!running) {
            return;
        }

        running = false;
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdown();
        }
        System.out.println("[MemDiag] Agent server stopped");
    }

    public boolean isRunning() {
        return running;
    }

    // ========== Simple JSON utilities ==========

    private String escapeJson(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder();
        sb.append('\"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c <= 0x1F) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('\"');
        return sb.toString();
    }

    private String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return escapeJson((String) obj);
        if (obj instanceof Number) return obj.toString();
        if (obj instanceof Boolean) return obj.toString();
        if (obj instanceof Instant) return escapeJson(obj.toString());
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(escapeJson(String.valueOf(entry.getKey())));
                sb.append(':');
                sb.append(toJson(entry.getValue()));
            }
            sb.append('}');
            return sb.toString();
        }
        if (obj instanceof List) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object item : (List<?>) obj) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJson(item));
            }
            sb.append(']');
            return sb.toString();
        }
        // Default to string representation
        return escapeJson(String.valueOf(obj));
    }

    private String successResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        response.put("timestamp", System.currentTimeMillis());
        return toJson(response);
    }

    private String errorResponse(String error) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("timestamp", System.currentTimeMillis());
        return toJson(response);
    }

    private void sendJson(HttpExchange exchange, String json, int statusCode) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendSuccess(HttpExchange exchange, Object data) throws IOException {
        sendJson(exchange, successResponse(data), 200);
    }

    private void sendError(HttpExchange exchange, String error, int statusCode) throws IOException {
        sendJson(exchange, errorResponse(error), statusCode);
    }

    // ========== Handlers ==========

    private class SimpleHandler implements HttpHandler {
        private final String type;

        SimpleHandler(String type) {
            this.type = type;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("message", type + " endpoint placeholder");
            sendSuccess(exchange, response);
        }
    }

    private class DetachHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }

            try {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Detach request received");
                sendSuccess(exchange, response);

                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        // Correctly update state and shutdown components
                        MemDiagAgent.shutdownAgent(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> status = new HashMap<>();
                status.put("available", nativeAnalyzer.isAvailable());
                status.put("platform", nativeAnalyzer.getPlatform());
                status.put("requiresAgent", nativeAnalyzer.requiresAgent());
                status.put("agentAttached", nativeAnalyzer.isAgentAttached());
                sendSuccess(exchange, status);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeSummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NativeMemorySummary summary = nativeAnalyzer.getSummary();
                
                long directSize = summary.getDirectByteBufferSize();
                long jniSize = summary.getJniAllocatedSize();
                long codeCacheSize = summary.getCodeCacheSize();
                long metaspaceSize = 0;

                // Supplement missing data from JMX if possible
                try {
                    for (java.lang.management.MemoryPoolMXBean pool : java.lang.management.ManagementFactory.getMemoryPoolMXBeans()) {
                        String name = pool.getName();
                        if (name.contains("Metaspace") || name.contains("Compressed Class Space")) {
                            metaspaceSize += pool.getUsage().getUsed();
                        } else if (name.contains("Code Cache") || name.contains("CodeHeap")) {
                            if (codeCacheSize == 0) {
                                codeCacheSize += pool.getUsage().getUsed();
                            }
                        }
                    }

                    // Try to get Direct memory via PlatformManagedObject
                    if (directSize == 0) {
                        try {
                            ObjectName directName = new ObjectName("java.nio:type=BufferPool,name=direct");
                            directSize = (long) ManagementFactory.getPlatformMBeanServer().getAttribute(directName, "MemoryUsed");
                        } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    System.err.println("[MemDiag] Error fetching JMX metrics for native summary: " + e.getMessage());
                }

                Map<String, Object> data = new HashMap<>();
                data.put("totalResident", summary.getTotalResident());
                data.put("totalVirtual", summary.getTotalVirtual());
                data.put("directByteBufferSize", directSize);
                data.put("jniAllocatedSize", jniSize);
                data.put("threadStackSize", summary.getThreadStackSize());
                data.put("codeCacheSize", codeCacheSize);
                
                Map<String, Long> breakdown = new HashMap<>(summary.getBreakdownByCategory());
                if (!breakdown.containsKey("Metaspace") && metaspaceSize > 0) {
                    breakdown.put("Metaspace", metaspaceSize);
                }
                if (!breakdown.containsKey("Code Cache") && codeCacheSize > 0) {
                    breakdown.put("Code Cache", codeCacheSize);
                }
                if (!breakdown.containsKey("Direct Buffer") && directSize > 0) {
                    breakdown.put("Direct Buffer", directSize);
                }
                
                data.put("breakdownByCategory", breakdown);
                sendSuccess(exchange, data);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeRegionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<MemoryRegion> regions = nativeAnalyzer.getMemoryRegions();
                List<Map<String, Object>> regionData = new ArrayList<>();
                for (MemoryRegion region : regions) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("startAddress", region.getStartAddress());
                    r.put("endAddress", region.getEndAddress());
                    r.put("size", region.getSize());
                    r.put("residentSize", region.getResidentSize());
                    r.put("permissions", region.getPermissions());
                    r.put("mappingFile", region.getMappingFile());
                    r.put("regionType", region.getRegionType());
                    regionData.add(r);
                }
                sendSuccess(exchange, regionData);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeDiagnoseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NativeDiagnosis diagnosis = nativeAnalyzer.analyzeNativeLeaks();
                Map<String, Object> data = new HashMap<>();
                data.put("findings", diagnosis.getFindings());
                data.put("warnings", diagnosis.getWarnings());
                data.put("recommendations", diagnosis.getRecommendations());
                sendSuccess(exchange, data);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    // ========== Phase 1: New Agent API Handlers ==========

    private class AgentStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> status = new HashMap<>();

                if (AgentContext.isInitialized()) {
                    AgentContext ctx = AgentContext.getInstance();
                    status.put("state", ctx.getState().name());
                    status.put("uptimeMs", ctx.getUptimeMs());
                    status.put("startTime", ctx.getStartTime());
                    status.put("serverRunning", ctx.isServerRunning());
                    status.put("instrumentationEnabled", ctx.getConfig().isInstrumentationEnabled());
                    status.put("jvmtiEnabled", ctx.getConfig().isJvmtiEnabled());
                    status.put("hasDataCollector", ctx.getDataCollector() != null);
                    status.put("hasInstrumentManager", ctx.getInstrumentManager() != null);
                    status.put("hasJvmtiLoader", ctx.getJvmtiLoader() != null);
                } else {
                    status.put("state", "UNINITIALIZED");
                    status.put("message", "Agent context not initialized");
                }

                sendSuccess(exchange, status);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AgentConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();

                if ("GET".equals(method)) {
                    // Get current configuration
                    Map<String, Object> config;
                    if (AgentContext.isInitialized()) {
                        config = AgentContext.getInstance().getConfig().toMap();
                    } else {
                        config = AgentConfig.defaults().toMap();
                    }
                    sendSuccess(exchange, config);
                } else if ("PUT".equals(method)) {
                    // Update configuration (placeholder - requires restart for most changes)
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Configuration update requires agent restart");
                    sendSuccess(exchange, response);
                } else {
                    sendError(exchange, "Method not allowed", 405);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AgentMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> metrics = new HashMap<>();

                // JVM metrics
                Runtime runtime = Runtime.getRuntime();
                metrics.put("jvm.totalMemory", runtime.totalMemory());
                metrics.put("jvm.freeMemory", runtime.freeMemory());
                metrics.put("jvm.maxMemory", runtime.maxMemory());
                metrics.put("jvm.availableProcessors", runtime.availableProcessors());

                // Agent metrics
                if (AgentContext.isInitialized()) {
                    AgentContext ctx = AgentContext.getInstance();
                    metrics.put("agent.uptimeMs", ctx.getUptimeMs());
                    metrics.put("agent.state", ctx.getState().name());
                }

                sendSuccess(exchange, metrics);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    // ========== Phase 3: Allocation Tracking API Handlers ==========

    private abstract class BaseAllocationHandler implements HttpHandler {
        protected DataCollector getDataCollector() {
            if (AgentContext.isInitialized()) {
                return AgentContext.getInstance().getDataCollector();
            }
            return null;
        }

        protected StatsAggregator getStatsAggregator() {
            if (AgentContext.isInitialized()) {
                return AgentContext.getInstance().getStatsAggregator();
            }
            return null;
        }

        protected int getIntParam(HttpExchange exchange, String name, int defaultValue) {
            String query = exchange.getRequestURI().getQuery();
            if (query == null) {
                return defaultValue;
            }
            for (String param : query.split("&")) {
                String[] parts = param.split("=", 2);
                if (parts.length == 2 && parts[0].equals(name)) {
                    try {
                        return Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                }
            }
            return defaultValue;
        }
    }

    private class AllocationsRecentHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DataCollector collector = getDataCollector();
                if (collector != null) {
                    int limit = getIntParam(exchange, "limit", 100);
                    sendSuccess(exchange, collector.getRecentEvents(limit));
                } else {
                    sendError(exchange, "Data collector not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsStatsHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DataCollector collector = getDataCollector();
                if (collector != null) {
                    sendSuccess(exchange, collector.toMap());
                } else {
                    sendError(exchange, "Data collector not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsTopHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DataCollector collector = getDataCollector();
                if (collector != null) {
                    int limit = getIntParam(exchange, "limit", 10);
                    String type = exchange.getRequestURI().getQuery() != null &&
                            exchange.getRequestURI().getQuery().contains("type=count") ? "count" : "size";

                    if ("count".equals(type)) {
                        sendSuccess(exchange, collector.getTopTypesByCount(limit));
                    } else {
                        sendSuccess(exchange, collector.getTopTypesBySize(limit));
                    }
                } else {
                    sendError(exchange, "Data collector not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsRateHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                StatsAggregator aggregator = getStatsAggregator();
                if (aggregator != null) {
                    aggregator.takeSnapshot();
                    int windowSec = getIntParam(exchange, "window", 60);

                    Map<String, Object> data = new HashMap<>();
                    data.put("currentRateBytesPerSec", aggregator.getCurrentRateBytesPerSec());
                    data.put("trend", aggregator.getTrend().name());
                    data.put("windowRates", aggregator.getRateHistory(windowSec));
                    sendSuccess(exchange, data);
                } else {
                    sendError(exchange, "Stats aggregator not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsSummaryHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                StatsAggregator aggregator = getStatsAggregator();
                if (aggregator != null) {
                    sendSuccess(exchange, aggregator.getSummary());
                } else {
                    sendError(exchange, "Stats aggregator not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    // ========== Phase 4: JVMTI API Handlers ==========

    private class JVMTIStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> status;

                if (AgentContext.isInitialized()) {
                    AgentContext ctx = AgentContext.getInstance();
                    AgentJVMTILoader loader = ctx.getJvmtiLoader();

                    if (loader != null) {
                        status = loader.getStatus();
                    } else {
                        status = new HashMap<>();
                        status.put("enabled", ctx.getConfig().isJvmtiEnabled());
                        status.put("autoLoad", ctx.getConfig().isJvmtiAutoLoad());
                        status.put("loaded", false);
                        status.put("available", false);
                        status.put("error", "JVMTI loader not initialized");
                        status.put("platform", System.getProperty("os.name"));
                        status.put("arch", System.getProperty("os.arch"));
                    }
                } else {
                    status = new HashMap<>();
                    status.put("error", "Agent context not initialized");
                    sendError(exchange, "Agent context not initialized", 500);
                    return;
                }

                sendSuccess(exchange, status);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    // ========== Local JVM Analysis Handlers ==========

    private class HeapHistogramHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                int limit = getIntParam(exchange, "limit", 100);
                HeapHistogram histogram = getLocalHeapHistogram(limit);
                // Convert to map for simple JSON serialization
                Map<String, Object> result = new HashMap<>();
                List<Map<String, Object>> statsList = new ArrayList<>();
                for (ClassStats stats : histogram.getClassStats()) {
                    Map<String, Object> statMap = new HashMap<>();
                    statMap.put("className", stats.getClassName());
                    statMap.put("objectCount", stats.getObjectCount());
                    statMap.put("shallowBytes", stats.getShallowBytes());
                    statsList.add(statMap);
                }
                result.put("classStats", statsList);
                result.put("totalBytes", histogram.getTotalBytes());
                result.put("totalObjects", histogram.getTotalObjects());
                sendJson(exchange, toJson(result), 200);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }

        private HeapHistogram getLocalHeapHistogram(int limit) throws Exception {
            ObjectName diagnosticName = new ObjectName("com.sun.management:type=DiagnosticCommand");
            String result = (String) ManagementFactory.getPlatformMBeanServer().invoke(
                diagnosticName,
                "gcClassHistogram",
                new Object[]{null},
                new String[]{String[].class.getName()}
            );
            return parseClassHistogram(result, limit);
        }

        private HeapHistogram parseClassHistogram(String output, int limit) {
            HeapHistogram full = new HeapHistogram();
            HeapHistogram limited = new HeapHistogram();

            try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
                String line;
                boolean inDataSection = false;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("num")) {
                        inDataSection = true;
                        continue;
                    }
                    if (line.startsWith("-") || line.startsWith("Total")) {
                        continue;
                    }
                    if (!inDataSection || line.isEmpty()) {
                        continue;
                    }

                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        try {
                            long count = Long.parseLong(parts[1]);
                            long bytes = Long.parseLong(parts[2]);
                            StringBuilder className = new StringBuilder(parts[3]);
                            for (int i = 4; i < parts.length; i++) {
                                className.append(" ").append(parts[i]);
                            }
                            full.add(new ClassStats(className.toString(), count, bytes));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse class histogram", e);
            }

            full.getTopByShallowBytes(limit).forEach(limited::add);
            return limited;
        }
    }

    private class ThreadsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                ThreadDump dump = getLocalThreadDump();
                // Convert to map for simple JSON serialization
                Map<String, Object> result = new HashMap<>();
                result.put("threadCount", dump.getThreadStats() != null ? dump.getThreadStats().size() : 0);
                result.put("timestamp", dump.getTimestamp() != null ? dump.getTimestamp().toString() : null);
                List<Map<String, Object>> statsList = new ArrayList<>();
                if (dump.getThreadStats() != null) {
                    for (ThreadStats stats : dump.getThreadStats()) {
                        Map<String, Object> statMap = new HashMap<>();
                        statMap.put("threadId", stats.getThreadId());
                        statMap.put("threadName", stats.getThreadName());
                        statMap.put("state", stats.getState() != null ? stats.getState().name() : null);
                        statMap.put("blockedCount", stats.getBlockedCount());
                        statMap.put("blockedTime", stats.getBlockedTime());
                        statMap.put("waitedCount", stats.getWaitedCount());
                        statMap.put("waitedTime", stats.getWaitedTime());
                        if (stats.getStackTrace() != null) {
                            List<Map<String, Object>> stackList = new ArrayList<>();
                            for (com.memdiag.core.thread.StackFrame frame : stats.getStackTrace()) {
                                Map<String, Object> frameMap = new HashMap<>();
                                frameMap.put("className", frame.getClassName());
                                frameMap.put("methodName", frame.getMethodName());
                                frameMap.put("fileName", frame.getFileName());
                                frameMap.put("lineNumber", frame.getLineNumber());
                                frameMap.put("nativeMethod", frame.isNativeMethod());
                                stackList.add(frameMap);
                            }
                            statMap.put("stackTrace", stackList);
                        }
                        statsList.add(statMap);
                    }
                }
                result.put("threadStats", statsList);
                sendJson(exchange, toJson(result), 200);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }

        private ThreadDump getLocalThreadDump() {
            ThreadDump dump = new ThreadDump();
            dump.setTimestamp(java.time.Instant.now());

            long[] allThreadIds = threadMXBean.getAllThreadIds();
            for (long threadId : allThreadIds) {
                java.lang.management.ThreadInfo info = threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
                if (info != null) {
                    dump.addThreadStats(convertToThreadStatsWithStack(info));
                }
            }
            return dump;
        }

        private ThreadStats convertToThreadStatsWithStack(java.lang.management.ThreadInfo jmxInfo) {
            ThreadStats stats = new ThreadStats();
            stats.setThreadId(jmxInfo.getThreadId());
            stats.setThreadName(jmxInfo.getThreadName());
            stats.setState(convertState(jmxInfo.getThreadState()));
            stats.setBlockedCount(jmxInfo.getBlockedCount());
            stats.setBlockedTime(jmxInfo.getBlockedTime());
            stats.setWaitedCount(jmxInfo.getWaitedCount());
            stats.setWaitedTime(jmxInfo.getWaitedTime());

            List<com.memdiag.core.thread.StackFrame> stackFrames = new ArrayList<>();
            for (StackTraceElement element : jmxInfo.getStackTrace()) {
                com.memdiag.core.thread.StackFrame frame = new com.memdiag.core.thread.StackFrame();
                frame.setClassName(element.getClassName());
                frame.setMethodName(element.getMethodName());
                frame.setFileName(element.getFileName());
                frame.setLineNumber(element.getLineNumber());
                frame.setNativeMethod(element.isNativeMethod());
                stackFrames.add(frame);
            }
            stats.setStackTrace(stackFrames);
            return stats;
        }

        private com.memdiag.core.thread.ThreadState convertState(Thread.State state) {
            if (state == null) return com.memdiag.core.thread.ThreadState.UNKNOWN;
            switch (state) {
                case NEW: return com.memdiag.core.thread.ThreadState.NEW;
                case RUNNABLE: return com.memdiag.core.thread.ThreadState.RUNNABLE;
                case BLOCKED: return com.memdiag.core.thread.ThreadState.BLOCKED;
                case WAITING: return com.memdiag.core.thread.ThreadState.WAITING;
                case TIMED_WAITING: return com.memdiag.core.thread.ThreadState.TIMED_WAITING;
                case TERMINATED: return com.memdiag.core.thread.ThreadState.TERMINATED;
                default: return com.memdiag.core.thread.ThreadState.UNKNOWN;
            }
        }
    }

    private class DiagnoseHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DiagnosisResult result = performLocalDiagnosis();
                // Convert to map for simple JSON serialization
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("timestamp", result.getTimestamp() != null ? result.getTimestamp().toString() : null);
                resultMap.put("totalHeapUsed", result.getTotalHeapUsed());
                resultMap.put("totalHeapCommitted", result.getTotalHeapCommitted());
                resultMap.put("threadCount", result.getThreadCount());
                resultMap.put("summary", result.getSummary());
                List<Map<String, Object>> issuesList = new ArrayList<>();
                if (result.getIssues() != null) {
                    for (com.memdiag.core.diagnose.Issue issue : result.getIssues()) {
                        Map<String, Object> issueMap = new HashMap<>();
                        issueMap.put("severity", issue.getSeverity() != null ? issue.getSeverity().name() : null);
                        issueMap.put("type", issue.getType());
                        issueMap.put("title", issue.getTitle());
                        issueMap.put("description", issue.getDescription());
                        issueMap.put("affectedClassName", issue.getAffectedClassName());
                        issueMap.put("affectedObjectCount", issue.getAffectedObjectCount());
                        issueMap.put("affectedBytes", issue.getAffectedBytes());
                        if (issue.getRecommendations() != null) {
                            List<Map<String, Object>> recList = new ArrayList<>();
                            for (com.memdiag.core.diagnose.Recommendation rec : issue.getRecommendations()) {
                                Map<String, Object> recMap = new HashMap<>();
                                recMap.put("priority", rec.getPriority());
                                recMap.put("title", rec.getTitle());
                                recMap.put("description", rec.getDescription());
                                recMap.put("action", rec.getAction());
                                recList.add(recMap);
                            }
                            issueMap.put("recommendations", recList);
                        }
                        issuesList.add(issueMap);
                    }
                }
                resultMap.put("issues", issuesList);
                sendJson(exchange, toJson(resultMap), 200);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }

        private DiagnosisResult performLocalDiagnosis() throws Exception {
            HeapHistogram histogram = new HeapHistogramHandler().getLocalHeapHistogram(100);
            ThreadDump threadDump = new ThreadsHandler().getLocalThreadDump();
            long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
            long heapCommitted = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted();

            DiagnosisResult.Builder result = DiagnosisResult.builder()
                .timestamp(java.time.Instant.now())
                .totalHeapUsed(heapUsed)
                .totalHeapCommitted(heapCommitted)
                .threadCount(threadDump.getThreadStats() != null ? threadDump.getThreadStats().size() : 0);

            List<com.memdiag.core.diagnose.Issue> issues = new ArrayList<>();

            com.memdiag.core.diagnose.DiagnosisContext context = com.memdiag.core.diagnose.DiagnosisContext.builder()
                .heapHistogram(histogram)
                .threadDump(threadDump)
                .totalHeapUsed(heapUsed)
                .totalHeapCommitted(heapCommitted)
                .build();

            RuleRegistry registry = RuleRegistry.withDefaults();
            for (com.memdiag.core.diagnose.DiagnosisRule rule : registry.getEnabledRules()) {
                try {
                    issues.addAll(rule.evaluate(context));
                } catch (Exception e) {
                    issues.add(com.memdiag.core.diagnose.Issue.builder()
                        .severity(com.memdiag.core.diagnose.Severity.WARNING)
                        .type("RULE_ERROR")
                        .title("Rule execution error: " + rule.getName())
                        .description("Rule " + rule.getId() + " failed: " + e.getMessage())
                        .build());
                }
            }

            long criticalCount = issues.stream().filter(i -> i.getSeverity() == com.memdiag.core.diagnose.Severity.CRITICAL).count();
            long warningCount = issues.stream().filter(i -> i.getSeverity() == com.memdiag.core.diagnose.Severity.WARNING).count();
            long infoCount = issues.stream().filter(i -> i.getSeverity() == com.memdiag.core.diagnose.Severity.INFO).count();

            String summary = String.format("Analysis complete: %,d critical, %,d warning, %,d info issues found. " +
                    "Heap: %,d bytes used, %,d classes. Threads: %,d active. Rules executed: %d.",
                criticalCount, warningCount, infoCount,
                histogram.getTotalBytes(),
                histogram.getClassStats().size(),
                threadDump.getThreadStats() != null ? threadDump.getThreadStats().size() : 0,
                registry.getEnabledRules().size());

            result.summary(summary);
            issues.forEach(result::addIssue);
            return result.build();
        }
    }

    private class GcRootsStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                GcRootTracker tracker = GcRootTracker.getInstance();
                // 确保 tracking 已启动
                tracker.startTracking();
                GcRootStats stats = tracker.getGcRootStats();

                // Convert to map for simple JSON serialization
                Map<String, Object> result = new HashMap<>();
                Map<String, Long> countsByType = new HashMap<>();
                for (GcRootType type : GcRootType.values()) {
                    countsByType.put(type.name(), stats.getCount(type));
                }
                result.put("countsByType", countsByType);
                result.put("totalRoots", stats.getTotalRoots());
                result.put("jvmtiAvailable", tracker.isAvailable());
                sendJson(exchange, toJson(result), 200);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class StartGcRootTrackingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }

            try {
                GcRootTracker tracker = GcRootTracker.getInstance();
                boolean success = tracker.startTracking();
                Map<String, Object> result = new HashMap<>();
                result.put("success", success);
                result.put("message", success ? "GC Root tracking started" : "Failed to start GC Root tracking");
                sendSuccess(exchange, result);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class StopGcRootTrackingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }

            try {
                GcRootTracker tracker = GcRootTracker.getInstance();
                boolean success = tracker.stopTracking();
                Map<String, Object> result = new HashMap<>();
                result.put("success", success);
                result.put("message", success ? "GC Root tracking stopped" : "Failed to stop GC Root tracking");
                sendSuccess(exchange, result);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private int getIntParam(HttpExchange exchange, String name, int defaultValue) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return defaultValue;
        }
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) {
                try {
                    return Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    // ========== Phase 3: Method Monitoring API Handlers ==========

    private abstract class BaseMethodHandler implements HttpHandler {
        protected MethodMonitorTransformer getMethodMonitorTransformer() {
            if (AgentContext.isInitialized()) {
                Object manager = AgentContext.getInstance().getInstrumentManager();
                if (manager instanceof InstrumentManager) {
                    return ((InstrumentManager) manager).getMethodMonitorTransformer();
                }
            }
            return null;
        }

        protected InstrumentManager getInstrumentManager() {
            if (AgentContext.isInitialized()) {
                Object manager = AgentContext.getInstance().getInstrumentManager();
                if (manager instanceof InstrumentManager) {
                    return (InstrumentManager) manager;
                }
            }
            return null;
        }
    }

    private class MethodsStatsHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                MethodMonitorTransformer transformer = getMethodMonitorTransformer();
                if (transformer != null) {
                    int limit = getIntParam(exchange, "limit", 20);
                    sendSuccess(exchange, transformer.toMap(limit));
                } else {
                    sendError(exchange, "Method monitor transformer not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class MethodsSlowHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                MethodMonitorTransformer transformer = getMethodMonitorTransformer();
                if (transformer != null) {
                    long thresholdMs = getIntParam(exchange, "threshold", 100);
                    int limit = getIntParam(exchange, "limit", 10);

                    List<Map<String, Object>> slowMethods = new ArrayList<>();
                    for (MethodMonitorTransformer.MethodStats stats : transformer.getTopMethodsByTotalTime(limit)) {
                        if (stats.getAverageTimeNanos() >= thresholdMs * 1_000_000L) {
                            slowMethods.add(stats.toMap());
                        }
                    }

                    sendSuccess(exchange, slowMethods);
                } else {
                    sendError(exchange, "Method monitor transformer not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class InstrumentationStatusHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                InstrumentManager manager = getInstrumentManager();
                Map<String, Object> status;

                if (manager != null) {
                    status = manager.toMap();
                } else {
                    status = new HashMap<>();
                    status.put("initialized", false);
                    status.put("message", "Instrument manager not available");
                    if (AgentContext.isInitialized()) {
                        status.put("configEnabled", AgentContext.getInstance().getConfig().isInstrumentationEnabled());
                    }
                }

                sendSuccess(exchange, status);
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class EnableAllocationTrackingHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                if (manager != null) {
                    boolean success = manager.enableAllocationTracking();
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Allocation tracking enabled");
                    } else {
                        response.put("error", "Failed to enable allocation tracking");
                    }
                    sendSuccess(exchange, response);
                } else {
                    sendError(exchange, "Instrument manager not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class DisableAllocationTrackingHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                if (manager != null) {
                    boolean success = manager.disableAllocationTracking();
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Allocation tracking disabled");
                    } else {
                        response.put("error", "Failed to disable allocation tracking");
                    }
                    sendSuccess(exchange, response);
                } else {
                    sendError(exchange, "Instrument manager not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class EnableMethodMonitoringHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                if (manager != null) {
                    boolean success = manager.enableMethodMonitoring();
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Method monitoring enabled");
                    } else {
                        response.put("error", "Failed to enable method monitoring");
                    }
                    sendSuccess(exchange, response);
                } else {
                    sendError(exchange, "Instrument manager not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }

    private class DisableMethodMonitoringHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                if (manager != null) {
                    boolean success = manager.disableMethodMonitoring();
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Method monitoring disabled");
                    } else {
                        response.put("error", "Failed to disable method monitoring");
                    }
                    sendSuccess(exchange, response);
                } else {
                    sendError(exchange, "Instrument manager not available", 500);
                }
            } catch (Exception e) {
                sendError(exchange, e.getMessage(), 500);
            }
        }
    }
}
