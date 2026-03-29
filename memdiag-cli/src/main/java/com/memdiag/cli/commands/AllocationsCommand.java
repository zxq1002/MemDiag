package com.memdiag.cli.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.memdiag.core.agent.AgentClient;
import picocli.CommandLine;

@CommandLine.Command(name = "allocations", description = "Show allocation statistics and trends", mixinStandardHelpOptions = true)
public class AllocationsCommand extends BaseCommand {

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "10", description = "Limit top types display")
    private int limit;

    @Override
    public void run() {
        if (!isAgentMode()) {
            System.err.println("Error: 'allocations' command requires agent mode (-a host:port)");
            return;
        }

        AgentClient client = createAgentClient();
        try {
            JsonObject response = client.getAllocationsSummary();
            if (response == null || !response.get("success").getAsBoolean()) {
                String error = response != null && response.has("error") ? response.get("error").getAsString() : "Unknown error";
                System.err.println("Failed to get allocation summary: " + error);
                return;
            }

            JsonObject data = response.getAsJsonObject("data");
            if (data == null) {
                System.err.println("No data received from agent");
                return;
            }

            displaySummary(data);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void displaySummary(JsonObject data) {
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

    private String truncate(String s, int maxLen) {
        if (s == null) return "null";
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }
}
