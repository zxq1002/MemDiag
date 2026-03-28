package com.memdiag.core.agent;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.File;
import java.io.IOException;

/**
 * Utility for dynamically attaching the MemDiag agent to a running JVM.
 */
public class AgentAttacher {

    private static final String DEFAULT_AGENT_PORT = "6789";
    private static final String AGENT_JAR_NAME = "memdiag-agent.jar";

    private AgentAttacher() {
        // Private constructor to prevent instantiation
    }

    /**
     * Attach the MemDiag agent to a target JVM.
     *
     * @param pid The PID of the target JVM
     * @param agentJarPath Path to the memdiag-agent.jar file
     * @param port The port for the agent's HTTP server
     * @return true if the agent was attached successfully
     */
    public static boolean attach(String pid, String agentJarPath, int port) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                String agentArgs = "port=" + port;
                vm.loadAgent(agentJarPath, agentArgs);
                return true;
            } finally {
                vm.detach();
            }
        } catch (AttachNotSupportedException e) {
            System.err.println("[AgentAttacher] Attach not supported: " + e.getMessage());
            return false;
        } catch (AgentLoadException e) {
            System.err.println("[AgentAttacher] Failed to load agent: " + e.getMessage());
            return false;
        } catch (AgentInitializationException e) {
            System.err.println("[AgentAttacher] Agent initialization failed: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("[AgentAttacher] IO error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Attach the MemDiag agent to a target JVM with default port.
     *
     * @param pid The PID of the target JVM
     * @param agentJarPath Path to the memdiag-agent.jar file
     * @return true if the agent was attached successfully
     */
    public static boolean attach(String pid, String agentJarPath) {
        return attach(pid, agentJarPath, Integer.parseInt(DEFAULT_AGENT_PORT));
    }

    /**
     * Try to find the agent jar in common locations.
     *
     * @return The path to the agent jar, or null if not found
     */
    public static String findAgentJar() {
        // Try current directory
        File jarFile = new File(AGENT_JAR_NAME);
        if (jarFile.exists()) {
            return jarFile.getAbsolutePath();
        }

        // Try working directory from system property
        String userDir = System.getProperty("user.dir");
        jarFile = new File(userDir, AGENT_JAR_NAME);
        if (jarFile.exists()) {
            return jarFile.getAbsolutePath();
        }

        // Try target directory (Maven build)
        jarFile = new File("memdiag-agent/target/" + AGENT_JAR_NAME);
        if (jarFile.exists()) {
            return jarFile.getAbsolutePath();
        }

        // Try parent directory's target
        jarFile = new File("../memdiag-agent/target/" + AGENT_JAR_NAME);
        if (jarFile.exists()) {
            return jarFile.getAbsolutePath();
        }

        return null;
    }

    /**
     * Check if the attach API is available.
     *
     * @return true if the attach API is available
     */
    public static boolean isAttachApiAvailable() {
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
