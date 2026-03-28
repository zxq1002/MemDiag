package com.memdiag.agent.collect;

/**
 * Represents a single allocation event.
 */
public class AllocationEvent {

    public enum AllocationType {
        BYTE_ARRAY,
        INT_ARRAY,
        LONG_ARRAY,
        OBJECT_ARRAY,
        DIRECT_BYTE_BUFFER,
        HEAP_BYTE_BUFFER,
        OTHER
    }

    private final long timestamp;
    private final long size;
    private final AllocationType type;
    private final String typeName;
    private final int stackTraceHash;
    private final long threadId;

    public AllocationEvent(long size, AllocationType type, String typeName, int stackTraceHash, long threadId) {
        this.timestamp = System.currentTimeMillis();
        this.size = size;
        this.type = type;
        this.typeName = typeName;
        this.stackTraceHash = stackTraceHash;
        this.threadId = threadId;
    }

    public AllocationEvent(long size, AllocationType type, String typeName) {
        this(size, type, typeName, 0, Thread.currentThread().getId());
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSize() {
        return size;
    }

    public AllocationType getType() {
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public int getStackTraceHash() {
        return stackTraceHash;
    }

    public long getThreadId() {
        return threadId;
    }

    public long getAgeMs() {
        return System.currentTimeMillis() - timestamp;
    }

    @Override
    public String toString() {
        return "AllocationEvent{" +
                "timestamp=" + timestamp +
                ", size=" + size +
                ", type=" + type +
                ", typeName='" + typeName + '\'' +
                ", threadId=" + threadId +
                '}';
    }
}
