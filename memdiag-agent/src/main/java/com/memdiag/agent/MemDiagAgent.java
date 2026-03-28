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
            return;
        }

        instrumentation = inst;
        AgentConfig config = AgentConfig.fromAgentArgs(agentArgs);
        AgentContext context = AgentContext.initialize(inst, config);
        context.setState(AgentContext.AgentState.INITIALIZING);

        if (startServer(config, context, inst)) {
            context.setState(AgentContext.AgentState.RUNNING);
        } else {
            context.setState(AgentContext.AgentState.STOPPED);
            return;
        }

        addShutdownHook(context);
        initializeOptionalComponents(config, context);
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
        context.setState(AgentContext.AgentState.STOPPED);
    }

    private static void initializeOptionalComponents(AgentConfig config, AgentContext context) {
        DataCollector dataCollector = new DataCollector(config.getRingBufferSize());
        StatsAggregator statsAggregator = new StatsAggregator(dataCollector);
        context.setDataCollector(dataCollector);
        context.setStatsAggregator(statsAggregator);

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
