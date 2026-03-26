package com.memdiag.core.util;

import com.memdiag.core.exception.AnalysisException;
import com.memdiag.core.exception.PlatformNotSupportedException;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class EnvironmentPrecheck {

    private EnvironmentPrecheck() {
        // Utility class
    }

    public static class PrecheckResult {
        private final boolean passed;
        private final List<String> warnings;
        private final List<String> errors;

        public PrecheckResult() {
            this.passed = true;
            this.warnings = new ArrayList<>();
            this.errors = new ArrayList<>();
        }

        public void addWarning(String message) {
            warnings.add(message);
        }

        public void addError(String message) {
            errors.add(message);
        }

        public boolean isPassed() {
            return errors.isEmpty();
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("Environment precheck FAILED:\n");
                for (String error : errors) {
                    sb.append("  - ERROR: ").append(error).append("\n");
                }
            }
            if (!warnings.isEmpty()) {
                sb.append("Environment precheck WARNINGS:\n");
                for (String warning : warnings) {
                    sb.append("  - WARN: ").append(warning).append("\n");
                }
            }
            if (errors.isEmpty() && warnings.isEmpty()) {
                sb.append("Environment precheck PASSED");
            }
            return sb.toString().trim();
        }
    }

    public static PrecheckResult precheckAttach(String pid) {
        PrecheckResult result = new PrecheckResult();

        // Check 1: PID format
        checkPidFormat(pid, result);

        // Check 2: PID exists
        checkPidExists(pid, result);

        // Check 3: Current JVM is JDK (not JRE)
        checkJdkAvailability(result);

        // Check 4: Operating system support
        checkOsSupport(result);

        // Check 5: Permission to attach
        checkAttachPermissions(pid, result);

        return result;
    }

    private static void checkPidFormat(String pid, PrecheckResult result) {
        if (pid == null || pid.trim().isEmpty()) {
            result.addError("PID cannot be empty");
            return;
        }
        try {
            long pidNum = Long.parseLong(pid);
            if (pidNum <= 0) {
                result.addError("PID must be a positive number, got: " + pid);
            }
        } catch (NumberFormatException e) {
            result.addError("Invalid PID format: '" + pid + "' is not a number");
        }
    }

    private static void checkPidExists(String pid, PrecheckResult result) {
        if (pid == null || pid.trim().isEmpty()) {
            return;
        }

        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("linux")) {
            File procDir = new File("/proc/" + pid);
            if (!procDir.exists() || !procDir.isDirectory()) {
                result.addError("Process with PID " + pid + " does not exist (no /proc/" + pid + " directory)");
            }
        } else if (osName.contains("mac")) {
            try {
                Process ps = new ProcessBuilder("ps", "-p", pid).start();
                int exitCode = ps.waitFor();
                if (exitCode != 0) {
                    result.addError("Process with PID " + pid + " does not exist (ps -p " + pid + " failed)");
                }
            } catch (Exception e) {
                result.addWarning("Could not verify PID existence: " + e.getMessage());
            }
        } else {
            result.addWarning("Cannot verify PID existence on " + osName);
        }
    }

    private static void checkJdkAvailability(PrecheckResult result) {
        String javaHome = System.getProperty("java.home");
        File javaHomeDir = new File(javaHome);

        // Check for tools.jar (for Java 8 and below)
        File toolsJar = new File(javaHomeDir, "../lib/tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(javaHomeDir, "lib/tools.jar");
        }

        // Check for jmods (for Java 9+)
        File jmodsDir = new File(javaHomeDir, "jmods");

        if (!toolsJar.exists() && !jmodsDir.exists()) {
            result.addWarning(
                "Could not find tools.jar or jmods directory. " +
                "Please ensure you are running with a JDK (not JRE). " +
                "Current JAVA_HOME: " + javaHome
            );
        }

        // Try to load Attach API
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException e) {
            result.addError(
                "Attach API not available. " +
                "Please run with a JDK and ensure tools.jar is in the classpath. " +
                "Current JAVA_HOME: " + javaHome
            );
        }
    }

    private static void checkOsSupport(PrecheckResult result) {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch");

        if (osName.contains("linux")) {
            // Linux is supported
        } else if (osName.contains("mac")) {
            // macOS is supported
        } else {
            result.addError("Unsupported operating system: " + osName);
        }

        // Check architecture
        if (!osArch.equals("amd64") && !osArch.equals("x86_64") && !osArch.equals("aarch64")) {
            result.addWarning("Unsupported architecture: " + osArch + ". Native agent may not work.");
        }
    }

    private static void checkAttachPermissions(String pid, PrecheckResult result) {
        String osName = System.getProperty("os.name").toLowerCase();
        String currentUser = System.getProperty("user.name");

        if (osName.contains("linux")) {
            // On Linux, check if we have access to /proc/<pid>/mem
            try {
                Path memPath = Paths.get("/proc/" + pid + "/mem");
                if (Files.exists(memPath)) {
                    if (!Files.isReadable(memPath)) {
                        result.addWarning(
                            "Cannot read /proc/" + pid + "/mem. " +
                            "You may not have sufficient permissions to attach to this process. " +
                            "Current user: " + currentUser
                        );
                    }
                }
            } catch (Exception e) {
                // Ignore, this is just a pre-check
            }

            // Check ptrace_scope on Linux
            try {
                Path ptraceScopePath = Paths.get("/proc/sys/kernel/yama/ptrace_scope");
                if (Files.exists(ptraceScopePath)) {
                    String value = Files.readString(ptraceScopePath).trim();
                    if ("1".equals(value) || "2".equals(value) || "3".equals(value)) {
                        result.addWarning(
                            "ptrace_scope is set to " + value + ". " +
                            "This may prevent attaching to processes. " +
                            "See https://www.kernel.org/doc/Documentation/security/Yama.txt for details."
                        );
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public static void precheckAttachOrThrow(String pid) {
        PrecheckResult result = precheckAttach(pid);
        if (!result.isPassed()) {
            throw new AnalysisException(result.getSummary());
        }
    }

    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public static String getJavaHome() {
        return System.getProperty("java.home");
    }

    public static boolean isJdk() {
        String javaHome = System.getProperty("java.home");
        File toolsJar = new File(javaHome, "../lib/tools.jar");
        if (!toolsJar.exists()) {
            toolsJar = new File(javaHome, "lib/tools.jar");
        }
        File jmodsDir = new File(javaHome, "jmods");
        return toolsJar.exists() || jmodsDir.exists();
    }
}
