package com.memdiag.core.thread;

public class ThreadStats {
    private final long threadId;
    private final String threadName;
    private final ThreadState state;
    private final long blockedCount;
    private final long blockedTime;
    private final long waitedCount;
    private final long waitedTime;
    private final long allocatedBytes;

    private ThreadStats(Builder builder) {
        this.threadId = builder.threadId;
        this.threadName = builder.threadName;
        this.state = builder.state;
        this.blockedCount = builder.blockedCount;
        this.blockedTime = builder.blockedTime;
        this.waitedCount = builder.waitedCount;
        this.waitedTime = builder.waitedTime;
        this.allocatedBytes = builder.allocatedBytes;
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public ThreadState getState() {
        return state;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public long getBlockedTime() {
        return blockedTime;
    }

    public long getWaitedCount() {
        return waitedCount;
    }

    public long getWaitedTime() {
        return waitedTime;
    }

    public long getAllocatedBytes() {
        return allocatedBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ThreadStats{" +
            "threadId=" + threadId +
            ", threadName='" + threadName + '\'' +
            ", state=" + state +
            '}';
    }

    public static class Builder {
        private long threadId;
        private String threadName;
        private ThreadState state = ThreadState.UNKNOWN;
        private long blockedCount;
        private long blockedTime;
        private long waitedCount;
        private long waitedTime;
        private long allocatedBytes;

        public Builder threadId(long threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder state(ThreadState state) {
            this.state = state;
            return this;
        }

        public Builder blockedCount(long blockedCount) {
            this.blockedCount = blockedCount;
            return this;
        }

        public Builder blockedTime(long blockedTime) {
            this.blockedTime = blockedTime;
            return this;
        }

        public Builder waitedCount(long waitedCount) {
            this.waitedCount = waitedCount;
            return this;
        }

        public Builder waitedTime(long waitedTime) {
            this.waitedTime = waitedTime;
            return this;
        }

        public Builder allocatedBytes(long allocatedBytes) {
            this.allocatedBytes = allocatedBytes;
            return this;
        }

        public ThreadStats build() {
            return new ThreadStats(this);
        }
    }
}
