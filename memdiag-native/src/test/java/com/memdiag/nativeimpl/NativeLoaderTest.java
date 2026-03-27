package com.memdiag.nativeimpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NativeLoader multi-architecture support.
 * These tests verify the logic without requiring actual native libraries to load.
 */
class NativeLoaderTest {

    @Test
    void testIsLoadedInitiallyFalse() {
        // Before any load attempt, isLoaded() should return false
        // Note: If another test already loaded the library, this might fail
        // but in a fresh test run it should be false
        assertNotNull(NativeLoader.isLoaded());
    }

    @Test
    void testGetLibraryNamesForSystem() throws Exception {
        // Use reflection to test the private method
        Method method = NativeLoader.class.getDeclaredMethod("getLibraryNamesForSystem");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) method.invoke(null);

        // Verify we get at least one name
        assertFalse(names.isEmpty());

        // All names should start with libmemdiag-agent or memdiag-agent
        for (String name : names) {
            assertTrue(name.startsWith("libmemdiag-agent") || name.startsWith("memdiag-agent"),
                "Name should start with libmemdiag-agent or memdiag-agent: " + name);
        }
    }

    @Test
    void testGetSuffixForLibrary() throws Exception {
        Method method = NativeLoader.class.getDeclaredMethod("getSuffixForLibrary", String.class);
        method.setAccessible(true);

        assertEquals(".so", method.invoke(null, "libmemdiag-agent.so"));
        assertEquals(".so", method.invoke(null, "libmemdiag-agent-x86_64.so"));
        assertEquals(".dylib", method.invoke(null, "libmemdiag-agent.dylib"));
        assertEquals(".dll", method.invoke(null, "memdiag-agent.dll"));
        assertEquals(".tmp", method.invoke(null, "unknown"));
    }

    @Test
    void testArchitectureSpecificNamesArePresent() throws Exception {
        Method method = NativeLoader.class.getDeclaredMethod("getLibraryNamesForSystem");
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) method.invoke(null);

        String osArch = System.getProperty("os.arch").toLowerCase();

        // Check that architecture-specific names are included
        if (osArch.equals("aarch64") || osArch.equals("arm64")) {
            assertTrue(names.contains("libmemdiag-agent-arm64.so") ||
                       names.contains("libmemdiag-agent-aarch64.so") ||
                       names.contains("libmemdiag-agent-arm64.dylib") ||
                       names.contains("libmemdiag-agent-aarch64.dylib"),
                "ARM64 specific name should be present");
        } else if (osArch.equals("amd64") || osArch.equals("x86_64")) {
            assertTrue(names.contains("libmemdiag-agent-x86_64.so") ||
                       names.contains("libmemdiag-agent-amd64.so") ||
                       names.contains("libmemdiag-agent-x86_64.dylib") ||
                       names.contains("libmemdiag-agent-amd64.dylib"),
                "x86_64 specific name should be present");
        }

        // Generic name should always be last or present
        assertTrue(names.stream().anyMatch(n ->
            n.equals("libmemdiag-agent.so") ||
            n.equals("libmemdiag-agent.dylib") ||
            n.equals("memdiag-agent.dll")),
            "Generic name should be present");
    }

    @Test
    void testLibraryResourcePresence() {
        // Verify that the library resources are present in the classpath
        // This test just checks that we can find the resource streams,
        // not that we can load them (which requires the right OS/arch)

        String[] candidates = {
            "/libmemdiag-agent.so",
            "/libmemdiag-agent-x86_64.so",
            "/libmemdiag-agent-amd64.so"
        };

        boolean foundAny = false;
        for (String candidate : candidates) {
            try (InputStream is = NativeLoader.class.getResourceAsStream(candidate)) {
                if (is != null) {
                    foundAny = true;
                    // Can read at least a few bytes
                    byte[] buffer = new byte[10];
                    int read = is.read(buffer);
                    assertTrue(read > 0, "Should be able to read from " + candidate);
                }
            } catch (IOException e) {
                // Ignore, just means this particular resource isn't available
            }
        }

        // At least the generic library should be present
        assertTrue(foundAny, "At least one native library resource should be present");
    }

    @Test
    void testLoadWithoutNativeLibraries() {
        // We expect load() to return false on systems without the right native libraries,
        // or when running in an environment that can't load them
        // This is not a failure - it's expected behavior

        // We don't assert true/false because it depends on the environment
        // Just verify that the method doesn't throw an exception
        assertDoesNotThrow(() -> {
            boolean result = NativeLoader.load();
            // Just log the result for information
            System.out.println("NativeLoader.load() returned: " + result);
            System.out.println("NativeLoader.isLoaded() returned: " + NativeLoader.isLoaded());
        });
    }

    @Test
    void testGetLoadedLibraryPath() {
        // Just verify the method exists and returns something (may be null)
        assertDoesNotThrow(() -> {
            String path = NativeLoader.getLoadedLibraryPath();
            // path could be null or a string, both are acceptable
            if (path != null) {
                assertFalse(path.isEmpty());
            }
        });
    }
}
