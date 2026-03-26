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
        ThreadDump dump = new ThreadDump();
        dump.setTimestamp(Instant.now());

        long[] allThreadIds = threadMXBean.getAllThreadIds();

        for (long threadId : allThreadIds) {
            ThreadInfo info = threadMXBean.getThreadInfo(threadId, Integer.MAX_VALUE);
            if (info != null) {
                dump.addThreadStats(convertToThreadStatsWithStack(info));
            }
        }

        return dump;
    }

    public List<ThreadStats> getThreadStats() {
        List<ThreadStats> stats = new ArrayList<>();
        long[] allThreadIds = threadMXBean.getAllThreadIds();

        for (long threadId : allThreadIds) {
            ThreadInfo info = threadMXBean.getThreadInfo(threadId, 0);
            if (info != null) {
                stats.add(convertToThreadStats(info));
            }
        }

        return stats;
    }

    public ThreadStats getThreadStats(long threadId) {
        ThreadInfo info = threadMXBean.getThreadInfo(threadId, 0);
        return info != null ? convertToThreadStats(info) : null;
    }

    private ThreadStats convertToThreadStats(ThreadInfo jmxInfo) {
        ThreadStats stats = new ThreadStats();
        stats.setThreadId(jmxInfo.getThreadId());
        stats.setThreadName(jmxInfo.getThreadName());
        stats.setState(convertState(jmxInfo.getThreadState()));
        stats.setBlockedCount(jmxInfo.getBlockedCount());
        stats.setBlockedTime(jmxInfo.getBlockedTime());
        stats.setWaitedCount(jmxInfo.getWaitedCount());
        stats.setWaitedTime(jmxInfo.getWaitedTime());
        return stats;
    }

    private ThreadStats convertToThreadStatsWithStack(ThreadInfo jmxInfo) {
        ThreadStats stats = convertToThreadStats(jmxInfo);

        List<StackFrame> stackFrames = new ArrayList<>();
        for (StackTraceElement element : jmxInfo.getStackTrace()) {
            StackFrame frame = new StackFrame();
            frame.setClassName(element.getClassName());
            frame.setMethodName(element.getMethodName());
            frame.setFileName(element.getFileName());
            frame.setLineNumber(element.getLineNumber());
            frame.setNativeMethod(element.isNativeMethod());
            stackFrames.add(frame);
        }
        stats.setStackTrace(stackFrames);

        return stats;
    }

    @Deprecated
    private ThreadDump.ThreadInfo convertThreadInfo(ThreadInfo jmxInfo) {
        ThreadStats stats = convertToThreadStats(jmxInfo);

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

    @Deprecated
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
