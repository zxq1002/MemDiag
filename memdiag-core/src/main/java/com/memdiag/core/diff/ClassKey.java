package com.memdiag.core.diff;

import java.util.Objects;

public class ClassKey {
    private final String className;
    private final int classLoaderHash;

    public ClassKey(String className, int classLoaderHash) {
        this.className = className;
        this.classLoaderHash = classLoaderHash;
    }

    public ClassKey(String className) {
        this(className, 0);
    }

    public String getClassName() {
        return className;
    }

    public int getClassLoaderHash() {
        return classLoaderHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassKey classKey = (ClassKey) o;
        return classLoaderHash == classKey.classLoaderHash &&
            Objects.equals(className, classKey.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, classLoaderHash);
    }

    @Override
    public String toString() {
        if (classLoaderHash == 0) {
            return className;
        }
        return className + "@" + Integer.toHexString(classLoaderHash);
    }
}
