package com.memdiag.nativeimpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class NativeLoader {

    private static volatile boolean loaded = false;
    private static final Object lock = new Object();
    private static String loadedLibraryPath = null;

    private NativeLoader() {
        // Private constructor to prevent instantiation
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static String getLoadedLibraryPath() {
        return loadedLibraryPath;
    }

    public static boolean load() {
        if (loaded) {
            return true;
        }

        synchronized (lock) {
            if (loaded) {
                return true;
            }

            // Try to load from system library path first
            for (String libName : getLibraryNamesForSystem()) {
                try {
                    System.loadLibrary(libName);
                    loaded = true;
                    loadedLibraryPath = "system:" + libName;
                    return true;
                } catch (UnsatisfiedLinkError e) {
                    // Continue to next candidate
                }
            }

            // Try to load from classpath
            return loadFromClasspath();
        }
    }

    private static boolean loadFromClasspath() {
        List<String> candidateNames = getLibraryNamesForSystem();

        for (String libraryName : candidateNames) {
            // Try to extract and load from classpath
            try (InputStream is = NativeLoader.class.getResourceAsStream("/" + libraryName)) {
                if (is == null) {
                    continue;
                }

                File tempFile = File.createTempFile("memdiag-agent-", getSuffixForLibrary(libraryName));
                tempFile.deleteOnExit();

                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }

                System.load(tempFile.getAbsolutePath());
                loaded = true;
                loadedLibraryPath = tempFile.getAbsolutePath();
                return true;
            } catch (IOException e) {
                // Continue to next candidate
            }
        }

        return false;
    }

    private static List<String> getLibraryNamesForSystem() {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        List<String> candidates = new ArrayList<>();

        String baseName;
        String suffix;

        if (osName.contains("linux")) {
            baseName = "libmemdiag-agent";
            suffix = ".so";
        } else if (osName.contains("mac")) {
            baseName = "libmemdiag-agent";
            suffix = ".dylib";
        } else if (osName.contains("windows")) {
            baseName = "memdiag-agent";
            suffix = ".dll";
        } else {
            return candidates;
        }

        // Architecture-specific names (highest priority)
        if (osArch.equals("aarch64") || osArch.equals("arm64")) {
            candidates.add(baseName + "-aarch64" + suffix);
            candidates.add(baseName + "-arm64" + suffix);
        } else if (osArch.equals("amd64") || osArch.equals("x86_64")) {
            candidates.add(baseName + "-amd64" + suffix);
            candidates.add(baseName + "-x86_64" + suffix);
        }

        // Generic name (fallback)
        candidates.add(baseName + suffix);

        return candidates;
    }

    private static String getSuffixForLibrary(String libraryName) {
        if (libraryName.endsWith(".so")) return ".so";
        if (libraryName.endsWith(".dylib")) return ".dylib";
        if (libraryName.endsWith(".dll")) return ".dll";
        return ".tmp";
    }
}
