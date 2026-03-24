package com.memdiag.agent;

import java.lang.instrument.Instrumentation;

public class MemDiagAgent {

    private static volatile Instrumentation instrumentation;
    private static volatile AgentServer server;

    public static void premain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst);
    }

    private static synchronized void initialize(String agentArgs, Instrumentation inst) {
        if (instrumentation != null) {
            System.err.println("[MemDiag] Agent already initialized");
            return;
        }

        instrumentation = inst;
        System.out.println("[MemDiag] Agent initialized");

        // Parse agent arguments
        int port = 6789;
        String host = "localhost";
        boolean startServer = true;

        if (agentArgs != null && !agentArgs.isEmpty()) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                String[] parts = arg.split("=", 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";
                switch (key) {
                    case "port":
                        try {
                            port = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            System.err.println("[MemDiag] Invalid port: " + value);
                        }
                        break;
                    case "host":
                        host = value;
                        break;
                    case "server":
                        startServer = Boolean.parseBoolean(value);
                        break;
                }
            }
        }

        // Start HTTP server if enabled
        if (startServer) {
            try {
                server = new AgentServer(host, port, instrumentation);
                server.start();
                System.out.println("[MemDiag] HTTP server started on " + host + ":" + port);
            } catch (Exception e) {
                System.err.println("[MemDiag] Failed to start HTTP server: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[MemDiag] Shutting down...");
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e) {
                    System.err.println("[MemDiag] Error stopping server: " + e.getMessage());
                }
            }
        }));
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public static boolean isInitialized() {
        return instrumentation != null;
    }
}
