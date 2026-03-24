package com.memdiag.cli.commands;

import com.memdiag.core.nativeapi.LibraryMapping;
import com.memdiag.core.nativeapi.MemoryRegion;
import com.memdiag.core.nativeapi.NativeDiagnosis;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzer;
import com.memdiag.core.nativeapi.NativeMemoryAnalyzerFactory;
import com.memdiag.core.nativeapi.NativeMemorySummary;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "native", description = "Native memory analysis (Linux only)")
public class NativeCommand extends BaseCommand {

    @CommandLine.Option(names = {"--status"}, description = "Check if native analysis is available")
    private boolean status;

    @CommandLine.Option(names = {"--summary"}, description = "Show native memory summary")
    private boolean summary;

    @CommandLine.Option(names = {"--regions"}, description = "Show memory regions")
    private boolean regions;

    @CommandLine.Option(names = {"--diagnose"}, description = "Run native leak diagnosis")
    private boolean diagnose;

    @Override
    public void run() {
        NativeMemoryAnalyzer analyzer;
        if (pid != null && !pid.isEmpty()) {
            analyzer = NativeMemoryAnalyzerFactory.getInstance(pid);
        } else {
            analyzer = NativeMemoryAnalyzerFactory.getInstance();
        }

        if (status) {
            printStatus(analyzer);
        } else if (summary) {
            printSummary(analyzer);
        } else if (regions) {
            printRegions(analyzer);
        } else if (diagnose) {
            printDiagnosis(analyzer);
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

        if (!analyzer.isAvailable()) {
            System.out.println();
            System.out.println("NOTE: Native memory analysis requires Linux with /proc filesystem mounted.");
        }
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
}
