package com.memdiag.cli.commands;

import com.memdiag.cli.client.AgentClient;
import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.output.HtmlFormatter;
import com.memdiag.core.output.JsonFormatter;
import com.memdiag.core.output.ReportFormatter;
import com.memdiag.core.output.TextFormatter;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(
    name = "report",
    description = "Generate complete diagnosis report",
    mixinStandardHelpOptions = true
)
public class ReportCommand extends BaseCommand {

    @CommandLine.Option(
        names = {"-f", "--format"},
        description = "Output format: text, html, json (default: text)",
        defaultValue = "text"
    )
    private String format;

    @CommandLine.Option(
        names = {"-o", "--output"},
        description = "Output file path (default: stdout)"
    )
    private String outputFile;

    @CommandLine.Option(
        names = {"-l", "--limit"},
        description = "Class limit (default: 50)",
        defaultValue = "50"
    )
    private int limit;

    @Override
    public void run() {
        try {
            HeapHistogram histogram;
            ThreadDump threadDump;
            DiagnosisResult diagnosis;

            if (isAgentMode()) {
                System.err.println("Connecting to agent " + agent + "...");
                AgentClient client = createAgentClient();
                histogram = client.getHeapHistogram(limit);
                threadDump = client.getThreadDump();
                diagnosis = client.getDiagnosis();
            } else {
                JmxClient client;
                if (pid != null && !pid.isEmpty()) {
                    System.err.println("Attaching to JVM PID " + pid + "...");
                    client = JmxClient.attachToPid(pid);
                } else {
                    System.err.println("Attaching to current JVM...");
                    client = JmxClient.attachToCurrentJvm();
                }

                HeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(client);
                ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
                DiagnosisEngine engine = new DiagnosisEngine(client, heapAnalyzer, threadAnalyzer);

                System.err.println("Collecting heap information...");
                histogram = heapAnalyzer.getHistogram(limit);

                System.err.println("Collecting thread information...");
                threadDump = threadAnalyzer.getThreadDump();

                System.err.println("Analyzing...");
                diagnosis = engine.analyze();
            }

            System.err.println("Generating report...");
            ReportFormatter formatter = createFormatter();
            String report = formatter.format(histogram, threadDump, diagnosis);

            outputReport(report);
        } catch (Exception e) {
            System.err.println("Failed to generate report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ReportFormatter createFormatter() {
        switch (format.toLowerCase()) {
            case "html":
                return new HtmlFormatter(limit);
            case "json":
                return new JsonFormatter(limit, true);
            case "text":
            default:
                return new TextFormatter(limit);
        }
    }

    private void outputReport(String report) throws IOException {
        if (outputFile != null) {
            Path path = Paths.get(outputFile);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(report);
            }

            System.err.println("Report saved to: " + outputFile);
        } else {
            System.out.println(report);
        }
    }
}
