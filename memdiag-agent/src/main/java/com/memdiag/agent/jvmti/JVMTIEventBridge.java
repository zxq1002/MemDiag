package com.memdiag.agent.jvmti;

import com.memdiag.agent.instrument.AllocationTransformer;

/**
 * Bridge between JVMTI native events and Java components.
 */
public class JVMTIEventBridge {

    /**
     * Registers native callbacks with the JVMTI agent.
     * This method is implemented in the native library.
     */
    public static native void registerCallbacks();

    /**
     * Called from the native JVMTI agent when an allocation is sampled.
     * 
     * @param size The size of the allocation in bytes
     * @param type The class signature of the allocated object
     */
    public static void onNativeAllocation(long size, String type) {
        AllocationTransformer transformer = AllocationTransformer.getInstance();
        if (transformer != null) {
            // Relays data to AllocationTransformer for recording
            transformer.recordAllocation(size, type);
        }
    }
}
