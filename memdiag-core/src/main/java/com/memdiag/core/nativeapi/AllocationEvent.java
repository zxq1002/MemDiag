package com.memdiag.core.nativeapi;

import java.time.Instant;
import java.util.List;

public class AllocationEvent {
    public enum Type {
        ALLOCATE,
        FREE,
        REALLOCATE
    }

    private final Type type;
    private final long address;
    private final long size;
    private final long oldAddress;
    private final long oldSize;
    private final Instant timestamp;
    private final long threadId;
    private final String threadName;
    private final List<NativeStackFrame> stackTrace;

    private AllocationEvent(Builder builder) {
        this.type = builder.type;
        this.address = builder.address;
        this.size = builder.size;
        this.oldAddress = builder.oldAddress;
        this.oldSize = builder.oldSize;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.threadId = builder.threadId;
        this.threadName = builder.threadName;
        this.stackTrace = builder.stackTrace;
    }

    public Type getType() { return type; }
    public long getAddress() { return address; }
    public long getSize() { return size; }
    public long getOldAddress() { return oldAddress; }
    public long getOldSize() { return oldSize; }
    public Instant getTimestamp() { return timestamp; }
    public long getThreadId() { return threadId; }
    public String getThreadName() { return threadName; }
    public List<NativeStackFrame> getStackTrace() { return stackTrace; }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        switch (type) {
            case ALLOCATE:
                return String.format("ALLOCATE 0x%016x %,d bytes", address, size);
            case FREE:
                return String.format("FREE     0x%016x", address);
            case REALLOCATE:
                return String.format("REALLOC  0x%016x -> 0x%016x (%,d -> %,d bytes)",
                    oldAddress, address, oldSize, size);
            default:
                return "UNKNOWN";
        }
    }

    public static class Builder {
        private Type type;
        private long address;
        private long size;
        private long oldAddress;
        private long oldSize;
        private Instant timestamp;
        private long threadId;
        private String threadName;
        private List<NativeStackFrame> stackTrace;

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public Builder address(long address) {
            this.address = address;
            return this;
        }

        public Builder size(long size) {
            this.size = size;
            return this;
        }

        public Builder oldAddress(long oldAddress) {
            this.oldAddress = oldAddress;
            return this;
        }

        public Builder oldSize(long oldSize) {
            this.oldSize = oldSize;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder threadId(long threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder stackTrace(List<NativeStackFrame> stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public AllocationEvent build() {
            return new AllocationEvent(this);
        }
    }
}
