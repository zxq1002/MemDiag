package com.memdiag.core.nativeapi;

import java.util.Collections;
import java.util.List;

public class NoOpNativeAnalyzer implements NativeMemoryAnalyzer {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getPlatform() {
        return System.getProperty("os.name");
    }

    @Override
    public boolean requiresAgent() {
        return false;
    }

    @Override
    public boolean isAgentAttached() {
        return false;
    }

    @Override
    public boolean attachAgent() {
        return false;
    }

    @Override
    public boolean detachAgent() {
        return false;
    }

    @Override
    public NativeMemorySummary getSummary() {
        return NativeMemorySummary.builder()
            .totalResident(0)
            .totalVirtual(0)
            .directByteBufferSize(0)
            .jniAllocatedSize(0)
            .threadStackSize(0)
            .codeCacheSize(0)
            .build();
    }

    @Override
    public List<MemoryRegion> getMemoryRegions() {
        return Collections.emptyList();
    }

    @Override
    public List<LibraryMapping> getLibraryMappings() {
        return Collections.emptyList();
    }

    @Override
    public NativeDiagnosis analyzeNativeLeaks() {
        return NativeDiagnosis.builder()
            .addWarning("Native memory analysis is not available on this platform")
            .addRecommendation("Check platform compatibility or enable native module")
            .build();
    }
}
