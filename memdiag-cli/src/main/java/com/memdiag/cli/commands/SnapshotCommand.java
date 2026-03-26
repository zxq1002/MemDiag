package com.memdiag.cli.commands;

import com.memdiag.cli.client.AgentClient;
import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.diff.SnapshotManager;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@CommandLine.Command(name = "snapshot", description = "Manage heap snapshots", mixinStandardHelpOptions = true)
public class SnapshotCommand extends BaseCommand {

    @CommandLine.Option(names = {"--save"}, description = "Save a new snapshot")
    private boolean save;

    @CommandLine.Option(names = {"--load"}, description = "Load and display a snapshot (ID or filename)")
    private String load;

    @CommandLine.Option(names = {"--list"}, description = "List all saved snapshots")
    private boolean list;

    @CommandLine.Option(names = {"--delete"}, description = "Delete a snapshot (ID or filename)")
    private String delete;

    @CommandLine.Option(names = {"--id"}, description = "Custom ID for the snapshot (when saving)")
    private String snapshotId;

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20", description = "Limit for class display")
    private int limit;

    private final SnapshotManager snapshotManager = new SnapshotManager();

    @Override
    public void run() {
        if (save) {
            saveSnapshot();
        } else if (load != null) {
            loadSnapshot();
        } else if (list) {
            listSnapshots();
        } else if (delete != null) {
            deleteSnapshot();
        } else {
            // Default: show help
            new CommandLine(this).usage(System.out);
        }
    }

    private void saveSnapshot() {
        System.out.println("CREATING SNAPSHOT");
        System.out.println("==========================================================================");

        HeapHistogram histogram;
        ThreadDump threadDump = null;

        if (isAgentMode()) {
            AgentClient client = createAgentClient();
            try {
                histogram = client.getHeapHistogram(1000);
                threadDump = client.getThreadDump();
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
            HeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(client);
            ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
            histogram = heapAnalyzer.getHistogram(1000);
            threadDump = threadAnalyzer.getThreadDump();
        }

        String id = snapshotId != null ? snapshotId : UUID.randomUUID().toString().substring(0, 8);
        Snapshot snapshot = new Snapshot.Builder()
            .setId(id)
            .setTimestamp(Instant.now())
            .setHeapHistogram(histogram)
            .setThreadDump(threadDump)
            .build();

        Path savedPath = snapshotManager.saveSnapshot(snapshot);

        System.out.println("✅ Snapshot created successfully!");
        System.out.println();
        System.out.println("Snapshot ID: " + id);
        System.out.println("Saved to: " + savedPath);
        System.out.println();
        System.out.println("Snapshot contains:");
        System.out.println("  - " + histogram.getClassStats().size() + " classes");
        System.out.println("  - " + String.format("%,d", histogram.getTotalObjects()) + " objects");
        System.out.println("  - " + String.format("%,d", histogram.getTotalBytes()) + " bytes");
        if (threadDump != null) {
            System.out.println("  - " + threadDump.getThreadCount() + " threads");
        }
        System.out.println();
        System.out.println("To load this snapshot later:");
        System.out.println("  memdiag snapshot --load " + id);
    }

    private void loadSnapshot() {
        Snapshot snapshot = snapshotManager.loadSnapshot(load);
        if (snapshot == null) {
            System.err.println("❌ Snapshot not found: " + load);
            System.err.println();
            System.err.println("Use 'memdiag snapshot --list' to see available snapshots.");
            return;
        }

        System.out.println("SNAPSHOT DETAILS");
        System.out.println("==========================================================================");
        System.out.println("ID:        " + (snapshot.getId() != null ? snapshot.getId() : "n/a"));
        System.out.println("Timestamp: " + snapshot.getTimestamp());
        System.out.println();

        if (snapshot.getHeapHistogram() != null) {
            HeapHistogram histogram = snapshot.getHeapHistogram();
            System.out.println("HEAP HISTOGRAM");
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("Total objects: " + String.format("%,d", histogram.getTotalObjects()));
            System.out.println("Total bytes:   " + String.format("%,d", histogram.getTotalBytes()));
            System.out.println();
            System.out.printf("%-40s %15s %15s%n", "CLASS NAME", "OBJECTS", "SHALLOW HEAP");
            System.out.println("-------------------------------------------------------------------------");
            for (var stats : histogram.getTopByShallowBytes(limit)) {
                System.out.printf("%-40s %,15d %,15d%n",
                    truncate(stats.getClassName(), 40),
                    stats.getObjectCount(),
                    stats.getShallowBytes());
            }
            System.out.println();
        }

        if (snapshot.getThreadDump() != null) {
            ThreadDump dump = snapshot.getThreadDump();
            System.out.println("THREAD DUMP");
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("Total threads: " + dump.getThreadCount());
            System.out.println("  RUNNABLE:     " + dump.getThreadsByState(com.memdiag.core.thread.ThreadState.RUNNABLE).size());
            System.out.println("  BLOCKED:      " + dump.getThreadsByState(com.memdiag.core.thread.ThreadState.BLOCKED).size());
            System.out.println("  WAITING:      " + dump.getThreadsByState(com.memdiag.core.thread.ThreadState.WAITING).size());
            System.out.println("  TIMED_WAITING:" + dump.getThreadsByState(com.memdiag.core.thread.ThreadState.TIMED_WAITING).size());
        }
    }

    private void listSnapshots() {
        List<SnapshotManager.SnapshotInfo> snapshots = snapshotManager.listSnapshots();

        System.out.println("AVAILABLE SNAPSHOTS");
        System.out.println("==========================================================================");

        if (snapshots.isEmpty()) {
            System.out.println("No snapshots saved yet.");
            System.out.println();
            System.out.println("To create a snapshot:");
            System.out.println("  memdiag snapshot --save [--pid <pid>]");
            return;
        }

        System.out.printf("%-12s %-25s %12s  %s%n", "ID", "TIMESTAMP", "SIZE", "FILENAME");
        System.out.println("--------------------------------------------------------------------------");

        for (SnapshotManager.SnapshotInfo info : snapshots) {
            String id = info.id != null ? info.id : "n/a";
            String timestamp = info.lastModified != null ? info.lastModified.toString().replace('T', ' ').substring(0, 19) : "n/a";
            String size = formatSize(info.size);
            System.out.printf("%-12s %-25s %12s  %s%n", id, timestamp, size, info.filename);
        }
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("Total: " + snapshots.size() + " snapshot(s)");
        System.out.println();
        System.out.println("To load a snapshot:");
        System.out.println("  memdiag snapshot --load <ID>");
        System.out.println();
        System.out.println("To delete a snapshot:");
        System.out.println("  memdiag snapshot --delete <ID>");
    }

    private void deleteSnapshot() {
        Path path = snapshotManager.findSnapshot(delete);
        if (path == null) {
            System.err.println("❌ Snapshot not found: " + delete);
            return;
        }

        if (snapshotManager.deleteSnapshot(path)) {
            System.out.println("✅ Deleted snapshot: " + path.getFileName());
        } else {
            System.err.println("❌ Failed to delete snapshot: " + path.getFileName());
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
