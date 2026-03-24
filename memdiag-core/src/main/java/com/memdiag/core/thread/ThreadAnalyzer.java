package com.memdiag.core.thread;

import com.memdiag.core.util.JmxClient;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadAnalyzer {
    private final ThreadMXBean threadMXBean;

    public ThreadAnalyzer(JmxClient jmxClient) {
        try {
            this.threadMXBean = ManagementFactory.newPlatformMXBeanProxy(
                jmxClient.getConnection(),
                ManagementFactory.THREAD_MXBEAN_NAME,
                ThreadMXBean.class
            );
            if (threadMXBean.isThreadCpuTimeSupported()) {
                threadMXBean.setThreadCpuTimeEnabled(true);
            }
            if (threadMXBean.isThreadContentionMonitoringSupported()) {
                threadMXBean.setThreadContentionMonitoringEnabled(true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ThreadAnalyzer", e);
        }
    }

    public ThreadDump getThreadDump() {
        Map<Long, ThreadDump.ThreadInfo> threadInfos = new HashMap<>();
        long[] allThreadIds = threadMXBean.getAllThreadIds();

        for (long threadId : allThreadIds) {
            ThreadInfo info = threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
            if (info != null) {
                threadInfos.put(threadId, convertThreadInfo(info));
            }
        }

        return ThreadDump.create(Instant.now(), threadInfos);
    }

    public List<ThreadStats> getThreadStats() {
        List<ThreadStats> stats = new ArrayList<>();
        long[] allThreadIds = threadMXBean.getAllThreadIds();

        for (long threadId : allThreadIds) {
            ThreadInfo info = threadMXBean.getThreadInfo(threadId, 0);
            if (info != null) {
                stats.add(convertThreadStats(info));
            }
        }

        return stats;
    }

    public ThreadStats getThreadStats(long threadId) {
        ThreadInfo info = threadMXBean.getThreadInfo(threadId, 0);
        return info != null ? convertThreadStats(info) : null;
    }

    private ThreadDump.ThreadInfo convertThreadInfo(ThreadInfo jmxInfo) {
        ThreadStats stats = convertThreadStats(jmxInfo);

        List<StackFrame> stackFrames = new ArrayList<>();
        for (StackTraceElement element : jmxInfo.getStackTrace()) {
            stackFrames.add(new StackFrame(
                element.getClassName(),
                element.getMethodName(),
                element.getFileName(),
                element.getLineNumber(),
                element.isNativeMethod()
            ));
        }

        List<Long> lockedMonitors = new ArrayList<>();
        for (java.lang.management.MonitorInfo info : jmxInfo.getLockedMonitors()) {
            lockedMonitors.add((long) info.getIdentityHashCode());
        }

        ThreadDump.ThreadInfo.Builder builder = ThreadDump.ThreadInfo.builder()
            .stats(stats)
            .stackTrace(stackFrames)
            .lockedMonitorIds(lockedMonitors);

        if (jmxInfo.getLockInfo() != null) {
            builder.blockedOnMonitorId((long) jmxInfo.getLockInfo().getIdentityHashCode());
        }
        if (jmxInfo.getLockOwnerName() != null) {
            builder.blockedOnLockOwnerName(jmxInfo.getLockOwnerName());
        }
        if (jmxInfo.getLockOwnerId() >= 0) {
            builder.blockedOnLockOwnerId(jmxInfo.getLockOwnerId());
        }

        return builder.build();
    }

    private ThreadStats convertThreadStats(ThreadInfo jmxInfo) {
        return ThreadStats.builder()
            .threadId(jmxInfo.getThreadId())
            .threadName(jmxInfo.getThreadName())
            .state(convertState(jmxInfo.getThreadState()))
            .blockedCount(jmxInfo.getBlockedCount())
            .blockedTime(jmxInfo.getBlockedTime())
            .waitedCount(jmxInfo.getWaitedCount())
            .waitedTime(jmxInfo.getWaitedTime())
            .build();
    }

    private ThreadState convertState(Thread.State state) {
        if (state == null) return ThreadState.UNKNOWN;
        switch (state) {
            case NEW: return ThreadState.NEW;
            case RUNNABLE: return ThreadState.RUNNABLE;
            case BLOCKED: return ThreadState.BLOCKED;
            case WAITING: return ThreadState.WAITING;
            case TIMED_WAITING: return ThreadState.TIMED_WAITING;
            case TERMINATED: return ThreadState.TERMINATED;
            default: return ThreadState.UNKNOWN;
        }
    }

    public int getThreadCount() {
        return threadMXBean.getThreadCount();
    }

    public int getPeakThreadCount() {
        return threadMXBean.getPeakThreadCount();
    }

    public long getTotalStartedThreadCount() {
        return threadMXBean.getTotalStartedThreadCount();
    }

    public int getDaemonThreadCount() {
        return threadMXBean.getDaemonThreadCount();
    }
}
