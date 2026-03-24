package com.memdiag.core.diagnose;

public enum Severity {
    INFO("Info"),
    WARNING("Warning"),
    CRITICAL("Critical");

    private final String displayName;

    Severity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
