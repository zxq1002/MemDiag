package com.memdiag.core.nativeapi;

import java.util.ServiceLoader;

public class NativeMemoryAnalyzerFactory {

    private static volatile NativeMemoryAnalyzer instance;
    private static volatile NativeMemoryAnalyzer jvmtInstance;

    private NativeMemoryAnalyzerFactory() {
        // Private constructor to prevent instantiation
    }

    public static NativeMemoryAnalyzer getInstance() {
        if (instance == null) {
            synchronized (NativeMemoryAnalyzerFactory.class) {
                if (instance == null) {
                    instance = createAnalyzer(false);
                }
            }
        }
        return instance;
    }

    public static NativeMemoryAnalyzer getInstance(String pid) {
        if (pid == null || pid.isEmpty()) {
            return getInstance();
        }
        return createAnalyzer(pid, false);
    }

    /**
     * Get a NativeMemoryAnalyzer that supports JVMTI features (attach, trace, etc.).
     * If JVMTI is not available, falls back to the basic analyzer.
     */
    public static NativeMemoryAnalyzer getInstanceWithJVMTI() {
        return getInstanceWithJVMTI(String.valueOf(ProcessHandle.current().pid()));
    }

    /**
     * Get a NativeMemoryAnalyzer that supports JVMTI features (attach, trace, etc.).
     * If JVMTI is not available, falls back to the basic analyzer.
     */
    public static NativeMemoryAnalyzer getInstanceWithJVMTI(String pid) {
        if (jvmtInstance == null) {
            synchronized (NativeMemoryAnalyzerFactory.class) {
                if (jvmtInstance == null) {
                    jvmtInstance = createAnalyzer(pid, true);
                }
            }
        }
        return jvmtInstance;
    }

    private static NativeMemoryAnalyzer createAnalyzer(boolean requireJVMTI) {
        return createAnalyzer(String.valueOf(ProcessHandle.current().pid()), requireJVMTI);
    }

    private static NativeMemoryAnalyzer createAnalyzer(String pid, boolean requireJVMTI) {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("linux")) {
            // First try JVMTI analyzer if requested
            if (requireJVMTI) {
                NativeMemoryAnalyzer jvmtAnalyzer = tryCreateJVMTIAnalyzer(pid);
                if (jvmtAnalyzer != null && jvmtAnalyzer.isAvailable()) {
                    return jvmtAnalyzer;
                }
                // If JVMTI requested but not available, still fall through to ProcFS
            }

            // Try ProcFileSystem analyzer (always available on Linux)
            ProcFileSystemNativeAnalyzer procAnalyzer = new ProcFileSystemNativeAnalyzer(pid);
            if (procAnalyzer.isAvailable()) {
                return procAnalyzer;
            }
        }

        // Fallback to NoOp implementation
        return new NoOpNativeAnalyzer();
    }

    private static NativeMemoryAnalyzer tryCreateJVMTIAnalyzer(String pid) {
        // First try to load the native library
        try {
            Class<?> nativeLoaderClass = Class.forName("com.memdiag.nativeimpl.NativeLoader");
            java.lang.reflect.Method loadMethod = nativeLoaderClass.getMethod("load");
            java.lang.reflect.Method isLoadedMethod = nativeLoaderClass.getMethod("isLoaded");

            // Try to load the library
            loadMethod.invoke(null);

            // Check if library was actually loaded
            boolean isLoaded = (Boolean) isLoadedMethod.invoke(null);
            if (!isLoaded) {
                return null;
            }
        } catch (Exception e) {
            // NativeLoader not available or library load failed
            return null;
        }

        try {
            // Use ServiceLoader to find JVMTINativeAnalyzer implementation
            ServiceLoader<NativeMemoryAnalyzer> loader = ServiceLoader.load(NativeMemoryAnalyzer.class);
            for (NativeMemoryAnalyzer analyzer : loader) {
                if (analyzer.getClass().getName().contains("JVMTINativeAnalyzer")) {
                    // Create instance with pid via reflection if needed
                    try {
                        return analyzer.getClass().getConstructor(String.class).newInstance(pid);
                    } catch (Exception e) {
                        // Try no-arg constructor
                        return analyzer;
                    }
                }
            }
        } catch (Exception e) {
            // JVMTI not available, return null
        }

        // Fallback: try direct class loading
        try {
            Class<?> clazz = Class.forName("com.memdiag.nativeimpl.JVMTINativeAnalyzer");
            try {
                return (NativeMemoryAnalyzer) clazz.getConstructor(String.class).newInstance(pid);
            } catch (Exception e) {
                return (NativeMemoryAnalyzer) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            // JVMTI not available
            return null;
        }
    }

    public static void setInstance(NativeMemoryAnalyzer analyzer) {
        instance = analyzer;
    }

    public static void reset() {
        instance = null;
        jvmtInstance = null;
    }
}
