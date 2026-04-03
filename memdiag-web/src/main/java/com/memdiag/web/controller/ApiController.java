package com.memdiag.web.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.nmt.NmtMemoryUsage;
import com.memdiag.core.nmt.NmtSnapshot;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;
import com.memdiag.core.util.JmxClient;
import com.memdiag.web.config.MemDiagProperties;
import com.memdiag.web.service.*;
import com.memdiag.web.validation.AddressValidator;
import com.memdiag.web.validation.PidValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private static final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonSerializer<Instant>) (src, typeOfSrc, context) -> 
                new com.google.gson.JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class, (com.google.gson.JsonDeserializer<Instant>) (json, typeOfT, context) -> 
                Instant.parse(json.getAsString()))
            .create();

    private static final String ERROR_HISTOGRAM = "Failed to retrieve histogram";
    private static final String ERROR_DIAGNOSIS = "Failed to perform diagnosis";
    private static final String ERROR_THREADS = "Failed to retrieve threads";
    private static final String ERROR_NMT = "Failed to retrieve NMT data";
    private static final String ERROR_CONNECTION = "Connection failed";
    private static final String ERROR_GENERIC = "An error occurred while processing your request";
    private static final String ERROR_AGENT_STATUS = "Failed to retrieve agent status";
    private static final String ERROR_AGENT_CONFIG = "Failed to retrieve agent configuration";
    private static final String ERROR_AGENT_METRICS = "Failed to retrieve agent metrics";
    private static final String ERROR_AGENT_DETACH = "Failed to detach agent";
    private static final String ERROR_NATIVE_STATUS = "Failed to retrieve native status";
    private static final String ERROR_NATIVE_SUMMARY = "Failed to retrieve native summary";
    private static final String ERROR_NATIVE_REGIONS = "Failed to retrieve native regions";
    private static final String ERROR_NATIVE_DIAGNOSIS = "Failed to perform native diagnosis";
    private static final String ERROR_ALLOCATIONS_RECENT = "Failed to retrieve recent allocations";
    private static final String ERROR_ALLOCATIONS_STATS = "Failed to retrieve allocation statistics";
    private static final String ERROR_ALLOCATIONS_TOP = "Failed to retrieve top allocations";
    private static final String ERROR_ALLOCATIONS_RATE = "Failed to retrieve allocation rate";
    private static final String ERROR_ALLOCATIONS_SUMMARY = "Failed to retrieve allocation summary";
    private static final String ERROR_METHODS_STATS = "Failed to retrieve method statistics";
    private static final String ERROR_METHODS_SLOW = "Failed to retrieve slow methods";
    private static final String ERROR_INSTRUMENTATION_STATUS = "Failed to retrieve instrumentation status";
    private static final String ERROR_INSTRUMENTATION_ENABLE = "Failed to enable instrumentation";
    private static final String ERROR_INSTRUMENTATION_DISABLE = "Failed to disable instrumentation";
    private static final String ERROR_JVMTI_STATUS = "Failed to retrieve JVMTI status";
    private static final String ERROR_GC_ROOTS_STATS = "Failed to retrieve GC roots statistics";
    private static final String ERROR_GC_ROOTS_TRACK = "Failed to control GC roots tracking";
    private static final String ERROR_SNAPSHOT_CREATE = "Failed to create snapshot";
    private static final String ERROR_SNAPSHOT_LIST = "Failed to list snapshots";
    private static final String ERROR_SNAPSHOT_DELETE = "Failed to delete snapshot";

    private final ConnectionManager connectionManager;
    private final JmxAnalysisService jmxAnalysisService;
    private final AgentApiService agentApiService;
    private final SnapshotService snapshotService;
    private final GcRootsService gcRootsService;
    private final MemDiagProperties properties;

    public ApiController(ConnectionManager connectionManager,
                         JmxAnalysisService jmxAnalysisService,
                         AgentApiService agentApiService,
                         SnapshotService snapshotService,
                         GcRootsService gcRootsService,
                         MemDiagProperties properties) {
        this.connectionManager = connectionManager;
        this.jmxAnalysisService = jmxAnalysisService;
        this.agentApiService = agentApiService;
        this.snapshotService = snapshotService;
        this.gcRootsService = gcRootsService;
        this.properties = properties;
    }

    private ResponseEntity<String> validationError(String message) {
        return ResponseEntity.badRequest().body(gson.toJson(errorResponse(message)));
    }

    private boolean isConnectionId(String id) {
        return !PidValidator.isValid(id);
    }

    // ========== Connection Management ==========

    @GetMapping("/connections")
    public ResponseEntity<String> getConnections() {
        try {
            return ResponseEntity.ok(gson.toJson(connectionManager.getConnections()));
        } catch (Exception e) {
            logger.error("Error getting connections", e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GENERIC)));
        }
    }

    @PostMapping("/connections/{id}")
    public ResponseEntity<String> connect(
            @PathVariable String id,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) String pid) {
        
        String effectiveTarget = target != null ? target : pid;

        if (effectiveTarget != null && !effectiveTarget.isEmpty()) {
            if (!AddressValidator.isValid(effectiveTarget)) {
                logger.warn("Invalid target address: {}", effectiveTarget);
                return validationError(AddressValidator.getErrorMessage(effectiveTarget));
            }
        }

        try {
            boolean success = connectionManager.connect(id, effectiveTarget);
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("id", id);
            result.addProperty("timestamp", System.currentTimeMillis());
            
            if (success) {
                return ResponseEntity.ok(gson.toJson(result));
            } else {
                return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_CONNECTION)));
            }
        } catch (Exception e) {
            logger.error("Error connecting to id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_CONNECTION)));
        }
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<String> disconnect(@PathVariable String id) {
        try {
            connectionManager.disconnect(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("id", id);
            result.addProperty("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error disconnecting id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GENERIC)));
        }
    }

    // ========== Core Analysis API ==========

    @GetMapping("/histogram/{id}")
    public ResponseEntity<String> getHistogram(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            HeapHistogram histogram;
            ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
            if (type == ConnectionManager.ConnectionType.AGENT) {
                histogram = agentApiService.getHistogram(id, limit);
            } else {
                JmxClient client = connectionManager.getJmxClient(id);
                if (client == null) throw new IllegalArgumentException("No connection found for id: " + id);
                histogram = jmxAnalysisService.getHistogram(client, limit);
            }

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("totalObjects", histogram.getTotalObjects());
            data.addProperty("totalBytes", histogram.getTotalBytes());

            JsonArray classesArray = new JsonArray();
            for (ClassStats stats : histogram.getClassStats()) {
                JsonObject item = new JsonObject();
                item.addProperty("className", stats.getClassName());
                item.addProperty("objectCount", stats.getObjectCount());
                item.addProperty("shallowBytes", stats.getShallowBytes());
                classesArray.add(item);
            }
            data.add("classes", classesArray);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting histogram for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_HISTOGRAM)));
        }
    }

    @GetMapping("/diagnose/{id}")
    public ResponseEntity<String> diagnose(@PathVariable String id) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            DiagnosisResult diagnosis;
            ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
            if (type == ConnectionManager.ConnectionType.AGENT) {
                diagnosis = agentApiService.getDiagnosis(id);
            } else {
                com.memdiag.core.diagnose.DiagnosisEngine engine = connectionManager.getDiagnosisEngine(id);
                if (engine == null) throw new IllegalArgumentException("No connection found for id: " + id);
                diagnosis = jmxAnalysisService.diagnose(engine);
            }

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("summary", diagnosis.getSummary());
            data.addProperty("totalHeapUsed", diagnosis.getTotalHeapUsed());
            data.addProperty("totalHeapCommitted", diagnosis.getTotalHeapCommitted());
            data.addProperty("threadCount", diagnosis.getThreadCount());

            JsonArray issuesArray = new JsonArray();
            for (Issue issue : diagnosis.getIssues()) {
                JsonObject item = new JsonObject();
                item.addProperty("severity", issue.getSeverity().name());
                item.addProperty("type", issue.getType());
                item.addProperty("title", issue.getTitle());
                item.addProperty("description", issue.getDescription());
                
                JsonArray recsArray = new JsonArray();
                for (Recommendation rec : issue.getRecommendations()) {
                    JsonObject recItem = new JsonObject();
                    recItem.addProperty("priority", rec.getPriority());
                    recItem.addProperty("title", rec.getTitle());
                    recItem.addProperty("description", rec.getDescription());
                    recItem.addProperty("action", rec.getAction());
                    recsArray.add(recItem);
                }
                item.add("recommendations", recsArray);
                issuesArray.add(item);
            }
            data.add("issues", issuesArray);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error performing diagnosis for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_DIAGNOSIS)));
        }
    }

    @GetMapping("/threads/{id}")
    public ResponseEntity<String> getThreads(@PathVariable String id) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            ThreadDump dump;
            ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
            if (type == ConnectionManager.ConnectionType.AGENT) {
                dump = agentApiService.getThreadDump(id);
            } else {
                JmxClient client = connectionManager.getJmxClient(id);
                if (client == null) throw new IllegalArgumentException("No connection found for id: " + id);
                dump = jmxAnalysisService.getThreadDump(client);
            }

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("threadCount", dump.getThreadStats().size());

            JsonArray threadsArray = new JsonArray();
            for (ThreadStats stats : dump.getThreadStats()) {
                JsonObject item = new JsonObject();
                item.addProperty("threadId", stats.getThreadId());
                item.addProperty("threadName", stats.getThreadName());
                item.addProperty("state", stats.getState().name());
                item.addProperty("blockedCount", stats.getBlockedCount());
                item.addProperty("waitedCount", stats.getWaitedCount());
                threadsArray.add(item);
            }
            data.add("threads", threadsArray);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting threads for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_THREADS)));
        }
    }

    @GetMapping("/nmt/{id}")
    public ResponseEntity<String> getNmt(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean detail) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            NmtSnapshot nmt;
            ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
            if (type == ConnectionManager.ConnectionType.AGENT) {
                nmt = agentApiService.getNmtSnapshot(id, detail);
            } else {
                JmxClient client = connectionManager.getJmxClient(id);
                if (client == null) throw new IllegalArgumentException("No connection found for id: " + id);
                nmt = jmxAnalysisService.getNmtSnapshot(client, detail);
            }

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("totalReserved", nmt.getTotalReserved());
            data.addProperty("totalCommitted", nmt.getTotalCommitted());
            
            JsonArray categoriesArray = new JsonArray();
            for (NmtMemoryUsage usage : nmt.getUsages()) {
                JsonObject item = new JsonObject();
                item.addProperty("name", usage.getCategory().name());
                item.addProperty("reserved", usage.getReserved());
                item.addProperty("committed", usage.getCommitted());
                categoriesArray.add(item);
            }
            data.add("categories", categoriesArray);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting NMT for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NMT)));
        }
    }

    // ========== Agent Management API ==========

    @GetMapping("/agent/status/{id}")
    public ResponseEntity<String> getAgentStatus(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAgentStatus(id)));
        } catch (Exception e) {
            logger.error("Error getting agent status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_STATUS)));
        }
    }

    @GetMapping("/agent/config/{id}")
    public ResponseEntity<String> getAgentConfig(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAgentConfig(id)));
        } catch (Exception e) {
            logger.error("Error getting agent config for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_CONFIG)));
        }
    }

    @PostMapping("/agent/config/{id}")
    public ResponseEntity<String> updateAgentConfig(@PathVariable String id, @RequestBody Map<String, Object> config) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.updateAgentConfig(id, config)));
        } catch (Exception e) {
            logger.error("Error updating agent config for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GENERIC)));
        }
    }

    @GetMapping("/agent/metrics/{id}")
    public ResponseEntity<String> getAgentMetrics(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAgentMetrics(id)));
        } catch (Exception e) {
            logger.error("Error getting agent metrics for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_METRICS)));
        }
    }

    @PostMapping("/agent/detach/{id}")
    public ResponseEntity<String> detachAgent(@PathVariable String id) {
        try {
            boolean success = agentApiService.detachAgent(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error detaching agent for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_DETACH)));
        }
    }

    // ========== Native Memory API ==========

    @GetMapping("/native/status/{id}")
    public ResponseEntity<String> getNativeStatus(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getNativeStatus(id)));
        } catch (Exception e) {
            logger.error("Error getting native status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_STATUS)));
        }
    }

    @GetMapping("/native/summary/{id}")
    public ResponseEntity<String> getNativeSummary(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getNativeSummary(id)));
        } catch (Exception e) {
            logger.error("Error getting native summary for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_SUMMARY)));
        }
    }

    @GetMapping("/native/regions/{id}")
    public ResponseEntity<String> getNativeRegions(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getNativeRegions(id)));
        } catch (Exception e) {
            logger.error("Error getting native regions for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_REGIONS)));
        }
    }

    @GetMapping("/native/diagnose/{id}")
    public ResponseEntity<String> getNativeDiagnosis(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getNativeDiagnosis(id)));
        } catch (Exception e) {
            logger.error("Error performing native diagnosis for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_DIAGNOSIS)));
        }
    }

    // ========== Allocations API ==========

    @GetMapping("/allocations/recent/{id}")
    public ResponseEntity<String> getAllocationsRecent(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAllocationsRecent(id, limit)));
        } catch (Exception e) {
            logger.error("Error getting recent allocations for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_RECENT)));
        }
    }

    @GetMapping("/allocations/stats/{id}")
    public ResponseEntity<String> getAllocationsStats(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAllocationsStats(id)));
        } catch (Exception e) {
            logger.error("Error getting allocation stats for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_STATS)));
        }
    }

    @GetMapping("/allocations/top/{id}")
    public ResponseEntity<String> getAllocationsTop(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAllocationsTop(id, limit)));
        } catch (Exception e) {
            logger.error("Error getting top allocations for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_TOP)));
        }
    }

    @GetMapping("/allocations/rate/{id}")
    public ResponseEntity<String> getAllocationsRate(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAllocationsRate(id)));
        } catch (Exception e) {
            logger.error("Error getting allocation rate for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_RATE)));
        }
    }

    @GetMapping("/allocations/summary/{id}")
    public ResponseEntity<String> getAllocationsSummary(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getAllocationsSummary(id)));
        } catch (Exception e) {
            logger.error("Error getting allocation summary for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_SUMMARY)));
        }
    }

    // ========== Methods API ==========

    @GetMapping("/methods/stats/{id}")
    public ResponseEntity<String> getMethodsStats(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getMethodsStats(id, limit)));
        } catch (Exception e) {
            logger.error("Error getting method stats for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_METHODS_STATS)));
        }
    }

    @GetMapping("/methods/slow/{id}")
    public ResponseEntity<String> getMethodsSlow(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "100") int threshold) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getMethodsSlow(id, limit, threshold)));
        } catch (Exception e) {
            logger.error("Error getting slow methods for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_METHODS_SLOW)));
        }
    }

    // ========== Instrumentation API ==========

    @GetMapping("/instrumentation/status/{id}")
    public ResponseEntity<String> getInstrumentationStatus(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getInstrumentationStatus(id)));
        } catch (Exception e) {
            logger.error("Error getting instrumentation status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_STATUS)));
        }
    }

    @PostMapping("/instrumentation/allocation/enable/{id}")
    public ResponseEntity<String> enableAllocationTracking(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.enableAllocationTracking(id)));
        } catch (Exception e) {
            logger.error("Error enabling allocation tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_ENABLE)));
        }
    }

    @PostMapping("/instrumentation/allocation/disable/{id}")
    public ResponseEntity<String> disableAllocationTracking(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.disableAllocationTracking(id)));
        } catch (Exception e) {
            logger.error("Error disabling allocation tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_DISABLE)));
        }
    }

    @PostMapping("/instrumentation/methods/enable/{id}")
    public ResponseEntity<String> enableMethodMonitoring(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.enableMethodMonitoring(id)));
        } catch (Exception e) {
            logger.error("Error enabling method monitoring for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GENERIC)));
        }
    }

    @PostMapping("/instrumentation/methods/disable/{id}")
    public ResponseEntity<String> disableMethodMonitoring(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.disableMethodMonitoring(id)));
        } catch (Exception e) {
            logger.error("Error disabling method monitoring for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GENERIC)));
        }
    }

    // ========== JVMTI API ==========

    @GetMapping("/jvmti/status/{id}")
    public ResponseEntity<String> getJvmtiStatus(@PathVariable String id) {
        try {
            return ResponseEntity.ok(gson.toJson(agentApiService.getJvmtiStatus(id)));
        } catch (Exception e) {
            logger.error("Error getting JVMTI status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_JVMTI_STATUS)));
        }
    }

    // ========== GC Roots API ==========

    @GetMapping("/gc-roots/stats/{id}")
    public ResponseEntity<String> getGcRootStats(@PathVariable String id) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            com.memdiag.core.heap.GcRootStats stats = gcRootsService.getGcRootStats(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            JsonObject countsObj = new JsonObject();
            for (Map.Entry<com.memdiag.core.heap.GcRootType, Long> entry : stats.getCountsByType().entrySet()) {
                countsObj.addProperty(entry.getKey().name(), entry.getValue());
            }
            data.add("countsByType", countsObj);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting GC roots statistics for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GC_ROOTS_STATS)));
        }
    }

    @PostMapping("/gc-roots/track/start/{id}")
    public ResponseEntity<String> startGcRootTracking(@PathVariable String id) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            boolean success = gcRootsService.startGcRootTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error starting GC root tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GC_ROOTS_TRACK)));
        }
    }

    @PostMapping("/gc-roots/track/stop/{id}")
    public ResponseEntity<String> stopGcRootTracking(@PathVariable String id) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            boolean success = gcRootsService.stopGcRootTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error stopping GC root tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GC_ROOTS_TRACK)));
        }
    }

    // ========== Snapshot Management ==========

    @PostMapping({"/snapshots/{id}", "/connections/{id}/snapshots"})
    public ResponseEntity<String> createSnapshot(
            @PathVariable String id,
            @RequestParam(required = false) String name) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            com.memdiag.core.diff.Snapshot snapshot = snapshotService.createSnapshot(id, name);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("id", snapshot.getId());
            data.addProperty("timestamp", snapshot.getTimestamp().toString());
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error creating snapshot for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/snapshots/{id}/{snapshotId}", "/connections/{id}/snapshots/{snapshotId}"})
    public ResponseEntity<String> loadSnapshot(
            @PathVariable String id,
            @PathVariable String snapshotId) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            com.memdiag.core.diff.Snapshot snapshot = snapshotService.loadSnapshot(id, snapshotId);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", gson.toJsonTree(snapshot));

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error loading snapshot {} for id: {}", snapshotId, id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/snapshots/{id}", "/connections/{id}/snapshots"})
    public ResponseEntity<String> listSnapshots(@PathVariable String id) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            java.util.List<com.memdiag.core.diff.SnapshotManager.SnapshotInfo> snapshots = snapshotService.listSnapshots(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonArray dataArray = new JsonArray();
            for (com.memdiag.core.diff.SnapshotManager.SnapshotInfo info : snapshots) {
                JsonObject item = new JsonObject();
                item.addProperty("id", info.id != null ? info.id : info.filename);
                item.addProperty("name", info.filename);
                item.addProperty("createdAt", info.lastModified.toString());
                item.addProperty("size", info.size);
                dataArray.add(item);
            }
            result.add("data", dataArray);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error listing snapshots for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_SNAPSHOT_LIST)));
        }
    }

    @DeleteMapping({"/snapshots/{id}/{snapshotId}", "/connections/{id}/snapshots/{snapshotId}"})
    public ResponseEntity<String> deleteSnapshot(
            @PathVariable String id,
            @PathVariable String snapshotId) {
        if (!isConnectionId(id)) {
            logger.warn("Invalid connection ID or PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            boolean deleted = snapshotService.deleteSnapshot(id, snapshotId);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("deleted", deleted);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error deleting snapshot for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_SNAPSHOT_DELETE)));
        }
    }

    // ========== Helper methods ==========

    private JsonObject errorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        return error;
    }
}
