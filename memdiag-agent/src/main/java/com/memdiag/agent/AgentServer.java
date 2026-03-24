package com.memdiag.agent;

import java.lang.instrument.Instrumentation;

public class AgentServer {

    private final String host;
    private final int port;
    private final Instrumentation instrumentation;

    public AgentServer(String host, int port, Instrumentation instrumentation) {
        this.host = host;
        this.port = port;
        this.instrumentation = instrumentation;
    }

    public void start() throws Exception {
        // HTTP server will be implemented in a later phase
        System.out.println("[MemDiag] Agent server placeholder - HTTP API coming soon");
    }

    public void stop() throws Exception {
        // No-op for now
    }
}
