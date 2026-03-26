package com.memdiag.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MemDiagConfig {
    private static final String CONFIG_FILE = "memdiag.properties";
    private static final MemDiagConfig INSTANCE = new MemDiagConfig();

    private final Properties properties;

    private MemDiagConfig() {
        this.properties = loadProperties();
    }

    public static MemDiagConfig getInstance() {
        return INSTANCE;
    }

    private Properties loadProperties() {
        Properties props = new Properties();

        // Set default values
        props.setProperty("memdiag.native.sampling-rate", "100000");
        props.setProperty("memdiag.jmx.heap-histogram-timeout-ms", "500");
        props.setProperty("memdiag.jmx.max-safe-point-time-ms", "500");
        props.setProperty("memdiag.analysis.memory-limit-bytes", "0");

        // Try to load from classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            // Ignore, use defaults
        }

        // Override with system properties
        props.putAll(System.getProperties());

        return props;
    }

    public int getNativeSamplingRate() {
        return getIntProperty("memdiag.native.sampling-rate", 100000);
    }

    public long getJmxHeapHistogramTimeoutMs() {
        return getLongProperty("memdiag.jmx.heap-histogram-timeout-ms", 500);
    }

    public long getJmxMaxSafePointTimeMs() {
        return getLongProperty("memdiag.jmx.max-safe-point-time-ms", 500);
    }

    public long getAnalysisMemoryLimitBytes() {
        long limit = getLongProperty("memdiag.analysis.memory-limit-bytes", 0);
        if (limit == 0) {
            // Default to 80% of max heap
            return Runtime.getRuntime().maxMemory() * 4 / 5;
        }
        return limit;
    }

    private int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        return defaultValue;
    }

    private long getLongProperty(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                // Use default
            }
        }
        return defaultValue;
    }
}
