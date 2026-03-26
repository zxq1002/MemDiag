package com.memdiag.cli.commands;

import com.memdiag.core.nmt.JmxNmtAnalyzer;
import com.memdiag.core.nmt.NmtCategory;
import com.memdiag.core.nmt.NmtMemoryUsage;
import com.memdiag.core.nmt.NmtSnapshot;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

@CommandLine.Command(
    name = "nmt",
    description = "Native Memory Tracking analysis (requires -XX:NativeMemoryTracking)",
    mixinStandardHelpOptions = true
)
public class NmtCommand extends BaseCommand {

    @CommandLine.Option(
        names = {"-d", "--detail"},
        description = "Show detailed NMT information"
    )
    private boolean detail;

    @Override
    public void run() {
        JmxClient client;
        try {
            if (pid != null && !pid.isEmpty()) {
                System.err.println("Attaching to JVM PID " + pid + "...");
                client = JmxClient.attachToPid(pid);
            } else {
                System.err.println("Attaching to current JVM...");
                client = JmxClient.attachToCurrentJvm();
            }
        } catch (Exception e) {
            System.err.println("Failed to attach to JVM: " + e.getMessage());
            return;
        }

        JmxNmtAnalyzer analyzer = new JmxNmtAnalyzer(client);

        // Check if NMT is enabled
        String nmtLevel = analyzer.getNmtLevel();
        System.err.println("NMT level: " + nmtLevel);

        if ("off".equalsIgnoreCase(nmtLevel)) {
            System.err.println();
            System.err.println("ERROR: Native Memory Tracking is not enabled.");
            System.err.println("Please start your JVM with:");
            System.err.println("  -XX:NativeMemoryTracking=summary  (for summary mode)");
            System.err.println("  -XX:NativeMemoryTracking=detail   (for detailed mode)");
            return;
        }

        if (!analyzer.isNmtEnabled()) {
            System.err.println();
            System.err.println("ERROR: Could not access NMT data.");
            System.err.println("Make sure DiagnosticCommandMBean is available.");
            return;
        }

        // Get snapshot
        NmtSnapshot snapshot;
        try {
            if (detail) {
                snapshot = analyzer.getDetailSnapshot();
            } else {
                snapshot = analyzer.getSummarySnapshot();
            }
        } catch (Exception e) {
            System.err.println("Failed to get NMT snapshot: " + e.getMessage());
            return;
        }

        // Print results
        System.out.println();
        System.out.println("NATIVE MEMORY TRACKING REPORT");
        System.out.println("==========================================================================");
        System.out.printf("Generated at: %s%n", snapshot.getTimestamp());
        System.out.printf("Total:    Reserved=%-15s Committed=%-15s%n",
            formatBytes(snapshot.getTotalReserved()),
            formatBytes(snapshot.getTotalCommitted()));
        System.out.println();

        System.out.printf("%-20s %15s %15s %15s %12s%n",
            "CATEGORY", "RESERVED", "COMMITTED", "MALLOCED", "COUNT");
        System.out.println("--------------------------------------------------------------------------");

        for (NmtMemoryUsage usage : snapshot.getUsages()) {
            System.out.printf("%-20s %,15d %,15d %,15d %,12d%n",
                usage.getCategory().getDisplayName(),
                usage.getReserved(),
                usage.getCommitted(),
                usage.getMalloced(),
                usage.getMallocCount());
        }

        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-20s %,15d %,15d %,15d%n",
            "TOTAL",
            snapshot.getTotalReserved(),
            snapshot.getTotalCommitted(),
            snapshot.getTotalMalloced());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
