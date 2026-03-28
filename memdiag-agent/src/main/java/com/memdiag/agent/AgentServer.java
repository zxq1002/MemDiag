package com.memdiag.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.memdiag.agent.collect.AllocationEvent;
import com.memdiag.agent.collect.DataCollector;
import com.memdiag.agent.collect.StatsAggregator;
import com.memdiag.agent.instrument.InstrumentManager;
import com.memdiag.agent.instrument.MethodMonitorTransformer;
import com.memdiag.agent.jvmti.AgentJVMTILoader;
import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.RuleRegistry;
import com.memdiag.core.heap.ClassStats;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgentServer {

    private final String host;
    private final int port;
    private final Instrumentation instrumentation;
    private final NativeMemoryAnalyzer nativeAnalyzer;
    private final ThreadMXBean threadMXBean;
    private final Gson gson;

    private HttpServer server;
    private ExecutorService executor;
    private volatile boolean running = false;

    public AgentServer(String host, int port, Instrumentation instrumentation) {
        this.host = host;
        this.port = port;
        this.instrumentation = instrumentation;
        this.nativeAnalyzer = NativeMemoryAnalyzerFactory.getInstance();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.gson = new GsonBuilder().setPrettyPrinting().create();

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

    private void sendJsonResponse(HttpExchange exchange, Object response, int statusCode) throws IOException {
        String json = gson.toJson(response);
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String error, int statusCode) throws IOException {
        Map<String, String> response = new HashMap<>();
        response.put("error", error);
        sendJsonResponse(exchange, response, statusCode);
    }

    private class SimpleHandler implements HttpHandler {
        private final String type;

        SimpleHandler(String type) {
            this.type = type;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", type + " endpoint placeholder");
            response.put("timestamp", System.currentTimeMillis());
            sendJsonResponse(exchange, response, 200);
        }
    }

    private class DetachHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }

            try {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Detach request received");
                sendJsonResponse(exchange, response, 200);

                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                        stop();
                    } catch (Exception e) {
                    }
                }).start();
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", status);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class NativeSummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NativeMemorySummary summary = nativeAnalyzer.getSummary();
                Map<String, Object> data = new HashMap<>();
                data.put("totalResident", summary.getTotalResident());
                data.put("totalVirtual", summary.getTotalVirtual());
                data.put("directByteBufferSize", summary.getDirectByteBufferSize());
                data.put("jniAllocatedSize", summary.getJniAllocatedSize());
                data.put("threadStackSize", summary.getThreadStackSize());
                data.put("codeCacheSize", summary.getCodeCacheSize());
                data.put("breakdownByCategory", summary.getBreakdownByCategory());

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", data);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", regionData);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", data);
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", status);
                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);

                    if (AgentContext.isInitialized()) {
                        AgentConfig config = AgentContext.getInstance().getConfig();
                        response.put("data", config.toMap());
                    } else {
                        response.put("data", AgentConfig.defaults().toMap());
                    }

                    response.put("timestamp", System.currentTimeMillis());
                    sendJsonResponse(exchange, response, 200);
                } else if ("PUT".equals(method)) {
                    // Update configuration (placeholder - requires restart for most changes)
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Configuration update requires agent restart");
                    response.put("timestamp", System.currentTimeMillis());
                    sendJsonResponse(exchange, response, 200);
                } else {
                    sendErrorResponse(exchange, "Method not allowed", 405);
                }
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", metrics);
                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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
                Map<String, Object> response = new HashMap<>();

                if (collector != null) {
                    int limit = getIntParam(exchange, "limit", 100);
                    response.put("success", true);
                    response.put("data", collector.getRecentEvents(limit));
                } else {
                    response.put("success", false);
                    response.put("error", "Data collector not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsStatsHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DataCollector collector = getDataCollector();
                Map<String, Object> response = new HashMap<>();

                if (collector != null) {
                    response.put("success", true);
                    response.put("data", collector.toMap());
                } else {
                    response.put("success", false);
                    response.put("error", "Data collector not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsTopHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                DataCollector collector = getDataCollector();
                Map<String, Object> response = new HashMap<>();

                if (collector != null) {
                    int limit = getIntParam(exchange, "limit", 10);
                    String type = exchange.getRequestURI().getQuery() != null &&
                            exchange.getRequestURI().getQuery().contains("type=count") ? "count" : "size";

                    response.put("success", true);
                    if ("count".equals(type)) {
                        response.put("data", collector.getTopTypesByCount(limit));
                    } else {
                        response.put("data", collector.getTopTypesBySize(limit));
                    }
                } else {
                    response.put("success", false);
                    response.put("error", "Data collector not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsRateHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                StatsAggregator aggregator = getStatsAggregator();
                Map<String, Object> response = new HashMap<>();

                if (aggregator != null) {
                    aggregator.takeSnapshot();
                    int windowSec = getIntParam(exchange, "window", 60);
                    response.put("success", true);

                    Map<String, Object> data = new HashMap<>();
                    data.put("currentRateBytesPerSec", aggregator.getCurrentRateBytesPerSec());
                    data.put("trend", aggregator.getTrend().name());
                    data.put("windowRates", aggregator.getRateHistory(windowSec));
                    response.put("data", data);
                } else {
                    response.put("success", false);
                    response.put("error", "Stats aggregator not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class AllocationsSummaryHandler extends BaseAllocationHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                StatsAggregator aggregator = getStatsAggregator();
                Map<String, Object> response = new HashMap<>();

                if (aggregator != null) {
                    response.put("success", true);
                    response.put("data", aggregator.getSummary());
                } else {
                    response.put("success", false);
                    response.put("error", "Stats aggregator not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    // ========== Phase 4: JVMTI API Handlers ==========

    private class JVMTIStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> response = new HashMap<>();

                if (AgentContext.isInitialized()) {
                    AgentContext ctx = AgentContext.getInstance();
                    AgentJVMTILoader loader = ctx.getJvmtiLoader();

                    response.put("success", true);
                    if (loader != null) {
                        response.put("data", loader.getStatus());
                    } else {
                        Map<String, Object> status = new HashMap<>();
                        status.put("enabled", ctx.getConfig().isJvmtiEnabled());
                        status.put("autoLoad", ctx.getConfig().isJvmtiAutoLoad());
                        status.put("loaded", false);
                        status.put("available", false);
                        status.put("error", "JVMTI loader not initialized");
                        status.put("platform", System.getProperty("os.name"));
                        status.put("arch", System.getProperty("os.arch"));
                        response.put("data", status);
                    }
                } else {
                    response.put("success", false);
                    response.put("error", "Agent context not initialized");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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
                sendJsonResponse(exchange, histogram, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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
                sendJsonResponse(exchange, dump, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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
                sendJsonResponse(exchange, result, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
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
                .threadCount(threadDump.getThreadCount());

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
                threadDump.getThreadCount(),
                registry.getEnabledRules().size());

            result.summary(summary);
            issues.forEach(result::addIssue);
            return result.build();
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
                Map<String, Object> response = new HashMap<>();

                if (transformer != null) {
                    int limit = getIntParam(exchange, "limit", 20);
                    response.put("success", true);
                    response.put("data", transformer.toMap(limit));
                } else {
                    response.put("success", false);
                    response.put("error", "Method monitor transformer not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class MethodsSlowHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                MethodMonitorTransformer transformer = getMethodMonitorTransformer();
                Map<String, Object> response = new HashMap<>();

                if (transformer != null) {
                    long thresholdMs = getIntParam(exchange, "threshold", 100);
                    int limit = getIntParam(exchange, "limit", 10);

                    List<Map<String, Object>> slowMethods = new ArrayList<>();
                    for (MethodMonitorTransformer.MethodStats stats : transformer.getTopMethodsByTotalTime(limit)) {
                        if (stats.getAverageTimeNanos() >= thresholdMs * 1_000_000L) {
                            slowMethods.add(stats.toMap());
                        }
                    }

                    response.put("success", true);
                    response.put("data", slowMethods);
                } else {
                    response.put("success", false);
                    response.put("error", "Method monitor transformer not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class InstrumentationStatusHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                InstrumentManager manager = getInstrumentManager();
                Map<String, Object> response = new HashMap<>();

                if (manager != null) {
                    response.put("success", true);
                    response.put("data", manager.toMap());
                } else {
                    Map<String, Object> status = new HashMap<>();
                    status.put("initialized", false);
                    status.put("message", "Instrument manager not available");
                    if (AgentContext.isInitialized()) {
                        status.put("configEnabled", AgentContext.getInstance().getConfig().isInstrumentationEnabled());
                    }
                    response.put("success", true);
                    response.put("data", status);
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class EnableAllocationTrackingHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                Map<String, Object> response = new HashMap<>();

                if (manager != null) {
                    boolean success = manager.enableAllocationTracking();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Allocation tracking enabled");
                    } else {
                        response.put("error", "Failed to enable allocation tracking");
                    }
                } else {
                    response.put("success", false);
                    response.put("error", "Instrument manager not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class DisableAllocationTrackingHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                Map<String, Object> response = new HashMap<>();

                if (manager != null) {
                    boolean success = manager.disableAllocationTracking();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Allocation tracking disabled");
                    } else {
                        response.put("error", "Failed to disable allocation tracking");
                    }
                } else {
                    response.put("success", false);
                    response.put("error", "Instrument manager not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class EnableMethodMonitoringHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                Map<String, Object> response = new HashMap<>();

                if (manager != null) {
                    boolean success = manager.enableMethodMonitoring();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Method monitoring enabled");
                    } else {
                        response.put("error", "Failed to enable method monitoring");
                    }
                } else {
                    response.put("success", false);
                    response.put("error", "Instrument manager not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }

    private class DisableMethodMonitoringHandler extends BaseMethodHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendErrorResponse(exchange, "Method not allowed", 405);
                return;
            }

            try {
                InstrumentManager manager = getInstrumentManager();
                Map<String, Object> response = new HashMap<>();

                if (manager != null) {
                    boolean success = manager.disableMethodMonitoring();
                    response.put("success", success);
                    if (success) {
                        response.put("message", "Method monitoring disabled");
                    } else {
                        response.put("error", "Failed to disable method monitoring");
                    }
                } else {
                    response.put("success", false);
                    response.put("error", "Instrument manager not available");
                }

                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, response, 200);
            } catch (Exception e) {
                sendErrorResponse(exchange, e.getMessage(), 500);
            }
        }
    }
}
