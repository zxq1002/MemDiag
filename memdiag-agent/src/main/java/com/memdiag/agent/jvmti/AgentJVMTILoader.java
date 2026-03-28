package com.memdiag.agent.jvmti;

import com.memdiag.agent.AgentConfig;

/**
 * Loader for JVMTI native library.
 * Automatically detects and loads the matching libmemdiag-agent.so.
 */
public class AgentJVMTILoader {

    private final AgentConfig config;
    private volatile boolean loaded = false;
    private volatile boolean available = false;
    private volatile String loadError = null;

    public AgentJVMTILoader(AgentConfig config) {
        this.config = config;
    }

    /**
     * Try to load the JVMTI native library.
     */
    public synchronized boolean load() {
        if (loaded) {
            return available;
        }

        loaded = true;

        if (!config.isJvmtiEnabled()) {
            System.out.println("[MemDiag] JVMTI disabled by configuration");
            return false;
        }

        if (!config.isJvmtiAutoLoad()) {
            System.out.println("[MemDiag] JVMTI auto-load disabled by configuration");
            return false;
        }

        System.out.println("[MemDiag] Attempting to load JVMTI native library...");

        // Check if we're on Linux (JVMTI only supported on Linux currently)
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("linux")) {
            loadError = "JVMTI only supported on Linux (current: " + osName + ")";
            System.out.println("[MemDiag] " + loadError);
            return false;
        }

        // Try to load using NativeLoader from memdiag-native
        try {
            // Use reflection to load NativeLoader to avoid hard dependency
            Class<?> nativeLoaderClass = Class.forName("com.memdiag.nativeimpl.NativeLoader");

            // Check if the library is already loaded
            java.lang.reflect.Method isLoadedMethod = nativeLoaderClass.getMethod("isLoaded");
            Boolean alreadyLoaded = (Boolean) isLoadedMethod.invoke(null);

            if (alreadyLoaded) {
                System.out.println("[MemDiag] JVMTI native library already loaded");
                available = true;
                initializeJVMTI();
                return true;
            }

            // Try to load with verbose output
            java.lang.reflect.Method loadMethod = nativeLoaderClass.getMethod("load", boolean.class);
            Boolean success = (Boolean) loadMethod.invoke(null, true);

            if (success) {
                System.out.println("[MemDiag] JVMTI native library loaded successfully");
                available = true;
                initializeJVMTI();
                return true;
            } else {
                loadError = "Failed to load JVMTI native library";
                System.out.println("[MemDiag] " + loadError);
                return false;
            }

        } catch (ClassNotFoundException e) {
            loadError = "NativeLoader not found (memdiag-native not in classpath)";
            System.out.println("[MemDiag] " + loadError);
            return false;
        } catch (Exception e) {
            loadError = "Error loading JVMTI library: " + e.getMessage();
            System.err.println("[MemDiag] " + loadError);
            e.printStackTrace();
            return false;
        }
    }

    private void initializeJVMTI() {
        System.out.println("[MemDiag] JVMTI initialized");
        // TODO: Register JVMTI event callbacks via JNI
    }

    /**
     * Check if JVMTI library is loaded and available.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Check if load has been attempted.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Get the error message if load failed.
     */
    public String getLoadError() {
        return loadError;
    }

    /**
     * Get the status as a map for API responses.
     */
    public java.util.Map<String, Object> getStatus() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("enabled", config.isJvmtiEnabled());
        status.put("autoLoad", config.isJvmtiAutoLoad());
        status.put("loaded", loaded);
        status.put("available", available);
        if (loadError != null) {
            status.put("error", loadError);
        }
        status.put("platform", System.getProperty("os.name"));
        status.put("arch", System.getProperty("os.arch"));
        return status;
    }
}
