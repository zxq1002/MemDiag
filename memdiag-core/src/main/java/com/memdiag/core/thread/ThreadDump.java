package com.memdiag.core.thread;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadDump implements Serializable {
    private static final long serialVersionUID = 1L;

    private Instant timestamp;
    private List<ThreadStats> threadStats = new ArrayList<>();

    public ThreadDump() {
    }

    private ThreadDump(Instant timestamp, List<ThreadStats> threadStats) {
        this.timestamp = timestamp;
        this.threadStats = Collections.unmodifiableList(new ArrayList<>(threadStats));
    }

    public static ThreadDump create(Instant timestamp, List<ThreadStats> threadStats) {
        return new ThreadDump(timestamp, threadStats);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<ThreadStats> getThreadStats() {
        return threadStats;
    }

    public void setThreadStats(List<ThreadStats> threadStats) {
        this.threadStats = threadStats;
    }

    public void addThreadStats(ThreadStats stats) {
        this.threadStats.add(stats);
    }

    @Deprecated
    public Map<Long, ThreadInfo> getThreadInfos() {
        Map<Long, ThreadInfo> infos = new HashMap<>();
        for (ThreadStats stats : threadStats) {
            ThreadInfo.Builder builder = ThreadInfo.builder()
                .stats(stats);
            if (stats.getStackTrace() != null) {
                builder.stackTrace(stats.getStackTrace());
            }
            infos.put(stats.getThreadId(), builder.build());
        }
        return Collections.unmodifiableMap(infos);
    }

    @Deprecated
    public ThreadInfo getThreadInfo(long threadId) {
        return getThreadInfos().get(threadId);
    }

    @Deprecated
    public int getThreadCount() {
        return threadStats.size();
    }

    @Deprecated
    public List<ThreadInfo> getThreadsByState(ThreadState state) {
        return getThreadInfos().values().stream()
            .filter(info -> info.getStats().getState() == state)
            .toList();
    }

    @Override
    public String toString() {
        return "ThreadDump{" +
            "timestamp=" + timestamp +
            ", threadCount=" + threadStats.size() +
            '}';
    }

    @Deprecated
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
