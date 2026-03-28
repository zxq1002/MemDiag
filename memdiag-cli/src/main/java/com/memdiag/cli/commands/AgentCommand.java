package com.memdiag.cli.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.memdiag.core.agent.AgentClient;
import picocli.CommandLine;

import java.util.Map;
import java.util.Set;

/**
 * Command for interacting with a running MemDiag agent.
 */
@CommandLine.Command(name = "agent", description = "Interact with a running MemDiag agent",
        mixinStandardHelpOptions = true,
        subcommands = {AgentCommand.StatusCommand.class, AgentCommand.ConfigCommand.class,
                AgentCommand.MetricsCommand.class, AgentCommand.AllocationsCommand.class,
                AgentCommand.JVMTICommand.class})
public class AgentCommand {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @CommandLine.Option(names = {"--host"}, defaultValue = "localhost", description = "Agent host")
    private String host = "localhost";

    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "6789", description = "Agent port")
    private int port = 6789;

    protected AgentClient createClient() {
        return new AgentClient(host, port);
    }

    protected void printJson(JsonObject json) {
        if (json != null) {
            System.out.println(gson.toJson(json));
        } else {
            System.err.println("Failed to get response from agent");
        }
    }

    /**
     * Subcommand: agent status
     */
    @CommandLine.Command(name = "status", description = "Show agent status")
    public static class StatusCommand implements Runnable {
        @CommandLine.ParentCommand
        private AgentCommand parent;

        @Override
        public void run() {
            System.out.println("AGENT STATUS");
            System.out.println("=".repeat(80));

            AgentClient client = parent.createClient();

            if (!client.isReachable()) {
                System.err.println("❌ Agent not reachable at " + client.getHost() + ":" + client.getPort());
                System.err.println("   Make sure the agent is running and the port is correct.");
                return;
            }

            System.out.println("✅ Agent is reachable");
            System.out.println();

            JsonObject status = client.getAgentStatus();
            if (status != null && status.has("data")) {
                JsonObject data = status.get("data").getAsJsonObject();
                System.out.println("Status details:");
                System.out.println(gson.toJson(data));
            } else {
                parent.printJson(status);
            }
        }
    }

    /**
     * Subcommand: agent config
     */
    @CommandLine.Command(name = "config", description = "Show or update agent configuration")
    public static class ConfigCommand implements Runnable {
        @CommandLine.ParentCommand
        private AgentCommand parent;

        @Override
        public void run() {
            System.out.println("AGENT CONFIGURATION");
            System.out.println("=".repeat(80));

            AgentClient client = parent.createClient();

            if (!client.isReachable()) {
                System.err.println("❌ Agent not reachable");
                return;
            }

            JsonObject config = client.getAgentConfig();
            parent.printJson(config);
        }
    }

    /**
     * Subcommand: agent metrics
     */
    @CommandLine.Command(name = "metrics", description = "Show agent metrics")
    public static class MetricsCommand implements Runnable {
        @CommandLine.ParentCommand
        private AgentCommand parent;

        @Override
        public void run() {
            System.out.println("AGENT METRICS");
            System.out.println("=".repeat(80));

            AgentClient client = parent.createClient();

            if (!client.isReachable()) {
                System.err.println("❌ Agent not reachable");
                return;
            }

            JsonObject metrics = client.getAgentMetrics();
            parent.printJson(metrics);
        }
    }

    /**
     * Subcommand: agent allocations
     */
    @CommandLine.Command(name = "allocations", description = "Show allocation statistics")
    public static class AllocationsCommand implements Runnable {
        @CommandLine.ParentCommand
        private AgentCommand parent;

        @CommandLine.Option(names = {"--recent"}, description = "Show recent allocation events")
        private boolean recent;

        @CommandLine.Option(names = {"--stats"}, description = "Show allocation statistics")
        private boolean stats;

        @CommandLine.Option(names = {"--top"}, description = "Show top allocation types")
        private boolean top;

        @CommandLine.Option(names = {"--rate"}, description = "Show allocation rate")
        private boolean rate;

        @CommandLine.Option(names = {"--summary"}, description = "Show full allocation summary")
        private boolean summary;

        @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20", description = "Limit number of results")
        private int limit = 20;

        @Override
        public void run() {
            System.out.println("ALLOCATION ANALYSIS");
            System.out.println("=".repeat(80));

            AgentClient client = parent.createClient();

            if (!client.isReachable()) {
                System.err.println("❌ Agent not reachable");
                return;
            }

            JsonObject response;
            if (recent) {
                response = client.getAllocationsRecent(limit);
            } else if (stats) {
                response = client.getAllocationsStats();
            } else if (top) {
                response = client.getAllocationsTop(limit);
            } else if (rate) {
                response = client.getAllocationsRate();
            } else if (summary) {
                response = client.getAllocationsSummary();
            } else {
                // Default: show summary
                response = client.getAllocationsSummary();
            }

            parent.printJson(response);
        }
    }

    /**
     * Subcommand: agent jvmti
     */
    @CommandLine.Command(name = "jvmti", description = "Show JVMTI status")
    public static class JVMTICommand implements Runnable {
        @CommandLine.ParentCommand
        private AgentCommand parent;

        @Override
        public void run() {
            System.out.println("JVMTI STATUS");
            System.out.println("=".repeat(80));

            AgentClient client = parent.createClient();

            if (!client.isReachable()) {
                System.err.println("❌ Agent not reachable");
                return;
            }

            JsonObject jvmtiStatus = client.getJvmtiStatus();
            parent.printJson(jvmtiStatus);
        }
    }
}
