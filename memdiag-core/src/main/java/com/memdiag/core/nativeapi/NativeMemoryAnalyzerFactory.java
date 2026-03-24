package com.memdiag.core.nativeapi;

public class NativeMemoryAnalyzerFactory {

    private static volatile NativeMemoryAnalyzer instance;

    private NativeMemoryAnalyzerFactory() {
        // Private constructor to prevent instantiation
    }

    public static NativeMemoryAnalyzer getInstance() {
        if (instance == null) {
            synchronized (NativeMemoryAnalyzerFactory.class) {
                if (instance == null) {
                    instance = createAnalyzer();
                }
            }
        }
        return instance;
    }

    public static NativeMemoryAnalyzer getInstance(String pid) {
        if (pid == null || pid.isEmpty()) {
            return getInstance();
        }
        return createAnalyzer(pid);
    }

    private static NativeMemoryAnalyzer createAnalyzer() {
        return createAnalyzer(String.valueOf(ProcessHandle.current().pid()));
    }

    private static NativeMemoryAnalyzer createAnalyzer(String pid) {
        // Try to create platform-specific analyzer
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("linux")) {
            ProcFileSystemNativeAnalyzer analyzer = new ProcFileSystemNativeAnalyzer(pid);
            if (analyzer.isAvailable()) {
                return analyzer;
            }
        }

        // Fallback to NoOp implementation
        return new NoOpNativeAnalyzer();
    }

    public static void setInstance(NativeMemoryAnalyzer analyzer) {
        instance = analyzer;
    }

    public static void reset() {
        instance = null;
    }
}
