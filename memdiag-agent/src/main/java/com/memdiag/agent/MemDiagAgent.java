package com.memdiag.agent;

import com.memdiag.agent.collect.DataCollector;
import com.memdiag.agent.collect.StatsAggregator;
import com.memdiag.agent.instrument.InstrumentManager;
import com.memdiag.agent.jvmti.AgentJVMTILoader;

import java.lang.instrument.Instrumentation;

/**
 * MemDiag Java Agent - Enhanced version with full lifecycle management.
 *
 * Supports both startup loading (-javaagent) and dynamic attachment.
 */
public class MemDiagAgent {

    // Keep legacy fields for backward compatibility
    @Deprecated
    private static volatile Instrumentation instrumentation;
    @Deprecated
    private static volatile AgentServer server;

    /**
     * Called when agent is loaded at JVM startup with -javaagent.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst, false);
    }

    /**
     * Called when agent is dynamically attached to a running JVM.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst, true);
    }

    /**
     * Initialize the agent with full lifecycle management.
     */
    private static synchronized void initialize(String agentArgs, Instrumentation inst, boolean isAttach) {
        // Check for duplicate initialization
        if (AgentContext.isInitialized()) {
            System.err.println("[MemDiag] Agent already initialized");
            AgentContext ctx = AgentContext.getInstance();
            System.err.printf("[MemDiag] Current state: %s, uptime: %d ms%n",
                    ctx.getState(), ctx.getUptimeMs());
            return;
        }

        // Set legacy fields for backward compatibility
        instrumentation = inst;

        System.out.println("[MemDiag] Initializing agent...");
        System.out.printf("[MemDiag] Loading mode: %s%n", isAttach ? "dynamic attach" : "startup");

        // Parse configuration
        AgentConfig config = AgentConfig.fromAgentArgs(agentArgs);
        System.out.printf("[MemDiag] Configuration: %s%n", config);

        // Initialize context
        AgentContext context = AgentContext.initialize(inst, config);
        context.setState(AgentContext.AgentState.INITIALIZING);

        // Start HTTP server
        if (startServer(config, context, inst)) {
            context.setState(AgentContext.AgentState.RUNNING);
            System.out.println("[MemDiag] Agent initialization complete");
        } else {
            context.setState(AgentContext.AgentState.STOPPED);
            System.err.println("[MemDiag] Agent initialization failed");
            return;
        }

        // Add shutdown hook for graceful cleanup
        addShutdownHook(context);

        // Initialize optional components (placeholder for future phases)
        initializeOptionalComponents(config, context);
    }

    /**
     * Start the HTTP server.
     */
    private static boolean startServer(AgentConfig config, AgentContext context, Instrumentation inst) {
        try {
            System.out.printf("[MemDiag] Starting HTTP server on %s:%d%n",
                    config.getHttpHost(), config.getHttpPort());

            AgentServer agentServer = new AgentServer(
                    config.getHttpHost(),
                    config.getHttpPort(),
                    inst);
            agentServer.start();

            // Update both context and legacy field
            context.setServer(agentServer);
            server = agentServer;

            System.out.printf("[MemDiag] HTTP server started on %s:%d%n",
                    config.getHttpHost(), config.getHttpPort());
            return true;
        } catch (Exception e) {
            System.err.println("[MemDiag] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Add a shutdown hook for graceful cleanup.
     */
    private static void addShutdownHook(AgentContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[MemDiag] JVM shutting down, stopping agent...");
            shutdownAgent(context);
        }, "MemDiag-Shutdown-Hook"));
    }

    /**
     * Shutdown the agent gracefully.
     */
    public static synchronized void shutdownAgent(AgentContext context) {
        if (context == null) {
            context = AgentContext.isInitialized() ? AgentContext.getInstance() : null;
        }
        if (context == null) {
            return;
        }

        if (context.getState() == AgentContext.AgentState.STOPPED) {
            return;
        }

        context.setState(AgentContext.AgentState.STOPPING);

        // Stop HTTP server
        AgentServer agentServer = context.getServer();
        if (agentServer != null) {
            try {
                System.out.println("[MemDiag] Stopping HTTP server...");
                agentServer.stop();
                System.out.println("[MemDiag] HTTP server stopped");
            } catch (Exception e) {
                System.err.println("[MemDiag] Error stopping server: " + e.getMessage());
            }
        }

        context.setState(AgentContext.AgentState.STOPPED);
        System.out.println("[MemDiag] Agent shutdown complete");
    }

    /**
     * Initialize optional components.
     */
    private static void initializeOptionalComponents(AgentConfig config, AgentContext context) {
        // Phase 3: Initialize data collector and stats aggregator
        System.out.println("[MemDiag] Initializing data collection...");
        DataCollector dataCollector = new DataCollector(config.getRingBufferSize());
        StatsAggregator statsAggregator = new StatsAggregator(dataCollector);
        context.setDataCollector(dataCollector);
        context.setStatsAggregator(statsAggregator);
        System.out.println("[MemDiag] Data collection initialized");

        // Phase 2: Initialize instrumentation manager
        if (config.isInstrumentationEnabled()) {
            System.out.println("[MemDiag] Initializing instrumentation manager...");
            InstrumentManager instrumentManager = new InstrumentManager(
                context.getInstrumentation(),
                config,
                dataCollector
            );
            context.setInstrumentManager(instrumentManager);
            instrumentManager.initialize();
            System.out.println("[MemDiag] Instrumentation manager initialized");
        }

        // Phase 4: Initialize JVMTI loader
        if (config.isJvmtiEnabled() && config.isJvmtiAutoLoad()) {
            System.out.println("[MemDiag] Initializing JVMTI...");
            AgentJVMTILoader jvmtiLoader = new AgentJVMTILoader(config);
            context.setJvmtiLoader(jvmtiLoader);
            jvmtiLoader.load();
        }
    }

    // ========== Legacy methods for backward compatibility ==========

    /**
     * @deprecated Use AgentContext.getInstance().getInstrumentation() instead
     */
    @Deprecated
    public static Instrumentation getInstrumentation() {
        if (AgentContext.isInitialized()) {
            return AgentContext.getInstance().getInstrumentation();
        }
        return instrumentation;
    }

    /**
     * @deprecated Use AgentContext.isInitialized() instead
     */
    @Deprecated
    public static boolean isInitialized() {
        return AgentContext.isInitialized();
    }

    /**
     * @deprecated Use AgentContext.getInstance().getServer() instead
     */
    @Deprecated
    public static AgentServer getServer() {
        if (AgentContext.isInitialized()) {
            return AgentContext.getInstance().getServer();
        }
        return server;
    }
}
