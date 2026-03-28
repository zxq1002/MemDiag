package com.memdiag.cli.commands;

import com.memdiag.core.agent.AgentAttacher;
import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.agent.AgentNativeAnalyzer;
import com.memdiag.core.nativeapi.AllocationPairDetector;
import com.memdiag.core.nativeapi.AllocationSite;
import com.memdiag.core.nativeapi.LibraryMapping;
import com.memdiag.core.nativeapi.MemoryRegion;
import com.memdiag.core.nativeapi.NativeDiagnosis;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzer;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzerFactory;
import com.memdiag.core.nativeapi.NativeMemorySummary;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "native", description = "Native memory analysis (Linux only)", mixinStandardHelpOptions = true)
public class NativeCommand extends BaseCommand {

    @CommandLine.Option(names = {"--status"}, description = "Check if native analysis is available")
    private boolean status;

    @CommandLine.Option(names = {"--summary"}, description = "Show native memory summary")
    private boolean summary;

    @CommandLine.Option(names = {"--regions"}, description = "Show memory regions")
    private boolean regions;

    @CommandLine.Option(names = {"--diagnose"}, description = "Run native leak diagnosis")
    private boolean diagnose;

    @CommandLine.Option(names = {"--attach"}, description = "Attach MemDiag agent to target process")
    private boolean attach;

    @CommandLine.Option(names = {"--detach"}, description = "Detach MemDiag agent from target process")
    private boolean detach;

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20", description = "Limit for output")
    private int limit;

    @CommandLine.Option(names = {"--agent-port"}, defaultValue = "6789", description = "Port for communicating with MemDiag agent")
    private int agentPort;

    @CommandLine.Option(names = {"--agent-host"}, defaultValue = "localhost", description = "Host for communicating with MemDiag agent")
    private String agentHost;

    @CommandLine.Option(names = {"--agent-jar"}, description = "Path to memdiag-agent.jar for dynamic attach")
    private String agentJarPath;

    // 用于演示的分配点检测器（实际会从 Agent 获取数据）
    private static final AllocationPairDetector demoDetector = new AllocationPairDetector();

    @Override
    public void run() {
        // Try Agent mode first (preferred)
        NativeMemoryAnalyzer analyzer = tryCreateAgentAnalyzer();

        if (analyzer == null) {
            // Fall back to ProcFS mode (basic, no agent required)
            String p = getPid();
            if (p != null && !p.isEmpty()) {
                analyzer = NativeMemoryAnalyzerFactory.getInstance(p);
            } else {
                analyzer = NativeMemoryAnalyzerFactory.getInstance();
            }
        }

        if (status) {
            printStatus(analyzer);
        } else if (summary) {
            printSummary(analyzer);
        } else if (regions) {
            printRegions(analyzer);
        } else if (diagnose) {
            printDiagnosis(analyzer);
        } else if (attach) {
            handleAttach(analyzer);
        } else if (detach) {
            handleDetach(analyzer);
        } else {
            printStatus(analyzer);
        }
    }

    private void printStatus(NativeMemoryAnalyzer analyzer) {
        System.out.println("NATIVE MEMORY ANALYSIS STATUS");
        System.out.println("==========================================================================");
        System.out.printf("Available: %s%n", analyzer.isAvailable() ? "✅ Yes" : "❌ No");
        System.out.printf("Platform: %s%n", analyzer.getPlatform());
        System.out.printf("Mode: %s%n", getModeDescription(analyzer));

        if (analyzer instanceof AgentNativeAnalyzer) {
            AgentNativeAnalyzer agentAnalyzer = (AgentNativeAnalyzer) analyzer;
            AgentClient client = agentAnalyzer.getClient();
            System.out.printf("Agent Connected: %s%n", client.isReachable() ? "✅ Yes" : "❌ No");
            if (client.isReachable()) {
                System.out.printf("Agent Endpoint: %s:%d%n", client.getHost(), client.getPort());
            }
        }

        if (!analyzer.isAvailable()) {
            System.out.println();
            System.out.println("NOTE: Native memory analysis requires Linux with /proc filesystem mounted.");
        }

        System.out.println();
        System.out.println("AVAILABLE MODES:");
        System.out.println("  ✅ Agent Mode (recommended) - Use memdiag-agent.jar for full features");
        System.out.println("     - Start target JVM with: java -javaagent:memdiag-agent.jar=port=6789 ...");
        System.out.println("     - Or attach dynamically: memdiag native --attach --agent-jar <path> --pid <pid>");
        System.out.println();
        System.out.println("  ✅ ProcFS Mode (basic) - Read-only /proc filesystem analysis");
        System.out.println("     - No agent required");
        System.out.println("     - Limited to status/summary/regions/diagnose");
    }

    private String getModeDescription(NativeMemoryAnalyzer analyzer) {
        if (analyzer instanceof AgentNativeAnalyzer) {
            return "Agent Mode (HTTP API)";
        }
        return "ProcFS Mode (read-only)";
    }

    private void printSummary(NativeMemoryAnalyzer analyzer) {
        if (!analyzer.isAvailable()) {
            System.err.println("Native memory analysis is not available on this platform.");
            return;
        }

        NativeMemorySummary summary = analyzer.getSummary();

        System.out.println("NATIVE MEMORY SUMMARY");
        System.out.println("==========================================================================");
        System.out.printf("Total Virtual:  %,15d bytes (%,.2f MB)%n",
            summary.getTotalVirtual(), summary.getTotalVirtual() / (1024.0 * 1024.0));
        System.out.printf("Total Resident: %,15d bytes (%,.2f MB)%n",
            summary.getTotalResident(), summary.getTotalResident() / (1024.0 * 1024.0));

        if (summary.getDirectByteBufferSize() > 0) {
            System.out.printf("Direct ByteBuffers: %,12d bytes (%,.2f MB)%n",
                summary.getDirectByteBufferSize(), summary.getDirectByteBufferSize() / (1024.0 * 1024.0));
        }

        if (summary.getThreadStackSize() > 0) {
            System.out.printf("Thread Stacks:    %,12d bytes (%,.2f MB)%n",
                summary.getThreadStackSize(), summary.getThreadStackSize() / (1024.0 * 1024.0));
        }

        if (summary.getCodeCacheSize() > 0) {
            System.out.printf("Code Cache:       %,12d bytes (%,.2f MB)%n",
                summary.getCodeCacheSize(), summary.getCodeCacheSize() / (1024.0 * 1024.0));
        }

        if (!summary.getBreakdownByCategory().isEmpty()) {
            System.out.println();
            System.out.println("BREAKDOWN BY CATEGORY:");
            for (String category : summary.getBreakdownByCategory().keySet()) {
                long size = summary.getBreakdownByCategory().get(category);
                System.out.printf("  %-20s %,15d bytes (%,.2f MB)%n",
                    category + ":", size, size / (1024.0 * 1024.0));
            }
        }
    }

    private void printRegions(NativeMemoryAnalyzer analyzer) {
        if (!analyzer.isAvailable()) {
            System.err.println("Native memory analysis is not available on this platform.");
            return;
        }

        List<MemoryRegion> regions = analyzer.getMemoryRegions();

        System.out.println("MEMORY REGIONS");
        System.out.println("==========================================================================");
        System.out.printf("%-18s %-18s %-8s %12s %12s %s%n",
            "START", "END", "PERMS", "SIZE", "RSS", "FILE");
        System.out.println("--------------------------------------------------------------------------");

        for (MemoryRegion region : regions) {
            System.out.printf("%016x-%016x %-8s %,12d %,12d %s%n",
                region.getStartAddress(),
                region.getEndAddress(),
                region.getPermissions(),
                region.getSize(),
                region.getResidentSize(),
                truncate(region.getMappingFile(), 40));
        }

        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("Total: %d regions%n", regions.size());
    }

    private void printDiagnosis(NativeMemoryAnalyzer analyzer) {
        if (!analyzer.isAvailable()) {
            System.err.println("Native memory analysis is not available on this platform.");
            return;
        }

        NativeDiagnosis diagnosis = analyzer.analyzeNativeLeaks();

        System.out.println("NATIVE MEMORY DIAGNOSIS");
        System.out.println("==========================================================================");

        if (!diagnosis.getFindings().isEmpty()) {
            System.out.println("FINDINGS:");
            for (String finding : diagnosis.getFindings()) {
                System.out.printf("  • %s%n", finding);
            }
            System.out.println();
        }

        if (!diagnosis.getWarnings().isEmpty()) {
            System.out.println("WARNINGS:");
            for (String warning : diagnosis.getWarnings()) {
                System.out.printf("  ⚠ %s%n", warning);
            }
            System.out.println();
        }

        if (!diagnosis.getRecommendations().isEmpty()) {
            System.out.println("RECOMMENDATIONS:");
            for (String recommendation : diagnosis.getRecommendations()) {
                System.out.printf("  💡 %s%n", recommendation);
            }
        }

        if (!diagnosis.hasFindings() && !diagnosis.hasWarnings()) {
            System.out.println("✅ No issues found!");
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null || s.isEmpty()) {
            return "[anonymous]";
        }
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }

    private void handleAttach(NativeMemoryAnalyzer analyzer) {
        System.out.println("ATTACHING MEMDIAG AGENT");
        System.out.println("==========================================================================");

        // Check if this is an AgentNativeAnalyzer
        if (analyzer instanceof AgentNativeAnalyzer) {
            AgentNativeAnalyzer agentAnalyzer = (AgentNativeAnalyzer) analyzer;
            AgentClient client = agentAnalyzer.getClient();

            // Check if already reachable
            if (client.isReachable()) {
                System.out.println("✅ Agent is already running on " + client.getHost() + ":" + client.getPort());
                return;
            }

            // Try to attach
            String jarPath = agentJarPath;
            if (jarPath == null || jarPath.isEmpty()) {
                jarPath = AgentAttacher.findAgentJar();
            }

            if (jarPath == null) {
                System.err.println("❌ Cannot find memdiag-agent.jar");
                System.err.println();
                System.err.println("Please specify the path with --agent-jar <path>");
                return;
            }

            String p = getPid();
            if (p == null || p.isEmpty()) {
                System.err.println("❌ PID is required for dynamic attach");
                System.err.println();
                System.err.println("Please specify the target PID with --pid <pid>");
                return;
            }

            System.out.println("Attaching to PID " + p + " with agent: " + jarPath);
            if (AgentAttacher.attach(p, jarPath, agentPort)) {
                // Wait for agent to start
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Verify
                if (client.isReachable()) {
                    System.out.println("✅ Agent attached successfully!");
                    System.out.println("   Agent listening on " + agentHost + ":" + agentPort);
                } else {
                    System.out.println("⚠️ Agent attach reported success, but cannot connect to HTTP endpoint");
                }
            } else {
                System.err.println("❌ Failed to attach agent");
            }
            return;
        }

        // Not in agent mode - provide instructions
        System.err.println("❌ Agent attach requires agent mode.");
        System.err.println();
        System.err.println("To attach the MemDiag agent:");
        System.err.println();
        System.err.println("   Option 1: Start target JVM with agent (recommended):");
        System.err.println("     java -javaagent:/app/memdiag-agent.jar=port=6789 -jar your-app.jar");
        System.err.println();
        System.err.println("   Option 2: Dynamically attach agent:");
        System.err.println("     memdiag native --attach --agent-jar /app/memdiag-agent.jar --pid <pid>");
        System.err.println();
        System.err.println("Then all subsequent commands will automatically connect to the agent.");
    }

    private void handleDetach(NativeMemoryAnalyzer analyzer) {
        System.out.println("DETACHING MEMDIAG AGENT");
        System.out.println("==========================================================================");

        // Check if this is an AgentNativeAnalyzer
        if (analyzer instanceof AgentNativeAnalyzer) {
            AgentNativeAnalyzer agentAnalyzer = (AgentNativeAnalyzer) analyzer;
            AgentClient client = agentAnalyzer.getClient();

            if (client.isReachable()) {
                if (client.detach()) {
                    System.out.println("✅ Detach request sent to agent");
                } else {
                    System.err.println("❌ Failed to send detach request");
                }
            } else {
                System.out.println("Agent is not reachable");
            }
            return;
        }

        System.err.println("❌ Agent mode not active.");
        System.err.println("   Use --attach first, or start the target JVM with:");
        System.err.println("     java -javaagent:memdiag-agent.jar=port=6789 ...");
    }

    /**
     * Try to create an AgentNativeAnalyzer if agent is available or can be attached.
     *
     * @return An AgentNativeAnalyzer, or null if agent mode is not available
     */
    private NativeMemoryAnalyzer tryCreateAgentAnalyzer() {
        // First, check if agent jar path is specified - try to attach
        String p = getPid();
        if (agentJarPath != null && p != null && !p.isEmpty()) {
            String jarPath = agentJarPath;
            if (agentJarPath.isEmpty()) {
                jarPath = AgentAttacher.findAgentJar();
            }
            if (jarPath != null) {
                AgentClient client = new AgentClient(agentHost, agentPort);
                return new AgentNativeAnalyzer(client, jarPath, p);
            }
        }

        // Check if agent is already running (reachable)
        AgentClient client = new AgentClient(agentHost, agentPort);
        if (client.isReachable()) {
            return new AgentNativeAnalyzer(client, null, p);
        }

        return null;
    }
}
