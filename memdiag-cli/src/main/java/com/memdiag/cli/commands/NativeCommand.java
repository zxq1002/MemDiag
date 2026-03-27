package com.memdiag.cli.commands;

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

    @CommandLine.Option(names = {"--attach"}, description = "Attach native agent to target process (requires JVMTI)")
    private boolean attach;

    @CommandLine.Option(names = {"--detach"}, description = "Detach native agent from target process (requires JVMTI)")
    private boolean detach;

    @CommandLine.Option(names = {"--start-trace"}, description = "Start native allocation tracing (requires JVMTI)")
    private boolean startTrace;

    @CommandLine.Option(names = {"--stop-trace"}, description = "Stop native allocation tracing (requires JVMTI)")
    private boolean stopTrace;

    @CommandLine.Option(names = {"--allocation-sites"}, description = "Show top allocation sites (requires JVMTI tracing)")
    private boolean allocationSites;

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20", description = "Limit for allocation sites output")
    private int limit;

    // 用于演示的分配点检测器（实际会从 native 层获取事件）
    private static final AllocationPairDetector demoDetector = new AllocationPairDetector();

    @Override
    public void run() {
        // Determine if we need JVMTI features
        boolean requiresJVMTI = attach || detach || startTrace || stopTrace || allocationSites;

        NativeMemoryAnalyzer analyzer;
        if (requiresJVMTI) {
            if (pid != null && !pid.isEmpty()) {
                analyzer = NativeMemoryAnalyzerFactory.getInstanceWithJVMTI(pid);
            } else {
                analyzer = NativeMemoryAnalyzerFactory.getInstanceWithJVMTI();
            }
        } else {
            if (pid != null && !pid.isEmpty()) {
                analyzer = NativeMemoryAnalyzerFactory.getInstance(pid);
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
        } else if (startTrace) {
            handleStartTrace(analyzer);
        } else if (stopTrace) {
            handleStopTrace(analyzer);
        } else if (allocationSites) {
            printAllocationSites(analyzer);
        } else {
            printStatus(analyzer);
        }
    }

    private void printStatus(NativeMemoryAnalyzer analyzer) {
        System.out.println("NATIVE MEMORY ANALYSIS STATUS");
        System.out.println("==========================================================================");
        System.out.printf("Available: %s%n", analyzer.isAvailable() ? "✅ Yes" : "❌ No");
        System.out.printf("Platform: %s%n", analyzer.getPlatform());
        System.out.printf("Requires Agent: %s%n", analyzer.requiresAgent() ? "Yes" : "No");
        System.out.printf("Agent Attached: %s%n", analyzer.isAgentAttached() ? "Yes" : "No");
        System.out.printf("JVMTI Support: %s%n", isJVMTIAvailable(analyzer) ? "✅ Yes" : "❌ No (install libmemdiag-agent.so for advanced features)");

        // Check if native library is loaded
        String libraryStatus = getNativeLibraryStatus();
        if (libraryStatus != null) {
            System.out.printf("Native Library: %s%n", libraryStatus);
        }

        if (!analyzer.isAvailable()) {
            System.out.println();
            System.out.println("NOTE: Native memory analysis requires Linux with /proc filesystem mounted.");
        }
        if (!isJVMTIAvailable(analyzer)) {
            System.out.println();
            System.out.println("NOTE: JVMTI advanced features (--attach, --trace) require libmemdiag-agent.so.");
            System.out.println("      Basic features (--status, --summary, --regions, --diagnose) are still available.");
        }
    }

    private String getNativeLibraryStatus() {
        try {
            Class<?> nativeLoaderClass = Class.forName("com.memdiag.nativeimpl.NativeLoader");
            java.lang.reflect.Method isLoadedMethod = nativeLoaderClass.getMethod("isLoaded");
            boolean isLoaded = (Boolean) isLoadedMethod.invoke(null);

            if (isLoaded) {
                java.lang.reflect.Method getPathMethod = nativeLoaderClass.getMethod("getLoadedLibraryPath");
                String path = (String) getPathMethod.invoke(null);
                return "✅ Loaded (" + path + ")";
            } else {
                return "❌ Not loaded";
            }
        } catch (Exception e) {
            return "❌ Not available (NativeLoader not found)";
        }
    }

    private boolean isJVMTIAvailable(NativeMemoryAnalyzer analyzer) {
        return analyzer.getClass().getName().contains("JVMTINativeAnalyzer");
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
        System.out.println("ATTACHING NATIVE AGENT");
        System.out.println("==========================================================================");

        // Check native library status first
        String libraryStatus = getNativeLibraryStatus();
        if (libraryStatus != null && libraryStatus.contains("❌")) {
            System.err.println("❌ Native library not loaded.");
            System.err.println("   Status: " + libraryStatus);
            System.err.println();
            System.err.println("To use this feature, ensure:");
            System.err.println("  1. libmemdiag-agent.so is built and in the classpath");
            System.err.println("  2. You're running on Linux");
            System.err.println("  3. The JVM has attach permissions");
            return;
        }

        if (!isJVMTIAvailable(analyzer)) {
            System.err.println("❌ JVMTI agent is not available.");
            System.err.println();
            System.err.println("To use this feature, ensure:");
            System.err.println("  1. libmemdiag-agent.so is built and in the classpath");
            System.err.println("  2. You're running on Linux");
            System.err.println("  3. The JVM has attach permissions");
            return;
        }

        if (analyzer.isAgentAttached()) {
            System.out.println("✅ Agent is already attached");
            return;
        }

        if (analyzer.attachAgent()) {
            System.out.println("✅ Agent attached successfully");
        } else {
            System.err.println("❌ Failed to attach agent");
            System.err.println();
            System.err.println("Troubleshooting:");
            System.err.println("  - Ensure you're running on Linux with JVMTI support");
            System.err.println("  - Verify the native library is available");
            System.err.println("  - Check that you have permission to attach to the target process");
        }
    }

    private void handleDetach(NativeMemoryAnalyzer analyzer) {
        System.out.println("DETACHING NATIVE AGENT");
        System.out.println("==========================================================================");

        if (!isJVMTIAvailable(analyzer)) {
            System.err.println("❌ JVMTI agent is not available.");
            return;
        }

        if (!analyzer.isAgentAttached()) {
            System.out.println("Agent is not attached");
            return;
        }

        if (analyzer.detachAgent()) {
            System.out.println("✅ Agent detached successfully");
        } else {
            System.err.println("❌ Failed to detach agent");
        }
    }

    private void handleStartTrace(NativeMemoryAnalyzer analyzer) {
        System.out.println("STARTING ALLOCATION TRACING");
        System.out.println("==========================================================================");

        if (!isJVMTIAvailable(analyzer)) {
            System.err.println("❌ JVMTI agent is not available.");
            System.err.println();
            System.err.println("To use allocation tracing, you need libmemdiag-agent.so.");
            return;
        }

        if (!analyzer.isAgentAttached()) {
            System.err.println("❌ Agent is not attached. Please run:");
            System.err.println("   memdiag native --attach [--pid <pid>]");
            return;
        }

        if (analyzer.isTrackingEnabled()) {
            System.out.println("Tracing is already enabled");
            return;
        }

        if (analyzer.startAllocationTracking()) {
            System.out.println("✅ Allocation tracing started");
            System.out.println();
            System.out.println("Next steps:");
            System.out.println("  - Let the application run to capture allocations");
            System.out.println("  - Use --allocation-sites to view results");
            System.out.println("  - Use --stop-trace to stop tracing");
        } else {
            System.err.println("❌ Failed to start allocation tracing");
        }
    }

    private void handleStopTrace(NativeMemoryAnalyzer analyzer) {
        System.out.println("STOPPING ALLOCATION TRACING");
        System.out.println("==========================================================================");

        if (!isJVMTIAvailable(analyzer)) {
            System.err.println("❌ JVMTI agent is not available.");
            return;
        }

        if (!analyzer.isTrackingEnabled()) {
            System.out.println("Tracing is not enabled");
            return;
        }

        if (analyzer.stopAllocationTracking()) {
            System.out.println("✅ Allocation tracing stopped");
            System.out.println();
            System.out.println("To view results:");
            System.out.println("  memdiag native --allocation-sites [--pid <pid>]");
        } else {
            System.err.println("❌ Failed to stop allocation tracing");
        }
    }

    private void printAllocationSites(NativeMemoryAnalyzer analyzer) {
        System.out.println("TOP ALLOCATION SITES");
        System.out.println("==========================================================================");

        if (isJVMTIAvailable(analyzer) && analyzer.isTrackingEnabled()) {
            // 显示来自 native 追踪器的实际数据
            long totalAllocated = analyzer.getTotalAllocated();
            long liveBytes = analyzer.getLiveBytes();

            System.out.printf("Total allocated:  %,15d bytes (%,.2f MB)%n",
                totalAllocated, totalAllocated / (1024.0 * 1024.0));
            System.out.printf("Live bytes:       %,15d bytes (%,.2f MB)%n",
                liveBytes, liveBytes / (1024.0 * 1024.0));
            System.out.println();
        } else if (!isJVMTIAvailable(analyzer)) {
            System.err.println("⚠ JVMTI agent not available. Showing demo data only.");
            System.err.println("  To see real allocation sites, install libmemdiag-agent.so and use:");
            System.err.println("    memdiag native --attach");
            System.err.println("    memdiag native --start-trace");
            System.err.println("    memdiag native --allocation-sites");
            System.out.println();
        } else if (!analyzer.isTrackingEnabled()) {
            System.err.println("⚠ Tracing is not enabled. Showing demo data only.");
            System.err.println("  To see real allocation sites:");
            System.err.println("    memdiag native --attach");
            System.err.println("    memdiag native --start-trace");
            System.err.println("    memdiag native --allocation-sites");
            System.out.println();
        }

        // 显示演示数据（模拟 LeakSimulator 的分配点）
        System.out.println("DEMO: Allocation sites (simulated for demo)");
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("%-8s %15s %15s %15s  %s%n",
            "COUNT", "TOTAL", "LIVE", "FREED", "SITE");
        System.out.println("--------------------------------------------------------------------------");

        // 模拟 LeakSimulator 的分配模式
        printDemoAllocationSite("LeakSimulator.simulateLeak", 150, 15360000, 15360000, 0);
        printDemoAllocationSite("LeakSimulator.allocateBuffers", 50, 5120000, 0, 5120000);
        printDemoAllocationSite("ByteBuffer.allocateDirect", 200, 20480000, 10240000, 10240000);
        printDemoAllocationSite("Unsafe.allocateMemory", 75, 7680000, 3840000, 3840000);

        System.out.println("--------------------------------------------------------------------------");
        System.out.println();
        System.out.println("Note: In a real run, these would show actual stack traces");
        System.out.println("      from the native allocation tracker.");
    }

    private void printDemoAllocationSite(String site, int count, long total, long live, long freed) {
        System.out.printf("%,8d %,15d %,15d %,15d  %s%n",
            count, total, live, freed, site);
    }
}
