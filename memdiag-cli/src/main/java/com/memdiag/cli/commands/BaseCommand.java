package com.memdiag.cli.commands;

import com.memdiag.cli.MemDiagCli;
import com.memdiag.core.agent.AgentClient;
import picocli.CommandLine;

public abstract class BaseCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "PID (optional for current JVM)", arity = "0..1")
    protected String pidParam;

    @CommandLine.Option(names = {"--pid"}, description = "Target JVM process ID")
    protected String pidOption;

    @CommandLine.ParentCommand
    protected MemDiagCli parent;

    protected String getPid() {
        if (pidOption != null && !pidOption.isEmpty()) {
            return pidOption;
        }
        return pidParam;
    }

    protected boolean isAgentMode() {
        return parent != null && parent.agent != null && !parent.agent.isEmpty();
    }

    protected AgentClient createAgentClient() {
        String[] parts = parent.agent.split(":", 2);
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6789;
        return new AgentClient(host, port);
    }
}
