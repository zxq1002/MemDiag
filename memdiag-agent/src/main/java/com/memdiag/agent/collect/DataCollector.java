package com.memdiag.agent.collect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and aggregates allocation statistics.
 */
public class DataCollector {

    private final AllocationRingBuffer ringBuffer;

    // Aggregated statistics
    private final AtomicLong totalAllocated = new AtomicLong(0);
    private final AtomicLong allocationCount = new AtomicLong(0);

    // Per-type statistics
    private final ConcurrentHashMap<String, TypeStats> typeStats = new ConcurrentHashMap<>();

    // Time window statistics (last 1 minute, 5 minutes, 15 minutes)
    private static final long[] WINDOWS_MS = {60_000, 300_000, 900_000};

    public DataCollector(int bufferSize) {
        this.ringBuffer = new AllocationRingBuffer(bufferSize);
    }

    public DataCollector() {
        this(10000);
    }

    /**
     * Record an allocation event.
     */
    public void recordAllocation(AllocationEvent event) {
        ringBuffer.add(event);
        totalAllocated.addAndGet(event.getSize());
        allocationCount.incrementAndGet();

        // Update per-type stats
        String typeName = event.getTypeName();
        typeStats.computeIfAbsent(typeName, k -> new TypeStats(typeName))
                .record(event.getSize());
    }

    /**
     * Record a simple allocation without full event details.
     */
    public void recordAllocation(long size, String typeName) {
        AllocationEvent.AllocationType type = parseType(typeName);
        recordAllocation(new AllocationEvent(size, type, typeName));
    }

    private AllocationEvent.AllocationType parseType(String typeName) {
        if (typeName == null) {
            return AllocationEvent.AllocationType.OTHER;
        }
        if (typeName.contains("byte[]")) {
            return AllocationEvent.AllocationType.BYTE_ARRAY;
        }
        if (typeName.contains("int[]")) {
            return AllocationEvent.AllocationType.INT_ARRAY;
        }
        if (typeName.contains("long[]")) {
            return AllocationEvent.AllocationType.LONG_ARRAY;
        }
        if (typeName.contains("DirectByteBuffer")) {
            return AllocationEvent.AllocationType.DIRECT_BYTE_BUFFER;
        }
        if (typeName.contains("ByteBuffer")) {
            return AllocationEvent.AllocationType.HEAP_BYTE_BUFFER;
        }
        if (typeName.contains("[]")) {
            return AllocationEvent.AllocationType.OBJECT_ARRAY;
        }
        return AllocationEvent.AllocationType.OTHER;
    }

    // ========== Getters for statistics ==========

    public AllocationRingBuffer getRingBuffer() {
        return ringBuffer;
    }

    public long getTotalAllocated() {
        return totalAllocated.get();
    }

    public long getAllocationCount() {
        return allocationCount.get();
    }

    /**
     * Get the average allocation size.
     */
    public double getAverageAllocationSize() {
        long count = allocationCount.get();
        if (count == 0) {
            return 0;
        }
        return (double) totalAllocated.get() / count;
    }

    /**
     * Get allocation rate for the specified time window.
     */
    public long getAllocationRateBytesPerSec(long windowMs) {
        long totalBytes = ringBuffer.getTotalBytesInWindow(windowMs);
        return totalBytes / (windowMs / 1000);
    }

    /**
     * Get statistics for all time windows.
     */
    public Map<String, Long> getWindowStats() {
        Map<String, Long> stats = new HashMap<>();
        for (long windowMs : WINDOWS_MS) {
            String key = (windowMs / 1000) + "s";
            stats.put(key, getAllocationRateBytesPerSec(windowMs));
        }
        return stats;
    }

    /**
     * Get top N allocation types by total size.
     */
    public List<TypeStats> getTopTypesBySize(int limit) {
        List<TypeStats> sorted = new ArrayList<>(typeStats.values());
        sorted.sort((a, b) -> Long.compare(b.getTotalSize(), a.getTotalSize()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    /**
     * Get top N allocation types by count.
     */
    public List<TypeStats> getTopTypesByCount(int limit) {
        List<TypeStats> sorted = new ArrayList<>(typeStats.values());
        sorted.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    /**
     * Get all type statistics.
     */
    public Map<String, TypeStats> getAllTypeStats() {
        return new HashMap<>(typeStats);
    }

    /**
     * Get recent allocation events.
     */
    public List<AllocationEvent> getRecentEvents(int limit) {
        return ringBuffer.getRecent(limit);
    }

    /**
     * Clear all collected data.
     */
    public void clear() {
        ringBuffer.clear();
        totalAllocated.set(0);
        allocationCount.set(0);
        typeStats.clear();
    }

    /**
     * Convert to a map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalAllocated", totalAllocated.get());
        map.put("allocationCount", allocationCount.get());
        map.put("averageSize", getAverageAllocationSize());
        map.put("windowRates", getWindowStats());
        map.put("typeCount", typeStats.size());
        map.put("bufferSize", ringBuffer.size());
        map.put("bufferCapacity", ringBuffer.getCapacity());
        return map;
    }

    /**
     * Statistics for a specific allocation type.
     */
    public static class TypeStats {
        private final String typeName;
        private final AtomicLong totalSize = new AtomicLong(0);
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong maxSize = new AtomicLong(0);

        public TypeStats(String typeName) {
            this.typeName = typeName;
        }

        public void record(long size) {
            totalSize.addAndGet(size);
            count.incrementAndGet();

            // Update max size
            long currentMax;
            do {
                currentMax = maxSize.get();
                if (size <= currentMax) {
                    break;
                }
            } while (!maxSize.compareAndSet(currentMax, size));
        }

        public String getTypeName() {
            return typeName;
        }

        public long getTotalSize() {
            return totalSize.get();
        }

        public long getCount() {
            return count.get();
        }

        public long getMaxSize() {
            return maxSize.get();
        }

        public double getAverageSize() {
            long c = count.get();
            if (c == 0) {
                return 0;
            }
            return (double) totalSize.get() / c;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("typeName", typeName);
            map.put("totalSize", totalSize.get());
            map.put("count", count.get());
            map.put("maxSize", maxSize.get());
            map.put("averageSize", getAverageSize());
            return map;
        }
    }
}
