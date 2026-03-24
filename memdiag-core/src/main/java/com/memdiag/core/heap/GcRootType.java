package com.memdiag.core.heap;

public enum GcRootType {
    /** System class (e.g., classes loaded by bootstrap class loader) */
    SYSTEM_CLASS,
    /** JNI local reference */
    JNI_LOCAL,
    /** JNI global reference */
    JNI_GLOBAL,
    /** Java-level static field */
    STATIC_FIELD,
    /** Java-level instance field (not a root, but included in path) */
    INSTANCE_FIELD,
    /** Thread stack local variable */
    THREAD_STACK,
    /** Monitor (synchronized block/object) */
    MONITOR,
    /** Other root type */
    OTHER
}
