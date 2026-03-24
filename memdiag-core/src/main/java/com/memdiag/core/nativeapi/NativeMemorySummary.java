package com.memdiag.core.nativeapi;

import java.util.HashMap;
import java.util.Map;

public class NativeMemorySummary {
    private final long totalResident;
    private final long totalVirtual;
    private final long directByteBufferSize;
    private final long jniAllocatedSize;
    private final long threadStackSize;
    private final long codeCacheSize;
    private final Map<String, Long> breakdownByCategory;

    private NativeMemorySummary(Builder builder) {
        this.totalResident = builder.totalResident;
        this.totalVirtual = builder.totalVirtual;
        this.directByteBufferSize = builder.directByteBufferSize;
        this.jniAllocatedSize = builder.jniAllocatedSize;
        this.threadStackSize = builder.threadStackSize;
        this.codeCacheSize = builder.codeCacheSize;
        this.breakdownByCategory = new HashMap<>(builder.breakdownByCategory);
    }

    public long getTotalResident() { return totalResident; }
    public long getTotalVirtual() { return totalVirtual; }
    public long getDirectByteBufferSize() { return directByteBufferSize; }
    public long getJniAllocatedSize() { return jniAllocatedSize; }
    public long getThreadStackSize() { return threadStackSize; }
    public long getCodeCacheSize() { return codeCacheSize; }
    public Map<String, Long> getBreakdownByCategory() {
        return new HashMap<>(breakdownByCategory);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalResident;
        private long totalVirtual;
        private long directByteBufferSize;
        private long jniAllocatedSize;
        private long threadStackSize;
        private long codeCacheSize;
        private Map<String, Long> breakdownByCategory = new HashMap<>();

        public Builder totalResident(long totalResident) {
            this.totalResident = totalResident;
            return this;
        }

        public Builder totalVirtual(long totalVirtual) {
            this.totalVirtual = totalVirtual;
            return this;
        }

        public Builder directByteBufferSize(long directByteBufferSize) {
            this.directByteBufferSize = directByteBufferSize;
            return this;
        }

        public Builder jniAllocatedSize(long jniAllocatedSize) {
            this.jniAllocatedSize = jniAllocatedSize;
            return this;
        }

        public Builder threadStackSize(long threadStackSize) {
            this.threadStackSize = threadStackSize;
            return this;
        }

        public Builder codeCacheSize(long codeCacheSize) {
            this.codeCacheSize = codeCacheSize;
            return this;
        }

        public Builder breakdownByCategory(Map<String, Long> breakdownByCategory) {
            this.breakdownByCategory = new HashMap<>(breakdownByCategory);
            return this;
        }

        public Builder addCategory(String category, long size) {
            this.breakdownByCategory.put(category, size);
            return this;
        }

        public NativeMemorySummary build() {
            return new NativeMemorySummary(this);
        }
    }
}
