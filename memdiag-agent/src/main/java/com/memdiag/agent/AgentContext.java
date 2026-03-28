package com.memdiag.agent;

import com.memdiag.agent.collect.DataCollector;
import com.memdiag.agent.collect.StatsAggregator;

import java.lang.instrument.Instrumentation;

/**
 * Holds the context for the MemDiag Agent, including all major components.
 */
public class AgentContext {

    private static volatile AgentContext instance;

    private final Instrumentation instrumentation;
    private final AgentConfig config;
    private volatile AgentState state;
    private volatile AgentServer server;

    // Data collection components (Phase 3)
    private volatile DataCollector dataCollector;
    private volatile StatsAggregator statsAggregator;

    // Components to be initialized in later phases
    private volatile Object instrumentManager; // Will be InstrumentManager
    private volatile Object jvmtiLoader; // Will be AgentJVMTILoader

    private long startTime;

    private AgentContext(Instrumentation instrumentation, AgentConfig config) {
        this.instrumentation = instrumentation;
        this.config = config;
        this.state = AgentState.UNINITIALIZED;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Initialize the agent context. Must be called once.
     */
    public static synchronized AgentContext initialize(Instrumentation instrumentation, AgentConfig config) {
        if (instance != null) {
            throw new IllegalStateException("AgentContext already initialized");
        }
        instance = new AgentContext(instrumentation, config);
        return instance;
    }

    /**
     * Get the singleton instance.
     */
    public static AgentContext getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AgentContext not initialized");
        }
        return instance;
    }

    /**
     * Check if the context is initialized.
     */
    public static boolean isInitialized() {
        return instance != null;
    }

    /**
     * Clear the instance (for testing only).
     */
    static void clearInstanceForTesting() {
        instance = null;
    }

    // State management

    public synchronized void setState(AgentState newState) {
        AgentState oldState = this.state;
        this.state = newState;
        System.out.printf("[MemDiag] State changed: %s -> %s%n", oldState, newState);
    }

    public AgentState getState() {
        return state;
    }

    public boolean isRunning() {
        return state == AgentState.RUNNING;
    }

    // Getters

    public Instrumentation getInstrumentation() {
        return instrumentation;
    }

    public AgentConfig getConfig() {
        return config;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getUptimeMs() {
        return System.currentTimeMillis() - startTime;
    }

    // Server management

    public void setServer(AgentServer server) {
        this.server = server;
    }

    public AgentServer getServer() {
        return server;
    }

    public boolean isServerRunning() {
        return server != null && server.isRunning();
    }

    // Data collection components (Phase 3)

    public void setDataCollector(DataCollector dataCollector) {
        this.dataCollector = dataCollector;
    }

    public DataCollector getDataCollector() {
        return dataCollector;
    }

    public void setStatsAggregator(StatsAggregator statsAggregator) {
        this.statsAggregator = statsAggregator;
    }

    public StatsAggregator getStatsAggregator() {
        return statsAggregator;
    }

    // Component access (for later phases)

    public void setInstrumentManager(Object instrumentManager) {
        this.instrumentManager = instrumentManager;
    }

    public Object getInstrumentManager() {
        return instrumentManager;
    }

    public void setJvmtiLoader(Object jvmtiLoader) {
        this.jvmtiLoader = jvmtiLoader;
    }

    public Object getJvmtiLoader() {
        return jvmtiLoader;
    }

    /**
     * Agent lifecycle states.
     */
    public enum AgentState {
        UNINITIALIZED,
        INITIALIZING,
        RUNNING,
        STOPPING,
        STOPPED
    }
}
