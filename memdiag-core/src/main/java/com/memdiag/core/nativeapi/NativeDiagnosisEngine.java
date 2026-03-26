package com.memdiag.core.nativeapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NativeDiagnosisEngine {

    private static final long LARGE_REGION_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static final long ANONYMOUS_REGION_GROWTH_THRESHOLD = 50 * 1024 * 1024; // 50MB

    private final NativeMemoryAnalyzer analyzer;

    public NativeDiagnosisEngine(NativeMemoryAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    public NativeDiagnosis analyze() {
        NativeDiagnosis.Builder diagnosis = NativeDiagnosis.builder();

        try {
            NativeMemorySummary summary = analyzer.getSummary();
            List<MemoryRegion> regions = analyzer.getMemoryRegions();
            List<LibraryMapping> mappings = analyzer.getLibraryMappings();

            // Analyze memory summary
            analyzeSummary(diagnosis, summary);

            // Analyze memory regions
            analyzeRegions(diagnosis, regions);

            // Analyze library mappings
            analyzeLibraryMappings(diagnosis, mappings);

            // Check for DirectByteBuffer issues
            analyzeDirectByteBuffers(diagnosis, summary);

            // Check for thread stack issues
            analyzeThreadStacks(diagnosis, summary);

            // Add recommendations
            addRecommendations(diagnosis, summary, regions);

        } catch (Exception e) {
            diagnosis.addWarning("Analysis encountered errors: " + e.getMessage());
        }

        return diagnosis.build();
    }

    private void analyzeSummary(NativeDiagnosis.Builder diagnosis, NativeMemorySummary summary) {
        if (summary.getTotalResident() > 0) {
            diagnosis.addFinding(String.format("Total resident memory: %,d bytes (%.2f MB)",
                summary.getTotalResident(),
                summary.getTotalResident() / (1024.0 * 1024.0)));
        }

        if (summary.getTotalVirtual() > 0) {
            diagnosis.addFinding(String.format("Total virtual memory: %,d bytes (%.2f MB)",
                summary.getTotalVirtual(),
                summary.getTotalVirtual() / (1024.0 * 1024.0)));
        }
    }

    private void analyzeRegions(NativeDiagnosis.Builder diagnosis, List<MemoryRegion> regions) {
        if (regions.isEmpty()) {
            return;
        }

        long totalAnonymousSize = 0;
        Map<String, Long> regionTypeStats = new HashMap<>();

        for (MemoryRegion region : regions) {
            String mappingFile = region.getMappingFile();
            boolean isAnonymous = mappingFile == null || mappingFile.isEmpty();

            if (isAnonymous) {
                totalAnonymousSize += region.getResidentSize();
            }

            if (region.getResidentSize() > LARGE_REGION_THRESHOLD) {
                String desc = isAnonymous ? "anonymous region" : mappingFile;
                diagnosis.addWarning(String.format("Large memory region: %,d bytes (%.2f MB) - %s",
                    region.getResidentSize(),
                    region.getResidentSize() / (1024.0 * 1024.0),
                    desc));
            }

            String type = region.getRegionType();
            if (type != null && !type.isEmpty()) {
                regionTypeStats.merge(type, region.getResidentSize(), Long::sum);
            }
        }

        if (totalAnonymousSize > ANONYMOUS_REGION_GROWTH_THRESHOLD) {
            diagnosis.addWarning(String.format("Large anonymous memory usage: %,d bytes (%.2f MB) - possible native heap fragmentation",
                totalAnonymousSize,
                totalAnonymousSize / (1024.0 * 1024.0)));
        }

        diagnosis.addFinding(String.format("Total memory regions: %d", regions.size()));
    }

    private void analyzeLibraryMappings(NativeDiagnosis.Builder diagnosis, List<LibraryMapping> mappings) {
        if (mappings.isEmpty()) {
            return;
        }

        Map<String, Long> librarySizes = new HashMap<>();

        for (LibraryMapping mapping : mappings) {
            String path = mapping.getPathname();
            if (path != null && !path.isEmpty()) {
                long size = mapping.getEndAddress() - mapping.getStartAddress();
                librarySizes.merge(path, size, Long::sum);
            }
        }

        diagnosis.addFinding(String.format("Loaded native libraries: %d", librarySizes.size()));

        List<Map.Entry<String, Long>> topLibraries = new ArrayList<>(librarySizes.entrySet());
        topLibraries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        int count = 0;
        for (Map.Entry<String, Long> entry : topLibraries) {
            if (count++ >= 5) break;
            String libName = extractLibraryName(entry.getKey());
            diagnosis.addFinding(String.format("  %s: %,d bytes (%.2f MB)",
                libName,
                entry.getValue(),
                entry.getValue() / (1024.0 * 1024.0)));
        }
    }

    private void analyzeDirectByteBuffers(NativeDiagnosis.Builder diagnosis, NativeMemorySummary summary) {
        long dbbSize = summary.getDirectByteBufferSize();
        if (dbbSize > 0) {
            diagnosis.addFinding(String.format("DirectByteBuffer usage: %,d bytes (%.2f MB)",
                dbbSize,
                dbbSize / (1024.0 * 1024.0)));

            if (dbbSize > 256 * 1024 * 1024) { // 256MB
                diagnosis.addWarning("High DirectByteBuffer usage - check for unreleased buffers");
            }
        }
    }

    private void analyzeThreadStacks(NativeDiagnosis.Builder diagnosis, NativeMemorySummary summary) {
        long stackSize = summary.getThreadStackSize();
        if (stackSize > 0) {
            diagnosis.addFinding(String.format("Thread stack usage: %,d bytes (%.2f MB)",
                stackSize,
                stackSize / (1024.0 * 1024.0)));
        }
    }

    private void addRecommendations(NativeDiagnosis.Builder diagnosis,
                                     NativeMemorySummary summary,
                                     List<MemoryRegion> regions) {
        // Check if NMT is available/should be enabled
        diagnosis.addRecommendation("Consider enabling NativeMemoryTracking with -XX:NativeMemoryTracking=summary for detailed JVM native memory breakdown");

        // Check for large anonymous regions
        boolean hasLargeAnonymous = false;
        for (MemoryRegion region : regions) {
            String mappingFile = region.getMappingFile();
            if ((mappingFile == null || mappingFile.isEmpty()) &&
                region.getResidentSize() > 50 * 1024 * 1024) {
                hasLargeAnonymous = true;
                break;
            }
        }

        if (hasLargeAnonymous) {
            diagnosis.addRecommendation("Large anonymous regions detected - consider using JVMTI agent to track native allocations");
        }

        // Check DirectByteBuffer usage
        if (summary.getDirectByteBufferSize() > 100 * 1024 * 1024) {
            diagnosis.addRecommendation("High DirectByteBuffer usage - review buffer allocation patterns and ensure timely release");
        }

        // General recommendations
        diagnosis.addRecommendation("Monitor RSS growth over time to identify memory leaks");
        diagnosis.addRecommendation("Use /proc/<pid>/smaps to get detailed memory region information");
    }

    private String extractLibraryName(String path) {
        if (path == null || path.isEmpty()) {
            return "anonymous";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }
}
