package com.memdiag.agent.instrument;

/**
 * Bridge class injected into the bootstrap classloader to allow instrumented
 * classes to call back into the agent.
 */
public class MemDiagSpy {
    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);
    private static volatile AllocationTransformer allocationTransformer;
    private static volatile MethodMonitorTransformer methodMonitorTransformer;

    public static void init(AllocationTransformer allocTransformer, MethodMonitorTransformer methodTransformer) {
        allocationTransformer = allocTransformer;
        methodMonitorTransformer = methodTransformer;
    }

    public static void recordAllocation(long size, String type) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (allocationTransformer != null) {
                allocationTransformer.recordAllocation(size, type);
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    public static void recordMethodEntry(String className, String methodName, String descriptor) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (methodMonitorTransformer != null) {
                methodMonitorTransformer.recordMethodEntry(className, methodName, descriptor);
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    public static void recordMethodExit(String className, String methodName, String descriptor, long durationNanos) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (methodMonitorTransformer != null) {
                methodMonitorTransformer.recordMethodExit(className, methodName, descriptor, durationNanos);
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }
}
