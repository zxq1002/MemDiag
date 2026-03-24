package com.memdiag.cli.commands;

import com.memdiag.cli.client.AgentClient;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

@CommandLine.Command(name = "histogram", description = "Show heap histogram")
public class HistogramCommand extends BaseCommand {

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20")
    private int limit;

    @Override
    public void run() {
        HeapHistogram histogram;

        if (isAgentMode()) {
            AgentClient client = createAgentClient();
            try {
                histogram = client.getHeapHistogram(limit);
            } catch (Exception e) {
                System.err.println("Failed to connect to agent: " + e.getMessage());
                return;
            }
        } else {
            JmxClient client;
            if (pid != null && !pid.isEmpty()) {
                client = JmxClient.attachToPid(pid);
            } else {
                client = JmxClient.attachToCurrentJvm();
            }
            HeapAnalyzer analyzer = new JmxHeapAnalyzer(client);
            histogram = analyzer.getHistogram(limit);
        }

        System.out.printf("%-40s %15s %15s%n", "CLASS NAME", "OBJECTS", "SHALLOW HEAP");
        System.out.println("-------------------------------------------------------------------------");
        for (ClassStats stats : histogram.getTopByShallowBytes(limit)) {
            System.out.printf("%-40s %,15d %,15d%n",
                truncate(stats.getClassName(), 40),
                stats.getObjectCount(),
                stats.getShallowBytes());
        }
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf("%-40s %,15d %,15d%n",
            "Total",
            histogram.getTotalObjects(),
            histogram.getTotalBytes());
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }
}
