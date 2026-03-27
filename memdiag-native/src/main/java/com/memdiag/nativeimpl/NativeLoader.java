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
        return load(false);
    }

    /**
     * Load the native library.
     * @param verbose If true, print debug information about loading attempts
     * @return true if the library was loaded successfully
     */
    public static boolean load(boolean verbose) {
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
                    if (verbose) {
                        System.out.println("[NativeLoader] Trying system library: " + libName);
                    }
                    System.loadLibrary(libName);
                    loaded = true;
                    loadedLibraryPath = "system:" + libName;
                    if (verbose) {
                        System.out.println("[NativeLoader] Loaded successfully from system: " + loadedLibraryPath);
                    }
                    return true;
                } catch (UnsatisfiedLinkError e) {
                    if (verbose) {
                        System.out.println("[NativeLoader] Failed to load " + libName + ": " + e.getMessage());
                    }
                    // Continue to next candidate
                }
            }

            // Try to load from classpath
            return loadFromClasspath(verbose);
        }
    }

    private static boolean loadFromClasspath() {
        return loadFromClasspath(false);
    }

    private static boolean loadFromClasspath(boolean verbose) {
        List<String> candidateNames = getLibraryNamesForSystem();

        if (verbose) {
            System.out.println("[NativeLoader] Candidates: " + candidateNames);
        }

        for (String libraryName : candidateNames) {
            if (verbose) {
                System.out.println("[NativeLoader] Trying classpath: /" + libraryName);
            }
            // Try to extract and load from classpath
            try (InputStream is = NativeLoader.class.getResourceAsStream("/" + libraryName)) {
                if (is == null) {
                    if (verbose) {
                        System.out.println("[NativeLoader] Resource not found: /" + libraryName);
                    }
                    continue;
                }

                File tempFile = File.createTempFile("memdiag-agent-", getSuffixForLibrary(libraryName));
                tempFile.deleteOnExit();
                if (verbose) {
                    System.out.println("[NativeLoader] Created temp file: " + tempFile.getAbsolutePath());
                }

                try (OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytes = 0;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                        totalBytes += bytesRead;
                    }
                    if (verbose) {
                        System.out.println("[NativeLoader] Wrote " + totalBytes + " bytes");
                    }
                }

                System.load(tempFile.getAbsolutePath());
                loaded = true;
                loadedLibraryPath = tempFile.getAbsolutePath();
                if (verbose) {
                    System.out.println("[NativeLoader] Loaded successfully: " + loadedLibraryPath);
                }
                return true;
            } catch (IOException e) {
                if (verbose) {
                    System.out.println("[NativeLoader] IO Error: " + e.getMessage());
                    e.printStackTrace();
                }
                // Continue to next candidate
            } catch (UnsatisfiedLinkError e) {
                if (verbose) {
                    System.out.println("[NativeLoader] Link Error: " + e.getMessage());
                    e.printStackTrace();
                }
                // Continue to next candidate
            }
        }

        if (verbose) {
            System.out.println("[NativeLoader] All candidates failed");
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
