package com.memdiag.core.thread;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ThreadStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private long threadId;
    private String threadName;
    private ThreadState state;
    private List<StackFrame> stackTrace = new ArrayList<>();
    private long blockedCount;
    private long blockedTime;
    private long waitedCount;
    private long waitedTime;
    private long allocatedBytes;

    public ThreadStats() {
    }

    public ThreadStats(long threadId, String threadName, ThreadState state) {
        this.threadId = threadId;
        this.threadName = threadName;
        this.state = state;
    }

    private ThreadStats(Builder builder) {
        this.threadId = builder.threadId;
        this.threadName = builder.threadName;
        this.state = builder.state;
        this.blockedCount = builder.blockedCount;
        this.blockedTime = builder.blockedTime;
        this.waitedCount = builder.waitedCount;
        this.waitedTime = builder.waitedTime;
        this.allocatedBytes = builder.allocatedBytes;
        if (builder.stackTrace != null) {
            this.stackTrace = new ArrayList<>(builder.stackTrace);
        }
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public ThreadState getState() {
        return state;
    }

    public void setState(ThreadState state) {
        this.state = state;
    }

    public List<StackFrame> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<StackFrame> stackTrace) {
        this.stackTrace = stackTrace;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    public long getBlockedTime() {
        return blockedTime;
    }

    public void setBlockedTime(long blockedTime) {
        this.blockedTime = blockedTime;
    }

    public long getWaitedCount() {
        return waitedCount;
    }

    public void setWaitedCount(long waitedCount) {
        this.waitedCount = waitedCount;
    }

    public long getWaitedTime() {
        return waitedTime;
    }

    public void setWaitedTime(long waitedTime) {
        this.waitedTime = waitedTime;
    }

    public long getAllocatedBytes() {
        return allocatedBytes;
    }

    public void setAllocatedBytes(long allocatedBytes) {
        this.allocatedBytes = allocatedBytes;
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
        private List<StackFrame> stackTrace;
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

        public Builder stackTrace(List<StackFrame> stackTrace) {
            this.stackTrace = stackTrace;
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
