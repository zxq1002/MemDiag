package com.memdiag.cli.commands;

import com.memdiag.cli.client.AgentClient;
import com.memdiag.core.diff.ClassDiff;
import com.memdiag.core.diff.HeapDiff;
import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.diff.SnapshotManager;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@CommandLine.Command(name = "diff", description = "Compare heap snapshots", mixinStandardHelpOptions = true)
public class DiffCommand extends BaseCommand {

    @CommandLine.Option(names = {"--baseline"}, description = "Baseline snapshot for comparison (ID or filename)")
    private String baseline;

    @CommandLine.Option(names = {"--current"}, description = "Current snapshot for comparison (ID or filename; defaults to live heap)")
    private String current;

    @CommandLine.Option(names = {"--growing"}, defaultValue = "10", description = "Show top N growing classes")
    private int growingLimit;

    @CommandLine.Option(names = {"--shrinking"}, defaultValue = "5", description = "Show top N shrinking classes")
    private int shrinkingLimit;

    @CommandLine.Option(names = {"--growth-rate"}, defaultValue = "5", description = "Show top N classes by growth rate")
    private int growthRateLimit;

    @CommandLine.Option(names = {"--all"}, description = "Show all changes (not just top N)")
    private boolean showAll;

    private final SnapshotManager snapshotManager = new SnapshotManager();

    @Override
    public void run() {
        if (baseline == null) {
            System.err.println("❌ Missing --baseline parameter");
            System.err.println();
            System.err.println("Usage:");
            System.err.println("  memdiag diff --baseline <snapshot-id> [--current <snapshot-id>] [--pid <pid>]");
            System.err.println();
            System.err.println("Examples:");
            System.err.println("  # Compare baseline with current live heap");
            System.err.println("  memdiag diff --baseline snapshot-123 --pid 4567");
            System.err.println();
            System.err.println("  # Compare two saved snapshots");
            System.err.println("  memdiag diff --baseline snapshot-123 --current snapshot-456");
            return;
        }

        Snapshot baselineSnapshot = snapshotManager.loadSnapshot(baseline);
        if (baselineSnapshot == null) {
            System.err.println("❌ Baseline snapshot not found: " + baseline);
            System.err.println();
            System.err.println("Use 'memdiag snapshot --list' to see available snapshots.");
            return;
        }

        Snapshot currentSnapshot;
        if (current != null) {
            currentSnapshot = snapshotManager.loadSnapshot(current);
            if (currentSnapshot == null) {
                System.err.println("❌ Current snapshot not found: " + current);
                return;
            }
        } else {
            // Take current snapshot from live heap
            currentSnapshot = takeLiveSnapshot();
            if (currentSnapshot == null) {
                return; // Error already printed
            }
        }

        computeAndDisplayDiff(baselineSnapshot, currentSnapshot);
    }

    private Snapshot takeLiveSnapshot() {
        HeapHistogram histogram;
        ThreadDump threadDump = null;

        if (isAgentMode()) {
            AgentClient client = createAgentClient();
            try {
                histogram = client.getHeapHistogram(1000);
                threadDump = client.getThreadDump();
            } catch (Exception e) {
                System.err.println("Failed to connect to agent: " + e.getMessage());
                return null;
            }
        } else {
            JmxClient client;
            if (pid != null && !pid.isEmpty()) {
                client = JmxClient.attachToPid(pid);
            } else {
                client = JmxClient.attachToCurrentJvm();
            }
            HeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(client);
            ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
            histogram = heapAnalyzer.getHistogram(1000);
            threadDump = threadAnalyzer.getThreadDump();
        }

        return new Snapshot.Builder()
            .setId(UUID.randomUUID().toString().substring(0, 8))
            .setTimestamp(Instant.now())
            .setHeapHistogram(histogram)
            .setThreadDump(threadDump)
            .build();
    }

    private void computeAndDisplayDiff(Snapshot baselineSnapshot, Snapshot currentSnapshot) {
        HeapDiff diff = HeapDiff.compute(baselineSnapshot, currentSnapshot);

        System.out.println("HEAP DIFF ANALYSIS");
        System.out.println("==========================================================================");
        System.out.println("Baseline: " + formatSnapshotInfo(baselineSnapshot));
        System.out.println("Current:  " + formatSnapshotInfo(currentSnapshot));
        System.out.println();

        // Summary
        System.out.println("SUMMARY");
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Total object delta: " + formatDelta(diff.getTotalObjectDelta()));
        System.out.println("Total byte delta:   " + formatByteDelta(diff.getTotalByteDelta()));
        System.out.println("Changed classes:    " + diff.getAllDiffs().size());
        System.out.println();

        List<ClassDiff> growingClasses = diff.getGrowingClasses(showAll ? Integer.MAX_VALUE : growingLimit);
        List<ClassDiff> shrinkingClasses = diff.getShrinkingClasses(showAll ? Integer.MAX_VALUE : shrinkingLimit);
        List<ClassDiff> newClasses = diff.getNewClasses();
        List<ClassDiff> disappearedClasses = diff.getDisappearedClasses();
        List<ClassDiff> topByGrowth = diff.getTopByGrowthRate(showAll ? Integer.MAX_VALUE : growthRateLimit);

        // Growing classes
        if (!growingClasses.isEmpty()) {
            System.out.println("GROWING CLASSES (top " + growingClasses.size() + ")");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-40s %15s %15s %10s%n", "CLASS NAME", "OBJ DELTA", "BYTE DELTA", "GROWTH");
            System.out.println("--------------------------------------------------------------------------");
            for (ClassDiff cd : growingClasses) {
                System.out.printf("%-40s %15s %15s %10s%n",
                    truncate(cd.getClassKey().getClassName(), 40),
                    formatDelta(cd.getObjectCountDelta()),
                    formatByteDelta(cd.getBytesDelta()),
                    formatGrowthRate(cd.getGrowthRate()));
            }
            System.out.println();
        }

        // Shrinking classes
        if (!shrinkingClasses.isEmpty()) {
            System.out.println("SHRINKING CLASSES (top " + shrinkingClasses.size() + ")");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-40s %15s %15s %10s%n", "CLASS NAME", "OBJ DELTA", "BYTE DELTA", "GROWTH");
            System.out.println("--------------------------------------------------------------------------");
            for (ClassDiff cd : shrinkingClasses) {
                System.out.printf("%-40s %15s %15s %10s%n",
                    truncate(cd.getClassKey().getClassName(), 40),
                    formatDelta(cd.getObjectCountDelta()),
                    formatByteDelta(cd.getBytesDelta()),
                    formatGrowthRate(cd.getGrowthRate()));
            }
            System.out.println();
        }

        // New classes
        if (!newClasses.isEmpty()) {
            System.out.println("NEW CLASSES (" + newClasses.size() + ")");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-40s %15s %15s%n", "CLASS NAME", "OBJECTS", "BYTES");
            System.out.println("--------------------------------------------------------------------------");
            for (ClassDiff cd : newClasses) {
                System.out.printf("%-40s %,15d %,15d%n",
                    truncate(cd.getClassKey().getClassName(), 40),
                    cd.getCurrentObjectCount(),
                    cd.getCurrentBytes());
            }
            System.out.println();
        }

        // Disappeared classes
        if (!disappearedClasses.isEmpty()) {
            System.out.println("DISAPPEARED CLASSES (" + disappearedClasses.size() + ")");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-40s %15s %15s%n", "CLASS NAME", "OBJECTS", "BYTES");
            System.out.println("--------------------------------------------------------------------------");
            for (ClassDiff cd : disappearedClasses) {
                System.out.printf("%-40s %,15d %,15d%n",
                    truncate(cd.getClassKey().getClassName(), 40),
                    cd.getBaselineObjectCount(),
                    cd.getBaselineBytes());
            }
            System.out.println();
        }

        // Top by growth rate
        if (!topByGrowth.isEmpty() && !showAll) {
            System.out.println("TOP BY GROWTH RATE (top " + topByGrowth.size() + ")");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-40s %15s %15s %10s%n", "CLASS NAME", "OBJ DELTA", "BYTE DELTA", "GROWTH");
            System.out.println("--------------------------------------------------------------------------");
            for (ClassDiff cd : topByGrowth) {
                System.out.printf("%-40s %15s %15s %10s%n",
                    truncate(cd.getClassKey().getClassName(), 40),
                    formatDelta(cd.getObjectCountDelta()),
                    formatByteDelta(cd.getBytesDelta()),
                    formatGrowthRate(cd.getGrowthRate()));
            }
            System.out.println();
        }
    }

    private String formatSnapshotInfo(Snapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        if (snapshot.getId() != null) {
            sb.append("[").append(snapshot.getId()).append("] ");
        }
        if (snapshot.getTimestamp() != null) {
            sb.append(snapshot.getTimestamp());
        }
        if (snapshot.getHeapHistogram() != null) {
            sb.append(" - ")
              .append(String.format("%,d", snapshot.getHeapHistogram().getTotalObjects())).append(" objs, ")
              .append(formatBytes(snapshot.getHeapHistogram().getTotalBytes()));
        }
        return sb.toString();
    }

    private String truncate(String s, int maxLen) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }

    private String formatDelta(long delta) {
        if (delta > 0) {
            return "+" + String.format("%,d", delta);
        } else if (delta < 0) {
            return String.format("%,d", delta);
        }
        return "0";
    }

    private String formatByteDelta(long delta) {
        if (delta > 0) {
            return "+" + formatBytes(delta);
        } else if (delta < 0) {
            return "-" + formatBytes(-delta);
        }
        return "0";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private String formatGrowthRate(double rate) {
        if (rate == Double.POSITIVE_INFINITY) {
            return "new";
        }
        if (rate == -1.0) {
            return "gone";
        }
        return String.format("%.1f%%", rate * 100);
    }
}
