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

        System.out.println("[MemDiag] Checking for JVMTI native library...");

        // Check if we're on Linux (JVMTI only supported on Linux currently)
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("linux")) {
            loadError = "JVMTI only supported on Linux (current: " + osName + ")";
            System.out.println("[MemDiag] " + loadError);
            System.out.println("[MemDiag] JVMTI features will not be available, but basic functionality still works");
            return false;
        }

        // Try to load using NativeLoader from memdiag-native
        try {
            // Use reflection to load NativeLoader to avoid hard dependency
            Class<?> nativeLoaderClass = Class.forName("com.memdiag.nativeimpl.NativeLoader");

            System.out.println("[MemDiag] Found NativeLoader, attempting to load JVMTI library...");

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
                System.out.println("[MemDiag] JVMTI features will not be available, but basic functionality still works");
                return false;
            }

        } catch (ClassNotFoundException e) {
            loadError = "memdiag-native not in classpath";
            System.out.println("[MemDiag] " + loadError);
            System.out.println("[MemDiag] To enable JVMTI features, include memdiag-native in the agent classpath");
            System.out.println("[MemDiag] Basic functionality (heap, threads, allocations) is still available");
            return false;
        } catch (Exception e) {
            loadError = "Error loading JVMTI library: " + e.getMessage();
            System.err.println("[MemDiag] " + loadError);
            System.out.println("[MemDiag] JVMTI features will not be available, but basic functionality still works");
            e.printStackTrace();
            return false;
        }
    }

    private void initializeJVMTI() {
        try {
            System.out.println("[MemDiag] JVMTI library loaded, initializing callbacks...");
            // Register JVMTI event callbacks via JNI
            JVMTIEventBridge.registerCallbacks();
            System.out.println("[MemDiag] JVMTI callbacks registered successfully");
        } catch (UnsatisfiedLinkError e) {
            loadError = "JVMTI JNI methods not available (JNI method mismatch): " + e.getMessage();
            available = false;
            System.out.println("[MemDiag] " + loadError);
            System.out.println("[MemDiag] JVMTI features will not be available, but bytecode instrumentation is still active");
            System.out.println("[MemDiag] Basic functionality (heap, threads, allocations via bytecode instrumentation) works normally");
        } catch (Throwable t) {
            loadError = "Error initializing JVMTI: " + t.getMessage();
            available = false;
            System.out.println("[MemDiag] " + loadError);
            System.out.println("[MemDiag] JVMTI features disabled, but basic functionality still works");
            t.printStackTrace();
        }
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
