package com.memdiag.core.thread;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadDump {
    private final Instant timestamp;
    private final Map<Long, ThreadInfo> threadInfos;

    private ThreadDump(Instant timestamp, Map<Long, ThreadInfo> threadInfos) {
        this.timestamp = timestamp;
        this.threadInfos = Collections.unmodifiableMap(new HashMap<>(threadInfos));
    }

    public static ThreadDump create(Instant timestamp, Map<Long, ThreadInfo> threadInfos) {
        return new ThreadDump(timestamp, threadInfos);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<Long, ThreadInfo> getThreadInfos() {
        return threadInfos;
    }

    public ThreadInfo getThreadInfo(long threadId) {
        return threadInfos.get(threadId);
    }

    public int getThreadCount() {
        return threadInfos.size();
    }

    public List<ThreadInfo> getThreadsByState(ThreadState state) {
        return threadInfos.values().stream()
            .filter(info -> info.getStats().getState() == state)
            .toList();
    }

    @Override
    public String toString() {
        return "ThreadDump{" +
            "timestamp=" + timestamp +
            ", threadCount=" + threadInfos.size() +
            '}';
    }

    public static class ThreadInfo {
        private final ThreadStats stats;
        private final List<StackFrame> stackTrace;
        private final List<Long> lockedMonitorIds;
        private final Long blockedOnMonitorId;
        private final String blockedOnLockOwnerName;
        private final Long blockedOnLockOwnerId;

        private ThreadInfo(Builder builder) {
            this.stats = builder.stats;
            this.stackTrace = Collections.unmodifiableList(new ArrayList<>(builder.stackTrace));
            this.lockedMonitorIds = Collections.unmodifiableList(new ArrayList<>(builder.lockedMonitorIds));
            this.blockedOnMonitorId = builder.blockedOnMonitorId;
            this.blockedOnLockOwnerName = builder.blockedOnLockOwnerName;
            this.blockedOnLockOwnerId = builder.blockedOnLockOwnerId;
        }

        public ThreadStats getStats() {
            return stats;
        }

        public List<StackFrame> getStackTrace() {
            return stackTrace;
        }

        public List<Long> getLockedMonitorIds() {
            return lockedMonitorIds;
        }

        public Long getBlockedOnMonitorId() {
            return blockedOnMonitorId;
        }

        public String getBlockedOnLockOwnerName() {
            return blockedOnLockOwnerName;
        }

        public Long getBlockedOnLockOwnerId() {
            return blockedOnLockOwnerId;
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public String toString() {
            return "ThreadInfo{" +
                "stats=" + stats +
                ", stackDepth=" + stackTrace.size() +
                '}';
        }

        public static class Builder {
            private ThreadStats stats;
            private List<StackFrame> stackTrace = new ArrayList<>();
            private List<Long> lockedMonitorIds = new ArrayList<>();
            private Long blockedOnMonitorId;
            private String blockedOnLockOwnerName;
            private Long blockedOnLockOwnerId;

            public Builder stats(ThreadStats stats) {
                this.stats = stats;
                return this;
            }

            public Builder stackTrace(List<StackFrame> stackTrace) {
                this.stackTrace = new ArrayList<>(stackTrace);
                return this;
            }

            public Builder addStackFrame(StackFrame frame) {
                this.stackTrace.add(frame);
                return this;
            }

            public Builder lockedMonitorIds(List<Long> lockedMonitorIds) {
                this.lockedMonitorIds = new ArrayList<>(lockedMonitorIds);
                return this;
            }

            public Builder addLockedMonitorId(long monitorId) {
                this.lockedMonitorIds.add(monitorId);
                return this;
            }

            public Builder blockedOnMonitorId(Long blockedOnMonitorId) {
                this.blockedOnMonitorId = blockedOnMonitorId;
                return this;
            }

            public Builder blockedOnLockOwnerName(String blockedOnLockOwnerName) {
                this.blockedOnLockOwnerName = blockedOnLockOwnerName;
                return this;
            }

            public Builder blockedOnLockOwnerId(Long blockedOnLockOwnerId) {
                this.blockedOnLockOwnerId = blockedOnLockOwnerId;
                return this;
            }

            public ThreadInfo build() {
                return new ThreadInfo(this);
            }
        }
    }
}
