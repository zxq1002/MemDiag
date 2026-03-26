package com.memdiag.core.nativeapi;

import java.util.List;

public interface NativeMemoryAnalyzer {

    boolean isAvailable();

    String getPlatform();

    boolean requiresAgent();

    boolean isAgentAttached();

    boolean attachAgent();

    boolean detachAgent();

    boolean startAllocationTracking();

    boolean stopAllocationTracking();

    boolean isTrackingEnabled();

    long getTotalAllocated();

    long getLiveBytes();

    NativeMemorySummary getSummary();

    List<MemoryRegion> getMemoryRegions();

    List<LibraryMapping> getLibraryMappings();

    NativeDiagnosis analyzeNativeLeaks();
}
