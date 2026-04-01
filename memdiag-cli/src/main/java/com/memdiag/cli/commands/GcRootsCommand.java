package com.memdiag.cli.commands;

import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.heap.GcRootAnalyzer;
import com.memdiag.core.heap.GcRootPath;
import com.memdiag.core.heap.GcRootStats;
import com.memdiag.core.heap.GcRootType;
import com.memdiag.core.heap.JmxGcRootAnalyzer;
import com.memdiag.core.heap.ObjectId;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "gc-roots", description = "GC Root analysis (Java 11+)", mixinStandardHelpOptions = true)
public class GcRootsCommand extends BaseCommand {

    @CommandLine.Option(names = {"--class"}, description = "Class name to analyze (e.g., 'byte[]', 'com.example.MyClass')")
    private String className;

    @CommandLine.Option(names = {"--depth"}, description = "Maximum search depth (default: 10)", defaultValue = "10")
    private int maxDepth;

    @CommandLine.Option(names = {"--paths"}, description = "Maximum number of paths to show (default: 5)", defaultValue = "5")
    private int maxPaths;

    @CommandLine.Option(names = {"--stats"}, description = "Show GC Root statistics only")
    private boolean statsOnly;

    @Override
    public void run() {
        String pidToUse = getPid();
        if (!isAgentMode() && (pidToUse == null || pidToUse.isEmpty())) {
            System.err.println("Error: PID is required. Use --pid <pid> or provide as parameter.");
            System.err.println();
            System.err.println("Usage:");
            System.err.println("  memdiag gc-roots <pid> [options]");
            System.err.println("  memdiag gc-roots --pid <pid> [options]");
            System.err.println("  memdiag gc-roots --agent=<host:port> [options]");
            System.exit(1);
        }

        try {
            GcRootStats stats;

            if (isAgentMode()) {
                AgentClient client = createAgentClient();

                // Start GC Root tracking if needed
                client.startGcRootTracking();

                stats = client.getGcRootStats();
                if (stats == null) {
                    System.err.println("Failed to get GC Root stats from agent");
                    return;
                }

                // 参数行为设计:
                // --stats: 只显示简洁的统计信息
                // --class: 只显示该类的 GC Root 路径（不显示统计）
                // 无参数: 显示完整的统计信息 + 使用提示
                if (statsOnly) {
                    printStatsCompact(stats);
                } else if (className != null) {
                    printGcRootsPlaceholder(className);
                } else {
                    printStats(stats);
                    printUsageHint();
                }

                // Stop tracking
                client.stopGcRootTracking();
            } else {
                JmxClient jmxClient = JmxClient.attachToPid(pidToUse);
                GcRootAnalyzer analyzer = new JmxGcRootAnalyzer(jmxClient);

                // 参数行为设计:
                // --stats: 只显示简洁的统计信息
                // --class: 只显示该类的 GC Root 路径（不显示统计）
                // 无参数: 显示完整的统计信息 + 使用提示
                if (statsOnly) {
                    printStatsCompact(analyzer);
                } else if (className != null) {
                    printGcRoots(analyzer);
                } else {
                    printStats(analyzer);
                    printUsageHint();
                }
            }
        } catch (Exception e) {
            System.err.println("Error performing GC Root analysis: " + e.getMessage());
            System.err.println();
            System.err.println("Note: GC Root analysis requires:");
            System.err.println("  1. The target JVM must be accessible via JMX or agent");
            System.err.println("  2. Full GC Root traversal requires JVMTI agent (coming soon)");
            System.err.println("  3. For now, only basic statistics are available");
            System.exit(1);
        }
    }

    private void printStats(GcRootAnalyzer analyzer) {
        printStats(analyzer.getGcRootStats());
    }

    private void printStats(GcRootStats stats) {
        System.out.println("GC ROOT STATISTICS");
        System.out.println("==========================================================================");

        System.out.printf("Total GC Roots: %,d%n", stats.getTotalRoots());
        System.out.println();
        System.out.println("BY TYPE:");
        System.out.printf("  %-20s %15s%n", "TYPE", "COUNT");
        System.out.println("  ----------------------------------------");

        for (GcRootType type : GcRootType.values()) {
            long count = stats.getCount(type);
            if (count > 0 || isRootType(type)) {
                System.out.printf("  %-20s %,15d%n", type.name(), count);
            }
        }

        System.out.println();
        System.out.println("Note: Full GC Root analysis requires JVMTI agent.");
        System.out.println("      For complete reference chain traversal, wait for future updates.");
    }

    private void printStatsCompact(GcRootAnalyzer analyzer) {
        printStatsCompact(analyzer.getGcRootStats());
    }

    private void printStatsCompact(GcRootStats stats) {
        System.out.printf("%,d GC Roots total", stats.getTotalRoots());
        boolean first = true;
        for (GcRootType type : GcRootType.values()) {
            long count = stats.getCount(type);
            if (count > 0) {
                if (first) {
                    System.out.print(" (");
                    first = false;
                } else {
                    System.out.print(", ");
                }
                System.out.printf("%s: %,d", type.name(), count);
            }
        }
        if (!first) {
            System.out.println(")");
        } else {
            System.out.println();
        }
    }

    private void printUsageHint() {
        System.out.println();
        System.out.println("💡 Usage Tips:");
        System.out.println("  --stats              Show compact statistics (one line)");
        System.out.println("  --class=<name>       Show GC root paths for specific class");
        System.out.println("  --class=<name> --stats  Show only stats for the class");
        System.out.println();
    }

    private boolean isRootType(GcRootType type) {
        return type != GcRootType.INSTANCE_FIELD;
    }

    private void printGcRoots(GcRootAnalyzer analyzer) {
        System.out.println("GC ROOT PATHS");
        System.out.println("==========================================================================");
        System.out.printf("Target Class: %s%n", className);
        System.out.printf("Max Depth: %d%n", maxDepth);
        System.out.printf("Max Paths: %d%n", maxPaths);
        System.out.println();

        System.out.println("⚠️ Feature Notice:");
        System.out.println("  Complete GC Root reference chain traversal requires JVMTI agent.");
        System.out.println("  This feature is coming in a future update.");
        System.out.println();
        System.out.println("  For now, you can:");
        System.out.println("    1. Use without --class to see GC Root type counts");
        System.out.println("    2. Take a heap dump and analyze with VisualVM/YourKit");
        System.out.println("    3. Wait for JVMTI-based full GC Root analysis");
        System.out.println();

        // Placeholder: In the future, this will show actual paths
        System.out.println("Example of what will be available:");
        System.out.println("--------------------------------------------------------------------------");
        printExamplePath();
    }

    private void printExamplePath() {
        System.out.println("Path 1/3 (depth=3):");
        System.out.println("  [THREAD_STACK] Thread: 'main' (id=1)");
        System.out.println("    ↳ java.lang.Thread.locals");
        System.out.println("      ↳ java.util.HashMap.table");
        System.out.println("        ↳ java.util.HashMap$Node");
        System.out.println("          ↳ " + className);
        System.out.println();
        System.out.println("Path 2/3 (depth=2):");
        System.out.println("  [STATIC_FIELD] Class: 'com.example.MyApp'");
        System.out.println("    ↳ com.example.MyApp.cache");
        System.out.println("      ↳ " + className);
        System.out.println();
    }

    private void printGcRootsPlaceholder(String targetClassName) {
        System.out.println("GC ROOT PATHS");
        System.out.println("==========================================================================");
        System.out.printf("Target Class: %s%n", targetClassName);
        System.out.printf("Max Depth: %d%n", maxDepth);
        System.out.printf("Max Paths: %d%n", maxPaths);
        System.out.println();

        System.out.println("⚠️ Feature Notice:");
        System.out.println("  Complete GC Root reference chain traversal requires JVMTI agent.");
        System.out.println("  This feature is coming in a future update.");
        System.out.println();
        System.out.println("  For now, you can:");
        System.out.println("    1. Use without --class to see GC Root type counts");
        System.out.println("    2. Take a heap dump and analyze with VisualVM/YourKit");
        System.out.println("    3. Wait for JVMTI-based full GC Root analysis");
        System.out.println();

        // Placeholder: In the future, this will show actual paths
        System.out.println("Example of what will be available:");
        System.out.println("--------------------------------------------------------------------------");
        printExamplePathForClass(targetClassName);
    }

    private void printExamplePathForClass(String targetClassName) {
        System.out.println("Path 1/3 (depth=3):");
        System.out.println("  [THREAD_STACK] Thread: 'main' (id=1)");
        System.out.println("    ↳ java.lang.Thread.locals");
        System.out.println("      ↳ java.util.HashMap.table");
        System.out.println("        ↳ java.util.HashMap$Node");
        System.out.println("          ↳ " + targetClassName);
        System.out.println();
        System.out.println("Path 2/3 (depth=2):");
        System.out.println("  [STATIC_FIELD] Class: 'com.example.MyApp'");
        System.out.println("    ↳ com.example.MyApp.cache");
        System.out.println("      ↳ " + targetClassName);
        System.out.println();
    }
}
