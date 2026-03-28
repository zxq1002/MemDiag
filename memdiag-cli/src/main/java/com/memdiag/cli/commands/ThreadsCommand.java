package com.memdiag.cli.commands;

import com.memdiag.cli.client.AgentClient;
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

@CommandLine.Command(name = "threads", description = "Show thread analysis", mixinStandardHelpOptions = true)
public class ThreadsCommand extends BaseCommand {

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20")
    private int limit;

    @CommandLine.Option(names = {"-s", "--stacks"}, defaultValue = "false")
    private boolean showStacks;

    @Override
    public void run() {
        ThreadDump dump;
        ThreadAnalyzer analyzer = null;

        if (isAgentMode()) {
            AgentClient client = createAgentClient();
            try {
                dump = client.getThreadDump();
            } catch (Exception e) {
                System.err.println("Failed to connect to agent: " + e.getMessage());
                return;
            }
        } else {
            JmxClient client;
            String p = getPid();
            if (p != null && !p.isEmpty()) {
                client = JmxClient.attachToPid(p);
            } else {
                client = JmxClient.attachToCurrentJvm();
            }
            analyzer = new ThreadAnalyzer(client);
            dump = analyzer.getThreadDump();
        }

        System.out.println("THREAD ANALYSIS");
        System.out.println("==========================================================================");
        if (analyzer != null) {
            System.out.printf("Total: %d, Peak: %d, Daemon: %d, Started: %d%n",
                dump.getThreadCount(),
                analyzer.getPeakThreadCount(),
                analyzer.getDaemonThreadCount(),
                analyzer.getTotalStartedThreadCount());
        } else {
            System.out.printf("Total: %d%n", dump.getThreadCount());
        }
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
