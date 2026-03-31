package com.memdiag.nativeimpl;

import java.util.Map;

/**
 * Native method declarations for JVMTI analyzer.
 * This class is called from the native JVMTI agent.
 */
public class JVMTINativeAnalyzer {

    // ==================== GC Root Analysis ====================

    /**
     * Check if JVMTI agent is attached.
     */
    public static native boolean isAgentAttached0();

    /**
     * Attach JVMTI agent with given sampling rate.
     */
    public static native boolean attachAgent0(int samplingRate);

    /**
     * Detach JVMTI agent.
     */
    public static native boolean detachAgent0();

    /**
     * Start allocation tracking.
     */
    public static native boolean startAllocationTracking0();

    /**
     * Stop allocation tracking.
     */
    public static native boolean stopAllocationTracking0();

    /**
     * Get total allocated bytes.
     */
    public static native long getTotalAllocated0();

    /**
     * Get live bytes.
     */
    public static native long getLiveBytes0();

    // ==================== GC Root Methods ====================

    /**
     * Get GC Root statistics as a Map.
     * Keys are GcRootType enum names, values are counts.
     */
    public static native Map<String, Long> getGcRootStats0();

    /**
     * Start GC Root tracking.
     */
    public static native boolean startGcRootTracking0();

    /**
     * Stop GC Root tracking.
     */
    public static native boolean stopGcRootTracking0();
}
