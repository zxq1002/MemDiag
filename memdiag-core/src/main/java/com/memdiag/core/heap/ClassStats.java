package com.memdiag.core.heap;

public class ClassStats {
    private final String className;
    private final long objectCount;
    private final long shallowBytes;

    public ClassStats(String className, long objectCount, long shallowBytes) {
        this.className = className;
        this.objectCount = objectCount;
        this.shallowBytes = shallowBytes;
    }

    public String getClassName() { return className; }
    public long getObjectCount() { return objectCount; }
    public long getShallowBytes() { return shallowBytes; }
}
