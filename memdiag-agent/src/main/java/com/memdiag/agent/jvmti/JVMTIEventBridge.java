package com.memdiag.agent.jvmti;

import com.memdiag.agent.collect.DataCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge between JVMTI native events and Java components.
 * Receives events from JVMTI native layer and forwards to DataCollector.
 */
public class JVMTIEventBridge {

    private final DataCollector dataCollector;
    private final List<JVMTIEventListener> listeners = new ArrayList<>();

    // Event counters
    private final Map<String, Long> eventCounts = new ConcurrentHashMap<>();

    public JVMTIEventBridge(DataCollector dataCollector) {
        this.dataCollector = dataCollector;
    }

    /**
     * Add an event listener.
     */
    public void addListener(JVMTIEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove an event listener.
     */
    public void removeListener(JVMTIEventListener listener) {
        listeners.remove(listener);
    }

    // ========== Event handlers called from JNI ==========

    /**
     * Called when a GC starts.
     */
    public static void onGcStart(long timestamp) {
        getInstance().notifyGcStart(timestamp);
    }

    /**
     * Called when a GC finishes.
     */
    public static void onGcFinish(long timestamp, long durationMs) {
        getInstance().notifyGcFinish(timestamp, durationMs);
    }

    /**
     * Called when a native memory allocation occurs.
     */
    public static void onNativeAllocation(long size, String type) {
        getInstance().notifyNativeAllocation(size, type);
    }

    /**
     * Called when a native memory free occurs.
     */
    public static void onNativeFree(long size, String type) {
        getInstance().notifyNativeFree(size, type);
    }

    /**
     * Called when a thread is created.
     */
    public static void onThreadStart(long threadId, String threadName) {
        getInstance().notifyThreadStart(threadId, threadName);
    }

    /**
     * Called when a thread exits.
     */
    public static void onThreadEnd(long threadId) {
        getInstance().notifyThreadEnd(threadId);
    }

    // ========== Internal notification methods ==========

    private void notifyGcStart(long timestamp) {
        incrementEventCount("gc.start");
        for (JVMTIEventListener listener : listeners) {
            try {
                listener.onGcStart(timestamp);
            } catch (Exception e) {
                System.err.println("[MemDiag] Error in GC start listener: " + e.getMessage());
            }
        }
    }

    private void notifyGcFinish(long timestamp, long durationMs) {
        incrementEventCount("gc.finish");
        for (JVMTIEventListener listener : listeners) {
            try {
                listener.onGcFinish(timestamp, durationMs);
            } catch (Exception e) {
                System.err.println("[MemDiag] Error in GC finish listener: " + e.getMessage());
            }
        }
    }

    private void notifyNativeAllocation(long size, String type) {
        incrementEventCount("native.allocation");

        // Forward to data collector
        if (dataCollector != null) {
            dataCollector.recordAllocation(size, "native:" + type);
        }

        for (JVMTIEventListener listener : listeners) {
            try {
                listener.onNativeAllocation(size, type);
            } catch (Exception e) {
                System.err.println("[MemDiag] Error in native allocation listener: " + e.getMessage());
            }
        }
    }

    private void notifyNativeFree(long size, String type) {
        incrementEventCount("native.free");
        for (JVMTIEventListener listener : listeners) {
            try {
                listener.onNativeFree(size, type);
            } catch (Exception e) {
                System.err.println("[MemDiag] Error in native free listener: " + e.getMessage());
            }
        }
    }

    private void notifyThreadStart(long threadId, String threadName) {
        incrementEventCount("thread.start");
        for (JVMTIEventListener listener : listeners) {
            try {
                listener.onThreadStart(threadId, threadName);
            } catch (Exception e) {
                System.err.println("[MemDiag] Error in thread start listener: " + e.getMessage());
            }
        }
    }

    private void notifyThreadEnd(long threadId) {
        incrementEventCount("thread.end");
        for (JVMTIEventListener listener : listeners) {
            try {
                listener.onThreadEnd(threadId);
            } catch (Exception e) {
                System.err.println("[MemDiag] Error in thread end listener: " + e.getMessage());
            }
        }
    }

    private void incrementEventCount(String eventType) {
        eventCounts.merge(eventType, 1L, Long::sum);
    }

    // ========== Getters ==========

    public Map<String, Long> getEventCounts() {
        return new ConcurrentHashMap<>(eventCounts);
    }

    public long getEventCount(String eventType) {
        return eventCounts.getOrDefault(eventType, 0L);
    }

    public void resetEventCounts() {
        eventCounts.clear();
    }

    // ========== Singleton ==========

    private static volatile JVMTIEventBridge instance;

    public static JVMTIEventBridge getInstance() {
        if (instance == null) {
            synchronized (JVMTIEventBridge.class) {
                if (instance == null) {
                    instance = new JVMTIEventBridge(null);
                }
            }
        }
        return instance;
    }

    public static void setInstance(JVMTIEventBridge bridge) {
        instance = bridge;
    }

    /**
     * Listener interface for JVMTI events.
     */
    public interface JVMTIEventListener {
        default void onGcStart(long timestamp) {}
        default void onGcFinish(long timestamp, long durationMs) {}
        default void onNativeAllocation(long size, String type) {}
        default void onNativeFree(long size, String type) {}
        default void onThreadStart(long threadId, String threadName) {}
        default void onThreadEnd(long threadId) {}
    }
}
