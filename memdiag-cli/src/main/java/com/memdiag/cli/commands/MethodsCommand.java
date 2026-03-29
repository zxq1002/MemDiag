package com.memdiag.cli.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.memdiag.core.agent.AgentClient;
import picocli.CommandLine;

@CommandLine.Command(name = "methods", description = "Show method monitoring statistics", mixinStandardHelpOptions = true)
public class MethodsCommand extends BaseCommand {

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20", description = "Limit method list")
    private int limit;

    @CommandLine.Option(names = {"-s", "--sort"}, defaultValue = "time", description = "Sort by: time, count")
    private String sort;

    @Override
    public void run() {
        if (!isAgentMode()) {
            System.err.println("Error: 'methods' command requires agent mode (-a host:port)");
            return;
        }

        AgentClient client = createAgentClient();
        try {
            JsonObject response = client.getMethodsStats(limit);
            if (response == null || !response.get("success").getAsBoolean()) {
                String error = response != null && response.has("error") ? response.get("error").getAsString() : "Unknown error";
                System.err.println("Failed to get method stats: " + error);
                return;
            }

            JsonObject data = response.getAsJsonObject("data");
            if (data == null) {
                System.err.println("No data received from agent");
                return;
            }

            displayStats(data);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void displayStats(JsonObject data) {
        System.out.println("Method Monitoring Statistics");
        System.out.println("============================");

        int totalMethods = data.has("totalMethods") ? data.get("totalMethods").getAsInt() : 0;
        System.out.printf("Total Methods Monitored: %d%n%n", totalMethods);

        String key = "count".equals(sort) ? "topByCount" : "topByTotalTime";
        JsonArray methods = data.has(key) ? data.getAsJsonArray(key) : null;

        if (methods == null || methods.size() == 0) {
            System.out.println("No method statistics available.");
            return;
        }

        System.out.printf("%-50s %10s %12s %12s %12s%n",
                "METHOD", "COUNT", "TOTAL(ms)", "AVG(ms)", "MAX(ms)");
        System.out.println("---------------------------------------------------------------------------------------------------------");

        for (int i = 0; i < methods.size(); i++) {
            JsonObject stats = methods.get(i).getAsJsonObject();
            String className = stats.has("className") ? stats.get("className").getAsString() : null;
            String methodName = stats.has("methodName") ? stats.get("methodName").getAsString() : null;
            String fullMethod = className != null && methodName != null ? className + "#" + methodName : "unknown";

            System.out.printf("%-50s %,10d %,12.2f %,12.2f %,12.2f%n",
                    truncate(fullMethod, 50),
                    stats.has("invocationCount") ? stats.get("invocationCount").getAsLong() : 0L,
                    stats.has("totalTimeMs") ? stats.get("totalTimeMs").getAsDouble() : 0.0,
                    stats.has("averageTimeMs") ? stats.get("averageTimeMs").getAsDouble() : 0.0,
                    stats.has("maxTimeMs") ? stats.get("maxTimeMs").getAsDouble() : 0.0);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }
}
