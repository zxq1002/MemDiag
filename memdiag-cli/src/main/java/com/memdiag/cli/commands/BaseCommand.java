package com.memdiag.cli.commands;

import com.memdiag.cli.client.AgentClient;
import picocli.CommandLine;

public abstract class BaseCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "PID (optional for current JVM)", arity = "0..1")
    protected String pidParam;

    @CommandLine.Option(names = {"--pid"}, description = "Target JVM process ID")
    protected String pidOption;

    @CommandLine.Option(names = {"-a", "--agent"}, description = "Connect to agent (format: host:port)")
    protected String agent;

    protected String getPid() {
        if (pidOption != null && !pidOption.isEmpty()) {
            return pidOption;
        }
        return pidParam;
    }

    protected boolean isAgentMode() {
        return agent != null && !agent.isEmpty();
    }

    protected AgentClient createAgentClient() {
        String[] parts = agent.split(":", 2);
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6789;
        return new AgentClient(host, port);
    }
}
