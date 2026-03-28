package com.memdiag.cli.commands;

import com.memdiag.cli.client.AgentClient;
import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

@CommandLine.Command(name = "diagnose", description = "Run diagnosis and show issues", mixinStandardHelpOptions = true)
public class DiagnoseCommand extends BaseCommand {

    @Override
    public void run() {
        DiagnosisResult result;

        if (isAgentMode()) {
            AgentClient client = createAgentClient();
            try {
                result = client.getDiagnosis();
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
            HeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(client);
            ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
            DiagnosisEngine engine = new DiagnosisEngine(client, heapAnalyzer, threadAnalyzer);
            result = engine.analyze();
        }

        System.out.println("DIAGNOSIS REPORT");
        System.out.println("==========================================================================");
        System.out.printf("Generated at: %s%n", result.getTimestamp());
        System.out.printf("Heap: %,d bytes used / %,d bytes committed%n",
            result.getTotalHeapUsed(), result.getTotalHeapCommitted());
        System.out.printf("Threads: %d active%n", result.getThreadCount());
        System.out.println();

        if (result.getSummary() != null) {
            System.out.println("SUMMARY:");
            System.out.println(result.getSummary());
            System.out.println();
        }

        if (!result.getCriticalIssues().isEmpty()) {
            System.out.println("CRITICAL ISSUES:");
            System.out.println("--------------------------------------------------------------------------");
            for (Issue issue : result.getCriticalIssues()) {
                printIssue(issue);
            }
            System.out.println();
        }

        if (!result.getWarningIssues().isEmpty()) {
            System.out.println("WARNING ISSUES:");
            System.out.println("--------------------------------------------------------------------------");
            for (Issue issue : result.getWarningIssues()) {
                printIssue(issue);
            }
            System.out.println();
        }

        if (!result.getInfoIssues().isEmpty()) {
            System.out.println("INFO ISSUES:");
            System.out.println("--------------------------------------------------------------------------");
            for (Issue issue : result.getInfoIssues()) {
                printIssue(issue);
            }
            System.out.println();
        }

        if (result.getIssues().isEmpty()) {
            System.out.println("✅ No issues found!");
        }
    }

    private void printIssue(Issue issue) {
        String severityIcon;
        switch (issue.getSeverity()) {
            case CRITICAL: severityIcon = "🔴"; break;
            case WARNING: severityIcon = "🟡"; break;
            case INFO: severityIcon = "🟢"; break;
            default: severityIcon = "";
        }

        System.out.printf("%s [%s] %s%n", severityIcon, issue.getType(), issue.getTitle());
        if (issue.getDescription() != null) {
            System.out.printf("   %s%n", issue.getDescription());
        }
        if (issue.getAffectedClassName() != null) {
            System.out.printf("   Affected: %s", issue.getAffectedClassName());
            if (issue.getAffectedObjectCount() != null) {
                System.out.printf(" (%,d instances", issue.getAffectedObjectCount());
                if (issue.getAffectedBytes() != null) {
                    System.out.printf(", %,d bytes", issue.getAffectedBytes());
                }
                System.out.print(")");
            }
            System.out.println();
        }
        if (!issue.getRecommendations().isEmpty()) {
            System.out.println("   Recommendations:");
            for (Recommendation rec : issue.getRecommendations()) {
                System.out.printf("   - [%s] %s%n", rec.getPriority(), rec.getTitle());
                if (rec.getDescription() != null) {
                    System.out.printf("     %s%n", rec.getDescription());
                }
                if (rec.getAction() != null) {
                    System.out.printf("     Action: %s%n", rec.getAction());
                }
            }
        }
        System.out.println();
    }
}
