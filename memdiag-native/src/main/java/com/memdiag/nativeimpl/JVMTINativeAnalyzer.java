package com.memdiag.nativeimpl;

import com.memdiag.core.config.MemDiagConfig;
import com.memdiag.core.nativeapi.LibraryMapping;
import com.memdiag.core.nativeapi.MemoryRegion;
import com.memdiag.core.nativeapi.NativeDiagnosis;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzer;
import com.memdiag.core.nativeapi.NativeMemorySummary;
import com.memdiag.core.nativeapi.NativeDiagnosisEngine;

import java.util.Collections;
import java.util.List;

public class JVMTINativeAnalyzer implements NativeMemoryAnalyzer {

    private final String pid;
    private final int samplingRate;
    private volatile boolean agentAttached = false;
    private volatile boolean trackingEnabled = false;

    // Native methods
    private static native boolean isAgentAttached0();
    private static native boolean attachAgent0(int samplingRate);
    private static native boolean detachAgent0();
    private static native boolean startAllocationTracking0();
    private static native boolean stopAllocationTracking0();
    private static native long getTotalAllocated0();
    private static native long getLiveBytes0();

    public JVMTINativeAnalyzer(String pid, int samplingRate) {
        this.pid = pid;
        this.samplingRate = samplingRate;
    }

    public JVMTINativeAnalyzer(String pid) {
        this(pid, MemDiagConfig.getInstance().getNativeSamplingRate());
    }

    public JVMTINativeAnalyzer() {
        this(String.valueOf(ProcessHandle.current().pid()));
    }

    static {
        try {
            NativeLoader.load();
        } catch (UnsatisfiedLinkError e) {
            // Library not available - will use fallback
        }
    }

    @Override
    public boolean isAvailable() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.contains("linux");
    }

    @Override
    public String getPlatform() {
        return "Linux (JVMTI)";
    }

    @Override
    public boolean requiresAgent() {
        return true;
    }

    @Override
    public boolean isAgentAttached() {
        try {
            return isAgentAttached0() || agentAttached;
        } catch (UnsatisfiedLinkError e) {
            return agentAttached;
        }
    }

    @Override
    public boolean attachAgent() {
        if (agentAttached) {
            return true;
        }
        try {
            if (attachAgent0(samplingRate)) {
                agentAttached = true;
                return true;
            }
        } catch (UnsatisfiedLinkError e) {
            // Fallback for when native library isn't loaded
        }
        return false;
    }

    @Override
    public boolean detachAgent() {
        if (!agentAttached) {
            return true;
        }
        try {
            if (detachAgent0()) {
                agentAttached = false;
                trackingEnabled = false;
                return true;
            }
        } catch (UnsatisfiedLinkError e) {
            // Fallback
        }
        agentAttached = false;
        trackingEnabled = false;
        return true;
    }

    public boolean startAllocationTracking() {
        if (!agentAttached) {
            return false;
        }
        try {
            if (startAllocationTracking0()) {
                trackingEnabled = true;
                return true;
            }
        } catch (UnsatisfiedLinkError e) {
            // Fallback
        }
        return false;
    }

    public boolean stopAllocationTracking() {
        if (!trackingEnabled) {
            return true;
        }
        try {
            if (stopAllocationTracking0()) {
                trackingEnabled = false;
                return true;
            }
        } catch (UnsatisfiedLinkError e) {
            // Fallback
        }
        trackingEnabled = false;
        return true;
    }

    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    public long getTotalAllocated() {
        try {
            return getTotalAllocated0();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    public long getLiveBytes() {
        try {
            return getLiveBytes0();
        } catch (UnsatisfiedLinkError e) {
            return 0;
        }
    }

    @Override
    public NativeMemorySummary getSummary() {
        NativeMemorySummary.Builder builder = NativeMemorySummary.builder();

        if (trackingEnabled) {
            builder.directByteBufferSize(getLiveBytes());
            builder.jniAllocatedSize(getTotalAllocated());
        }

        return builder
            .totalResident(0)
            .totalVirtual(0)
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
        NativeDiagnosisEngine engine = new NativeDiagnosisEngine(this);
        NativeDiagnosis diagnosis = engine.analyze();

        if (trackingEnabled) {
            long totalAllocated = getTotalAllocated();
            long liveBytes = getLiveBytes();

            NativeDiagnosis.Builder builder = NativeDiagnosis.builder();
            for (String finding : diagnosis.getFindings()) {
                builder.addFinding(finding);
            }
            for (String warning : diagnosis.getWarnings()) {
                builder.addWarning(warning);
            }
            for (String recommendation : diagnosis.getRecommendations()) {
                builder.addRecommendation(recommendation);
            }

            builder.addFinding(String.format("Total allocated via tracker: %,d bytes (%.2f MB)",
                totalAllocated, totalAllocated / (1024.0 * 1024.0)));
            builder.addFinding(String.format("Live bytes tracked: %,d bytes (%.2f MB)",
                liveBytes, liveBytes / (1024.0 * 1024.0)));

            return builder.build();
        }

        return diagnosis;
    }
}
