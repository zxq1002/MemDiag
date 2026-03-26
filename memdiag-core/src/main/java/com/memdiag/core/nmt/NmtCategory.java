package com.memdiag.core.nmt;

/**
 * NMT 内存分类
 */
public enum NmtCategory {
    JAVA_HEAP("Java Heap"),
    CLASS("Class"),
    THREAD("Thread"),
    CODE("Code"),
    GC("GC"),
    COMPILER("Compiler"),
    INTERNAL("Internal"),
    SYMBOL("Symbol"),
    NATIVE_LIBRARY("Native Library"),
    UNKNOWN("Unknown");

    private final String displayName;

    NmtCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static NmtCategory fromString(String name) {
        for (NmtCategory category : values()) {
            if (category.displayName.equalsIgnoreCase(name) ||
                category.name().equalsIgnoreCase(name)) {
                return category;
            }
        }
        return UNKNOWN;
    }
}
