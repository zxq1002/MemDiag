package com.memdiag.cli.commands;

import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.thread.StackFrame;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadState;
import com.memdiag.core.thread.ThreadStats;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "threads", description = "Show thread analysis")
public class ThreadsCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "PID (optional for current JVM)", arity = "0..1")
    private String pid;

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20")
    private int limit;

    @CommandLine.Option(names = {"-s", "--stacks"}, defaultValue = "false")
    private boolean showStacks;

    @Override
    public void run() {
        JmxClient client = pid != null && !pid.isEmpty()
            ? JmxClient.attachToPid(pid)
            : JmxClient.attachToCurrentJvm();
        ThreadAnalyzer analyzer = new ThreadAnalyzer(client);
        ThreadDump dump = analyzer.getThreadDump();

        System.out.println("THREAD ANALYSIS");
        System.out.println("==========================================================================");
        System.out.printf("Total: %d, Peak: %d, Daemon: %d, Started: %d%n",
            dump.getThreadCount(),
            analyzer.getPeakThreadCount(),
            analyzer.getDaemonThreadCount(),
            analyzer.getTotalStartedThreadCount());
        System.out.println();

        System.out.println("THREAD STATES:");
        System.out.printf("  RUNNABLE:     %3d%n", dump.getThreadsByState(ThreadState.RUNNABLE).size());
        System.out.printf("  BLOCKED:      %3d%n", dump.getThreadsByState(ThreadState.BLOCKED).size());
        System.out.printf("  WAITING:      %3d%n", dump.getThreadsByState(ThreadState.WAITING).size());
        System.out.printf("  TIMED_WAITING:%3d%n", dump.getThreadsByState(ThreadState.TIMED_WAITING).size());
        System.out.println();

        System.out.println("TOP THREADS:");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-8s %-30s %-15s %10s %10s%n",
            "TID", "NAME", "STATE", "BLOCKED", "WAITED");
        System.out.println("--------------------------------------------------------------------------");

        dump.getThreadInfos().values().stream()
            .sorted(Comparator.comparingLong(info -> -info.getStats().getThreadId()))
            .limit(limit)
            .forEach(info -> {
                ThreadStats stats = info.getStats();
                System.out.printf("%-8d %-30s %-15s %,10d %,10d%n",
                    stats.getThreadId(),
                    truncate(stats.getThreadName(), 30),
                    stats.getState(),
                    stats.getBlockedCount(),
                    stats.getWaitedCount());

                if (showStacks && !info.getStackTrace().isEmpty()) {
                    System.out.println("  Stack:");
                    for (StackFrame frame : info.getStackTrace()) {
                        System.out.printf("    at %s%n", frame);
                    }
                    System.out.println();
                }
            });
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen - 3) + "...";
    }
}
