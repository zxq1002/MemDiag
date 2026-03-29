package com.memdiag.agent.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Utility methods for ASM-based bytecode instrumentation.
 */
public class AsmUtils {

    private AsmUtils() {
        // Utility class
    }

    /**
     * Get the appropriate flags for ClassWriter based on the class reader.
     *
     * @param reader Class reader
     * @return Class writer flags
     */
    public static int getClassWriterFlags(ClassReader reader) {
        // Use COMPUTE_FRAMES for Java 7+, but fall back to COMPUTE_MAXS
        // to avoid potential class version compatibility issues
        return ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS;
    }

    /**
     * Get the internal class name from a fully qualified name.
     *
     * @param className Fully qualified class name (e.g., "java.lang.String")
     * @return Internal class name (e.g., "java/lang/String")
     */
    public static String toInternalName(String className) {
        return className.replace('.', '/');
    }

    /**
     * Get the descriptor for a class.
     *
     * @param className Fully qualified class name
     * @return Type descriptor (e.g., "Ljava/lang/String;")
     */
    public static String toDescriptor(String className) {
        return "L" + toInternalName(className) + ";";
    }

    /**
     * Get the appropriate ASM API version based on the class version.
     *
     * @param classVersion Class version from class file
     * @return ASM API version constant
     */
    public static int getAsmApiVersion(int classVersion) {
        if (classVersion >= Opcodes.V21) {
            return Opcodes.ASM9;
        } else if (classVersion >= Opcodes.V17) {
            return Opcodes.ASM9;
        } else if (classVersion >= Opcodes.V11) {
            return Opcodes.ASM9;
        } else if (classVersion >= Opcodes.V9) {
            return Opcodes.ASM7;
        } else if (classVersion >= Opcodes.V1_8) {
            return Opcodes.ASM5;
        } else {
            return Opcodes.ASM4;
        }
    }

    /**
     * Check if a class version is compatible with our instrumentation.
     *
     * @param classVersion Class version from class file
     * @return true if compatible
     */
    public static boolean isCompatibleClassVersion(int classVersion) {
        // Support Java 6+
        return classVersion >= Opcodes.V1_6;
    }
}
