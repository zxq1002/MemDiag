package com.memdiag.agent;

import com.memdiag.agent.collect.DataCollector;
import com.memdiag.agent.collect.StatsAggregator;
import com.memdiag.agent.instrument.InstrumentManager;
import com.memdiag.agent.instrument.MemDiagSpy;
import com.memdiag.agent.jvmti.AgentJVMTILoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * MemDiag Java Agent - Enhanced version with full lifecycle management.
 */
public class MemDiagAgent {

    private static volatile Instrumentation instrumentation;
    private static volatile AgentServer server;

    public static void premain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst, false);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst, true);
    }

    private static synchronized void initialize(String agentArgs, Instrumentation inst, boolean isAttach) {
        if (AgentContext.isInitialized()) {
            System.out.println("[MemDiag] Agent already initialized");
            return;
        }

        System.out.println("");
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MemDiag Agent Starting                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        if (isAttach) {
            System.out.println("[MemDiag] Attaching to running JVM...");
        } else {
            System.out.println("[MemDiag] Starting with premain...");
        }

        instrumentation = inst;
        AgentConfig config = AgentConfig.fromAgentArgs(agentArgs);
        AgentContext context = AgentContext.initialize(inst, config);
        context.setState(AgentContext.AgentState.INITIALIZING);

        // Initialize components BEFORE starting server
        initializeOptionalComponents(config, context);

        if (startServer(config, context, inst)) {
            context.setState(AgentContext.AgentState.RUNNING);
            System.out.printf("[MemDiag] HTTP server started on %s:%d%n",
                    config.getHttpHost(), config.getHttpPort());
        } else {
            context.setState(AgentContext.AgentState.STOPPED);
            System.err.println("[MemDiag] Failed to start HTTP server");
            return;
        }

        addShutdownHook(context);

        // Print final status summary
        printStartupSummary(context, config);
    }

    private static void printStartupSummary(AgentContext context, AgentConfig config) {
        System.out.println("");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                      MemDiag Agent Ready                         │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");

        System.out.printf("│ HTTP Server:  %-50s │%n",
                config.getHttpHost() + ":" + config.getHttpPort());

        String instrStatus = config.isInstrumentationEnabled() ? "ENABLED" : "disabled";
        System.out.printf("│ Instrumentation: %-46s │%n", instrStatus);

        String jvmtiStatus = "UNKNOWN";
        if (context.getJvmtiLoader() != null) {
            if (context.getJvmtiLoader().isAvailable()) {
                jvmtiStatus = "ENABLED (native)";
            } else if (context.getJvmtiLoader().getLoadError() != null) {
                jvmtiStatus = "DISABLED (" + context.getJvmtiLoader().getLoadError() + ")";
            } else {
                jvmtiStatus = "not loaded";
            }
        } else if (!config.isJvmtiEnabled()) {
            jvmtiStatus = "disabled by config";
        }
        System.out.printf("│ JVMTI:         %-46s │%n", jvmtiStatus);

        System.out.println("└─────────────────────────────────────────────────────────────────┘");
        System.out.println("");
        System.out.println("[MemDiag] Available commands:");
        System.out.println("  memdiag --agent=" + config.getHttpHost() + ":" + config.getHttpPort() + " histogram");
        System.out.println("  memdiag --agent=" + config.getHttpHost() + ":" + config.getHttpPort() + " threads");
        System.out.println("  memdiag --agent=" + config.getHttpHost() + ":" + config.getHttpPort() + " diagnose");
        System.out.println("  memdiag --agent=" + config.getHttpHost() + ":" + config.getHttpPort() + " allocations");
        System.out.println("");
    }

    private static boolean startServer(AgentConfig config, AgentContext context, Instrumentation inst) {
        try {
            AgentServer agentServer = new AgentServer(config.getHttpHost(), config.getHttpPort(), inst);
            agentServer.start();
            context.setServer(agentServer);
            server = agentServer;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void addShutdownHook(AgentContext context) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownAgent(context);
        }, "MemDiag-Shutdown-Hook"));
    }

    public static synchronized void shutdownAgent(AgentContext context) {
        if (context == null && AgentContext.isInitialized()) {
            context = AgentContext.getInstance();
        }
        if (context == null || context.getState() == AgentContext.AgentState.STOPPED) {
            return;
        }

        context.setState(AgentContext.AgentState.STOPPING);
        AgentServer agentServer = context.getServer();
        if (agentServer != null) {
            try {
                agentServer.stop();
            } catch (Exception e) {}
        }
        context.shutdownScheduler();
        context.setState(AgentContext.AgentState.STOPPED);
    }

    private static void initializeOptionalComponents(AgentConfig config, AgentContext context) {
        DataCollector dataCollector = new DataCollector(config.getRingBufferSize());
        StatsAggregator statsAggregator = new StatsAggregator(dataCollector);
        context.setDataCollector(dataCollector);
        context.setStatsAggregator(statsAggregator);

        // Start periodic snapshot task
        context.startScheduler().scheduleAtFixedRate(
            statsAggregator::takeSnapshot,
            1, 1, java.util.concurrent.TimeUnit.SECONDS
        );

        // Inject MemDiagSpy via a dedicated small JAR to avoid LinkageError
        try {
            File spyJar = createSpyJar();
            System.out.println("[MemDiag] Created Spy JAR: " + spyJar.getAbsolutePath());
            context.getInstrumentation().appendToBootstrapClassLoaderSearch(new JarFile(spyJar));
        } catch (Exception e) {
            System.err.println("[MemDiag] Failed to inject MemDiagSpy: " + e.getMessage());
            e.printStackTrace();
        }

        if (config.isInstrumentationEnabled()) {
            InstrumentManager instrumentManager = new InstrumentManager(context.getInstrumentation(), config, dataCollector);
            context.setInstrumentManager(instrumentManager);
            instrumentManager.initialize();
            
            // Initialize the spy with transformers
            MemDiagSpy.init(
                instrumentManager.getAllocationTransformer(),
                instrumentManager.getMethodMonitorTransformer()
            );

            // Enable tracking
            instrumentManager.enableAllocationTracking();
            if (config.isMethodMonitoringEnabled()) {
                instrumentManager.enableMethodMonitoring();
            }
        }

        if (config.isJvmtiEnabled() && config.isJvmtiAutoLoad()) {
            AgentJVMTILoader jvmtiLoader = new AgentJVMTILoader(config);
            context.setJvmtiLoader(jvmtiLoader);
            jvmtiLoader.load();
        }
    }

    private static File createSpyJar() throws Exception {
        File tempJar = Files.createTempFile("memdiag-spy", ".jar").toFile();
        tempJar.deleteOnExit();

        String spyClassName = "com/memdiag/agent/instrument/MemDiagSpy.class";
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(tempJar));
             InputStream is = MemDiagAgent.class.getClassLoader().getResourceAsStream(spyClassName)) {
            
            if (is == null) {
                throw new RuntimeException("Could not find " + spyClassName + " in classpath");
            }
            
            jos.putNextEntry(new JarEntry(spyClassName));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                jos.write(buffer, 0, len);
            }
            jos.closeEntry();
        }
        return tempJar;
    }
}
