package com.memdiag.core.nativeapi;

import java.util.List;

public class AllocationSite {
    private final List<NativeStackFrame> stackTrace;
    private final long allocationCount;
    private final long totalBytesAllocated;
    private final long bytesStillLive;
    private final long freeCount;
    private final long bytesFreed;

    private AllocationSite(Builder builder) {
        this.stackTrace = builder.stackTrace;
        this.allocationCount = builder.allocationCount;
        this.totalBytesAllocated = builder.totalBytesAllocated;
        this.bytesStillLive = builder.bytesStillLive;
        this.freeCount = builder.freeCount;
        this.bytesFreed = builder.bytesFreed;
    }

    public List<NativeStackFrame> getStackTrace() { return stackTrace; }
    public long getAllocationCount() { return allocationCount; }
    public long getTotalBytesAllocated() { return totalBytesAllocated; }
    public long getBytesStillLive() { return bytesStillLive; }
    public long getFreeCount() { return freeCount; }
    public long getBytesFreed() { return bytesFreed; }

    public double getLiveRatio() {
        if (totalBytesAllocated == 0) {
            return 0;
        }
        return (double) bytesStillLive / totalBytesAllocated;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSignature() {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return "unknown";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(5, stackTrace.size()); i++) {
            if (i > 0) sb.append("; ");
            NativeStackFrame frame = stackTrace.get(i);
            if (frame.getFunctionName() != null) {
                sb.append(frame.getFunctionName());
            } else {
                sb.append(String.format("0x%016x", frame.getInstructionAddress()));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("AllocationSite{allocations=%d, totalBytes=%,d, liveBytes=%,d, liveRatio=%.2f%%}",
            allocationCount, totalBytesAllocated, bytesStillLive, getLiveRatio() * 100);
    }

    public static class Builder {
        private List<NativeStackFrame> stackTrace;
        private long allocationCount;
        private long totalBytesAllocated;
        private long bytesStillLive;
        private long freeCount;
        private long bytesFreed;

        public Builder stackTrace(List<NativeStackFrame> stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public Builder allocationCount(long allocationCount) {
            this.allocationCount = allocationCount;
            return this;
        }

        public Builder totalBytesAllocated(long totalBytesAllocated) {
            this.totalBytesAllocated = totalBytesAllocated;
            return this;
        }

        public Builder bytesStillLive(long bytesStillLive) {
            this.bytesStillLive = bytesStillLive;
            return this;
        }

        public Builder freeCount(long freeCount) {
            this.freeCount = freeCount;
            return this;
        }

        public Builder bytesFreed(long bytesFreed) {
            this.bytesFreed = bytesFreed;
            return this;
        }

        public AllocationSite build() {
            return new AllocationSite(this);
        }
    }
}
