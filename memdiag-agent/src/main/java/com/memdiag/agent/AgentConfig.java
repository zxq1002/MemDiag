package com.memdiag.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration for the MemDiag Agent.
 * Supports configuration via agent arguments, system properties, and defaults.
 */
public class AgentConfig {

    private static final String PREFIX = "memdiag.agent.";

    // HTTP server configuration
    private String httpHost = "localhost";
    private int httpPort = 6789;

    // Instrumentation configuration
    private boolean instrumentationEnabled = true;
    private int samplingRate = 100; // 1% sampling by default (1 out of 100)

    // JVMTI configuration
    private boolean jvmtiEnabled = true;
    private boolean jvmtiAutoLoad = true;

    // Ring buffer configuration
    private int ringBufferSize = 10000;

    // Internal state
    private final Map<String, String> rawConfig = new HashMap<>();

    private AgentConfig() {
    }

    /**
     * Create a default configuration.
     */
    public static AgentConfig defaults() {
        return new AgentConfig();
    }

    /**
     * Parse configuration from agent arguments string.
     * Format: "key1=value1,key2=value2"
     */
    public static AgentConfig fromAgentArgs(String agentArgs) {
        AgentConfig config = new AgentConfig();

        // First load from system properties
        config.loadFromSystemProperties();

        // Then override with agent arguments
        if (agentArgs != null && !agentArgs.isEmpty()) {
            String[] args = agentArgs.split(",");
            for (String arg : args) {
                arg = arg.trim();
                if (arg.isEmpty()) {
                    continue;
                }
                int eqIndex = arg.indexOf('=');
                if (eqIndex > 0) {
                    String key = arg.substring(0, eqIndex).trim();
                    String value = arg.substring(eqIndex + 1).trim();
                    config.rawConfig.put(key, value);
                    config.applyKeyValue(key, value);
                }
            }
        }

        return config;
    }

    private void loadFromSystemProperties() {
        Properties props = System.getProperties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(PREFIX)) {
                String configKey = key.substring(PREFIX.length());
                String value = props.getProperty(key);
                rawConfig.put(configKey, value);
                applyKeyValue(configKey, value);
            }
        }
    }

    private void applyKeyValue(String key, String value) {
        switch (key) {
            case "host":
            case "http.host":
                httpHost = value;
                break;
            case "port":
            case "http.port":
                try {
                    httpPort = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    System.err.println("[MemDiag] Invalid port: " + value + ", using default: " + httpPort);
                }
                break;
            case "instrumentation.enabled":
                instrumentationEnabled = Boolean.parseBoolean(value);
                break;
            case "samplingRate":
            case "instrumentation.samplingRate":
                try {
                    int rate = Integer.parseInt(value);
                    if (rate >= 1) {
                        samplingRate = rate;
                    } else {
                        System.err.println("[MemDiag] Invalid sampling rate: " + value + ", must be >= 1");
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[MemDiag] Invalid sampling rate: " + value + ", using default: " + samplingRate);
                }
                break;
            case "jvmti.enabled":
                jvmtiEnabled = Boolean.parseBoolean(value);
                break;
            case "jvmti.autoLoad":
                jvmtiAutoLoad = Boolean.parseBoolean(value);
                break;
            case "ringBuffer.size":
                try {
                    int size = Integer.parseInt(value);
                    if (size >= 100) {
                        ringBufferSize = size;
                    } else {
                        System.err.println("[MemDiag] Invalid ring buffer size: " + value + ", must be >= 100");
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[MemDiag] Invalid ring buffer size: " + value + ", using default: " + ringBufferSize);
                }
                break;
            default:
                System.err.println("[MemDiag] Unknown configuration key: " + key);
        }
    }

    // Getters

    public String getHttpHost() {
        return httpHost;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public boolean isInstrumentationEnabled() {
        return instrumentationEnabled;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public boolean isJvmtiEnabled() {
        return jvmtiEnabled;
    }

    public boolean isJvmtiAutoLoad() {
        return jvmtiAutoLoad;
    }

    public int getRingBufferSize() {
        return ringBufferSize;
    }

    public Map<String, String> getRawConfig() {
        return new HashMap<>(rawConfig);
    }

    /**
     * Get the sampling probability as a decimal (0.0 to 1.0).
     */
    public double getSamplingProbability() {
        return 1.0 / samplingRate;
    }

    /**
     * Check if we should sample this event based on the configured sampling rate.
     */
    public boolean shouldSample(long counter) {
        return counter % samplingRate == 0;
    }

    /**
     * Convert to a map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("http.host", httpHost);
        map.put("http.port", httpPort);
        map.put("instrumentation.enabled", instrumentationEnabled);
        map.put("instrumentation.samplingRate", samplingRate);
        map.put("jvmti.enabled", jvmtiEnabled);
        map.put("jvmti.autoLoad", jvmtiAutoLoad);
        map.put("ringBuffer.size", ringBufferSize);
        return map;
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "httpHost='" + httpHost + '\'' +
                ", httpPort=" + httpPort +
                ", instrumentationEnabled=" + instrumentationEnabled +
                ", samplingRate=" + samplingRate +
                ", jvmtiEnabled=" + jvmtiEnabled +
                ", jvmtiAutoLoad=" + jvmtiAutoLoad +
                ", ringBufferSize=" + ringBufferSize +
                '}';
    }
}
