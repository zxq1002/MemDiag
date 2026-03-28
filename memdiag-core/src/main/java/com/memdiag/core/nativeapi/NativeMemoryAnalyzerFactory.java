package com.memdiag.core.nativeapi;

/**
 * Factory for creating NativeMemoryAnalyzer instances.
 * <p>
 * Two primary modes:
 * <ul>
 *   <li>ProcFS Mode - Read-only analysis via /proc filesystem (Linux only)</li>
 *   <li>Agent Mode - Full features via HTTP API to memdiag-agent.jar</li>
 * </ul>
 * <p>
 * JVMTI Mode is integrated within the Agent, not a separate mode here.
 */
public class NativeMemoryAnalyzerFactory {

    private NativeMemoryAnalyzerFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get a NativeMemoryAnalyzer for the current process.
     *
     * @return A NativeMemoryAnalyzer instance (never null)
     */
    public static NativeMemoryAnalyzer getInstance() {
        return getInstance(String.valueOf(ProcessHandle.current().pid()));
    }

    /**
     * Get a NativeMemoryAnalyzer for the specified process.
     * <p>
     * Returns the best available analyzer for the platform:
     * <ol>
     *   <li>ProcFS analyzer on Linux</li>
     *   <li>NoOp analyzer on other platforms</li>
     * </ol>
     *
     * @param pid Target process ID
     * @return A NativeMemoryAnalyzer instance (never null)
     */
    public static NativeMemoryAnalyzer getInstance(String pid) {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("linux")) {
            ProcFileSystemNativeAnalyzer procAnalyzer = new ProcFileSystemNativeAnalyzer(pid);
            if (procAnalyzer.isAvailable()) {
                return procAnalyzer;
            }
        }

        // Fallback to NoOp implementation
        return new NoOpNativeAnalyzer();
    }

    /**
     * Check if the current platform supports native memory analysis.
     *
     * @return true if native analysis is available
     */
    public static boolean isNativeAnalysisAvailable() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("linux")) {
            return false;
        }
        ProcFileSystemNativeAnalyzer analyzer = new ProcFileSystemNativeAnalyzer(
            String.valueOf(ProcessHandle.current().pid())
        );
        return analyzer.isAvailable();
    }
}
