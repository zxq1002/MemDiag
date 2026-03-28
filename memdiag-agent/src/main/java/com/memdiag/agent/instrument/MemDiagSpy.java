package com.memdiag.agent.instrument;

import java.lang.reflect.Method;

/**
 * Bridge class injected into the bootstrap classloader to allow instrumented
 * classes to call back into the agent.
 */
public class MemDiagSpy {
    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);
    
    private static Object allocationTransformer;
    private static Method recordAllocationMethod;
    
    private static Object methodMonitorTransformer;
    private static Method recordMethodEntryMethod;
    private static Method recordMethodExitMethod;

    public static void init(Object allocTransformer, Object methodTransformer) {
        try {
            allocationTransformer = allocTransformer;
            recordAllocationMethod = allocTransformer.getClass().getMethod("recordAllocation", long.class, String.class);
            
            methodMonitorTransformer = methodTransformer;
            recordMethodEntryMethod = methodTransformer.getClass().getMethod("recordMethodEntry", String.class, String.class, String.class);
            recordMethodExitMethod = methodTransformer.getClass().getMethod("recordMethodExit", String.class, String.class, String.class, long.class);
        } catch (Exception e) {
            System.err.println("[MemDiagSpy] Failed to initialize: " + e.getMessage());
        }
    }

    public static void recordAllocation(long size, String type) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (recordAllocationMethod != null) {
                recordAllocationMethod.invoke(allocationTransformer, size, type);
            }
        } catch (Exception e) {
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    public static void recordMethodEntry(String className, String methodName, String descriptor) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (recordMethodEntryMethod != null) {
                recordMethodEntryMethod.invoke(methodMonitorTransformer, className, methodName, descriptor);
            }
        } catch (Exception e) {
        } finally {
            IN_PROGRESS.set(false);
        }
    }

    public static void recordMethodExit(String className, String methodName, String descriptor, long durationNanos) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (recordMethodExitMethod != null) {
                recordMethodExitMethod.invoke(methodMonitorTransformer, className, methodName, descriptor, durationNanos);
            }
        } catch (Exception e) {
        } finally {
            IN_PROGRESS.set(false);
        }
    }
}
