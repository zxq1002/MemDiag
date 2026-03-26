package com.memdiag.nativeimpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NativeLoader {

    private static volatile boolean loaded = false;
    private static final Object lock = new Object();

    private NativeLoader() {
        // Private constructor to prevent instantiation
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean load() {
        if (loaded) {
            return true;
        }

        synchronized (lock) {
            if (loaded) {
                return true;
            }

            try {
                // Try to load from system library path first
                System.loadLibrary("memdiag-agent");
                loaded = true;
                return true;
            } catch (UnsatisfiedLinkError e) {
                // Try to load from classpath
                return loadFromClasspath();
            }
        }
    }

    private static boolean loadFromClasspath() {
        String osName = System.getProperty("os.name").toLowerCase();
        String libraryName;

        if (osName.contains("linux")) {
            libraryName = "libmemdiag-agent.so";
        } else if (osName.contains("mac")) {
            libraryName = "libmemdiag-agent.dylib";
        } else if (osName.contains("windows")) {
            libraryName = "memdiag-agent.dll";
        } else {
            return false;
        }

        // Try to extract and load from classpath
        try (InputStream is = NativeLoader.class.getResourceAsStream("/" + libraryName)) {
            if (is == null) {
                return false;
            }

            File tempFile = File.createTempFile("memdiag-agent-", libraryName);
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
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
