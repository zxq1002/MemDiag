package com.memdiag.cli.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.memdiag.core.agent.AgentClient;
import picocli.CommandLine;

@CommandLine.Command(name = "allocations", description = "Show allocation statistics and trends", mixinStandardHelpOptions = true)
public class AllocationsCommand extends BaseCommand {

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "10", description = "Limit top types/events display")
    private int limit;

    @CommandLine.Option(names = {"--summary"}, description = "Show allocation summary (default)")
    private boolean showSummary;

    @CommandLine.Option(names = {"--recent"}, description = "Show recent allocation events")
    private boolean showRecent;

    @CommandLine.Option(names = {"--top"}, description = "Show top allocation types")
    private boolean showTop;

    @CommandLine.Option(names = {"--stats"}, description = "Show allocation statistics")
    private boolean showStats;

    @CommandLine.Option(names = {"--rate"}, description = "Show allocation rate")
    private boolean showRate;

    @Override
    public void run() {
        if (!isAgentMode()) {
            System.err.println("Error: 'allocations' command requires agent mode (-a host:port)");
            return;
        }

        AgentClient client = createAgentClient();
        try {
            if (showRecent) {
                displayRecent(client);
            } else if (showTop) {
                displayTop(client);
            } else if (showStats) {
                displayStats(client);
            } else if (showRate) {
                displayRate(client);
            } else {
                displaySummary(client);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private JsonObject getAndValidate(JsonObject response, String operation) {
        if (response == null) {
            System.err.println("Failed to " + operation + ": no response from agent");
            return null;
        }
        if (!response.has("success") || !response.get("success").getAsBoolean()) {
            String error = response.has("error") ? response.get("error").getAsString() : "Unknown error";
            System.err.println("Failed to " + operation + ": " + error);
            return null;
        }
        return response.getAsJsonObject("data");
    }

    private void displaySummary(AgentClient client) {
        JsonObject data = getAndValidate(client.getAllocationsSummary(), "get allocation summary");
        if (data == null) return;

        System.out.println("Allocation Summary");
        System.out.println("==================");

        long totalAllocated = data.has("totalAllocated") ? data.get("totalAllocated").getAsLong() : 0L;
        long totalCount = data.has("allocationCount") ? data.get("allocationCount").getAsLong() : 0L;
        long currentRate = data.has("currentRateBytesPerSec") ? data.get("currentRateBytesPerSec").getAsLong() : 0L;
        String trend = data.has("trend") ? data.get("trend").getAsString() : "unknown";

        System.out.printf("Total Allocated:  %,d bytes (%,.2f MB)%n",
                totalAllocated, (double)totalAllocated / (1024 * 1024));
        System.out.printf("Total Count:      %,d allocations%n", totalCount);
        System.out.printf("Current Rate:     %,d bytes/sec (%,.2f MB/sec)%n",
                currentRate, (double)currentRate / (1024 * 1024));
        System.out.printf("Trend:            %s%n", trend);

        if (data.has("windowRates")) {
            JsonObject windowRates = data.getAsJsonObject("windowRates");
            System.out.println("\nAllocation Rates (bytes/sec):");
            for (String window : windowRates.keySet()) {
                System.out.printf("  %-5s: %,d%n", window, windowRates.get(window).getAsLong());
            }
        }

        if (data.has("topTypesBySize")) {
            JsonArray topBySize = data.getAsJsonArray("topTypesBySize");
            if (topBySize != null && topBySize.size() > 0) {
                System.out.println("\nTop Types by Size:");
                System.out.printf("%-50s %15s %12s%n", "TYPE NAME", "TOTAL SIZE", "COUNT");
                System.out.println("-------------------------------------------------------------------------------");
                int count = 0;
                for (int i = 0; i < topBySize.size() && count < limit; i++) {
                    JsonObject stats = topBySize.get(i).getAsJsonObject();
                    String typeName = stats.has("typeName") ? stats.get("typeName").getAsString() : "unknown";
                    long totalSize = stats.has("totalSize") ? stats.get("totalSize").getAsLong() : 0L;
                    long typeCount = stats.has("count") ? stats.get("count").getAsLong() : 0L;
                    System.out.printf("%-50s %,15d %,12d%n",
                            truncate(typeName, 50),
                            totalSize,
                            typeCount);
                    count++;
                }
            }
        }
    }

    private void displayRecent(AgentClient client) {
        JsonObject data = getAndValidate(client.getAllocationsRecent(limit), "get recent allocations");
        if (data == null) return;

        System.out.println("Recent Allocation Events");
        System.out.println("========================");

        if (data.has("events")) {
            JsonArray events = data.getAsJsonArray("events");
            if (events != null && events.size() > 0) {
                System.out.printf("%-20s %-50s %12s%n", "TIMESTAMP", "TYPE", "SIZE");
                System.out.println("-------------------------------------------------------------------------------");
                for (int i = 0; i < events.size(); i++) {
                    JsonObject event = events.get(i).getAsJsonObject();
                    String timestamp = event.has("timestamp") ? event.get("timestamp").getAsString() : "";
                    String typeName = event.has("typeName") ? event.get("typeName").getAsString() : "unknown";
                    long size = event.has("size") ? event.get("size").getAsLong() : 0L;
                    System.out.printf("%-20s %-50s %,12d%n",
                            truncate(timestamp, 20),
                            truncate(typeName, 50),
                            size);
                }
            } else {
                System.out.println("No recent allocation events");
            }
        }
    }

    private void displayTop(AgentClient client) {
        JsonObject data = getAndValidate(client.getAllocationsTop(limit), "get top allocations");
        if (data == null) return;

        System.out.println("Top Allocation Types");
        System.out.println("====================");

        System.out.printf("%-50s %15s %12s %15s%n", "TYPE NAME", "TOTAL SIZE", "COUNT", "AVG SIZE");
        System.out.println("----------------------------------------------------------------------------------------");

        if (data.has("types")) {
            JsonArray types = data.getAsJsonArray("types");
            if (types != null && types.size() > 0) {
                for (int i = 0; i < types.size(); i++) {
                    JsonObject stats = types.get(i).getAsJsonObject();
                    String typeName = stats.has("typeName") ? stats.get("typeName").getAsString() : "unknown";
                    long totalSize = stats.has("totalSize") ? stats.get("totalSize").getAsLong() : 0L;
                    long count = stats.has("count") ? stats.get("count").getAsLong() : 0L;
                    long avgSize = count > 0 ? totalSize / count : 0L;
                    System.out.printf("%-50s %,15d %,12d %,15d%n",
                            truncate(typeName, 50),
                            totalSize,
                            count,
                            avgSize);
                }
            } else {
                System.out.println("No allocation type data available");
            }
        }
    }

    private void displayStats(AgentClient client) {
        JsonObject data = getAndValidate(client.getAllocationsStats(), "get allocation stats");
        if (data == null) return;

        System.out.println("Allocation Statistics");
        System.out.println("=====================");

        for (String key : data.keySet()) {
            JsonElement elem = data.get(key);
            if (elem.isJsonPrimitive()) {
                System.out.printf("%-25s: ", key);
                if (elem.getAsJsonPrimitive().isNumber()) {
                    System.out.printf("%,d%n", elem.getAsLong());
                } else {
                    System.out.printf("%s%n", elem.getAsString());
                }
            }
        }
    }

    private void displayRate(AgentClient client) {
        JsonObject data = getAndValidate(client.getAllocationsRate(), "get allocation rate");
        if (data == null) return;

        System.out.println("Allocation Rate");
        System.out.println("===============");

        if (data.has("currentRateBytesPerSec")) {
            long rate = data.get("currentRateBytesPerSec").getAsLong();
            System.out.printf("Current Rate: %,d bytes/sec (%,.2f MB/sec)%n",
                    rate, (double)rate / (1024 * 1024));
        }

        if (data.has("windowRates")) {
            JsonObject windowRates = data.getAsJsonObject("windowRates");
            System.out.println("\nRates by Window:");
            for (String window : windowRates.keySet()) {
                long rate = windowRates.get(window).getAsLong();
                System.out.printf("  %-5s: %,12d bytes/sec (%,.2f MB/sec)%n",
                        window, rate, (double)rate / (1024 * 1024));
            }
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }
}
