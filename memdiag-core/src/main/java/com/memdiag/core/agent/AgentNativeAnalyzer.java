package com.memdiag.core.agent;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.memdiag.core.nativeapi.LibraryMapping;
import com.memdiag.core.nativeapi.MemoryRegion;
import com.memdiag.core.nativeapi.NativeDiagnosis;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzer;
import com.memdiag.core.nativeapi.NativeMemorySummary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NativeMemoryAnalyzer implementation that communicates with a running MemDiag agent via HTTP.
 */
public class AgentNativeAnalyzer implements NativeMemoryAnalyzer {

    private static final Gson gson = new Gson();

    private final AgentClient client;
    private final String agentJarPath;
    private final String targetPid;
    private volatile boolean attached = false;

    public AgentNativeAnalyzer(AgentClient client, String agentJarPath, String targetPid) {
        this.client = client;
        this.agentJarPath = agentJarPath;
        this.targetPid = targetPid;
    }

    public AgentNativeAnalyzer(String host, int port, String agentJarPath, String targetPid) {
        this(new AgentClient(host, port), agentJarPath, targetPid);
    }

    public AgentNativeAnalyzer(int port, String agentJarPath, String targetPid) {
        this(new AgentClient(port), agentJarPath, targetPid);
    }

    @Override
    public boolean isAvailable() {
        return client.isReachable();
    }

    @Override
    public String getPlatform() {
        return "Linux (Agent HTTP)";
    }

    @Override
    public boolean requiresAgent() {
        return true;
    }

    @Override
    public boolean isAgentAttached() {
        if (attached) {
            return true;
        }
        return client.isReachable();
    }

    @Override
    public boolean attachAgent() {
        if (client.isReachable()) {
            attached = true;
            return true;
        }

        if (agentJarPath != null && targetPid != null) {
            if (AgentAttacher.attach(targetPid, agentJarPath, client.getPort())) {
                // Wait a bit for the agent to start
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Verify the agent is reachable
                for (int i = 0; i < 10; i++) {
                    if (client.isReachable()) {
                        attached = true;
                        return true;
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        return client.isReachable();
    }

    @Override
    public boolean detachAgent() {
        boolean result = client.detach();
        attached = false;
        return result;
    }

    @Override
    public boolean startAllocationTracking() {
        // TODO: Implement via agent API
        return false;
    }

    @Override
    public boolean stopAllocationTracking() {
        // TODO: Implement via agent API
        return false;
    }

    @Override
    public boolean isTrackingEnabled() {
        // TODO: Implement via agent API
        return false;
    }

    @Override
    public long getTotalAllocated() {
        // TODO: Implement via agent API
        return 0;
    }

    @Override
    public long getLiveBytes() {
        // TODO: Implement via agent API
        return 0;
    }

    @Override
    public NativeMemorySummary getSummary() {
        JsonObject response = client.getNativeSummary();
        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            if (response.has("data")) {
                JsonObject data = response.getAsJsonObject("data");
                NativeMemorySummary.Builder builder = NativeMemorySummary.builder();

                if (data.has("totalResident")) {
                    builder.totalResident(data.get("totalResident").getAsLong());
                }
                if (data.has("totalVirtual")) {
                    builder.totalVirtual(data.get("totalVirtual").getAsLong());
                }
                if (data.has("directByteBufferSize")) {
                    builder.directByteBufferSize(data.get("directByteBufferSize").getAsLong());
                }
                if (data.has("threadStackSize")) {
                    builder.threadStackSize(data.get("threadStackSize").getAsLong());
                }
                if (data.has("codeCacheSize")) {
                    builder.codeCacheSize(data.get("codeCacheSize").getAsLong());
                }
                if (data.has("breakdownByCategory")) {
                    JsonObject categories = data.getAsJsonObject("breakdownByCategory");
                    Map<String, Long> categoryMap = new HashMap<>();
                    for (String key : categories.keySet()) {
                        categoryMap.put(key, categories.get(key).getAsLong());
                    }
                    builder.breakdownByCategory(categoryMap);
                }

                return builder.build();
            }
        }
        // Fallback to empty summary if parsing fails
        return NativeMemorySummary.builder()
                .totalResident(0)
                .totalVirtual(0)
                .threadStackSize(0)
                .codeCacheSize(0)
                .build();
    }

    @Override
    public List<MemoryRegion> getMemoryRegions() {
        JsonObject response = client.getNativeRegions();
        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            if (response.has("data")) {
                JsonArray dataArray = response.getAsJsonArray("data");
                List<MemoryRegion> regions = new ArrayList<>();

                for (JsonElement element : dataArray) {
                    JsonObject regionJson = element.getAsJsonObject();
                    MemoryRegion.Builder builder = MemoryRegion.builder();

                    if (regionJson.has("startAddress")) {
                        builder.startAddress(regionJson.get("startAddress").getAsLong());
                    }
                    if (regionJson.has("endAddress")) {
                        builder.endAddress(regionJson.get("endAddress").getAsLong());
                    }
                    if (regionJson.has("size")) {
                        builder.size(regionJson.get("size").getAsLong());
                    }
                    if (regionJson.has("residentSize")) {
                        builder.residentSize(regionJson.get("residentSize").getAsLong());
                    }
                    if (regionJson.has("permissions")) {
                        builder.permissions(regionJson.get("permissions").getAsString());
                    }
                    if (regionJson.has("mappingFile")) {
                        builder.mappingFile(regionJson.get("mappingFile").getAsString());
                    }
                    if (regionJson.has("regionType")) {
                        builder.regionType(regionJson.get("regionType").getAsString());
                    }

                    regions.add(builder.build());
                }

                return regions;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<LibraryMapping> getLibraryMappings() {
        return Collections.emptyList();
    }

    @Override
    public NativeDiagnosis analyzeNativeLeaks() {
        JsonObject response = client.getNativeDiagnosis();
        if (response != null && response.has("success") && response.get("success").getAsBoolean()) {
            if (response.has("data")) {
                JsonObject data = response.getAsJsonObject("data");
                NativeDiagnosis.Builder builder = NativeDiagnosis.builder();

                if (data.has("findings")) {
                    JsonArray findingsArray = data.getAsJsonArray("findings");
                    List<String> findings = new ArrayList<>();
                    for (JsonElement element : findingsArray) {
                        findings.add(element.getAsString());
                    }
                    builder.findings(findings);
                }
                if (data.has("warnings")) {
                    JsonArray warningsArray = data.getAsJsonArray("warnings");
                    List<String> warnings = new ArrayList<>();
                    for (JsonElement element : warningsArray) {
                        warnings.add(element.getAsString());
                    }
                    builder.warnings(warnings);
                }
                if (data.has("recommendations")) {
                    JsonArray recommendationsArray = data.getAsJsonArray("recommendations");
                    List<String> recommendations = new ArrayList<>();
                    for (JsonElement element : recommendationsArray) {
                        recommendations.add(element.getAsString());
                    }
                    builder.recommendations(recommendations);
                }

                return builder.build();
            }
        }
        return NativeDiagnosis.builder().build();
    }

    public AgentClient getClient() {
        return client;
    }
}
