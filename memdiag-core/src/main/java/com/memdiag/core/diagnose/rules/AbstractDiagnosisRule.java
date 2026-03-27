package com.memdiag.core.diagnose.rules;

import com.memdiag.core.diagnose.DiagnosisRule;

/**
 * Abstract base class for diagnosis rules with common functionality.
 */
public abstract class AbstractDiagnosisRule implements DiagnosisRule {

    private final String id;
    private final String name;
    private final String description;

    protected AbstractDiagnosisRule(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Truncate class name for display.
     */
    protected String truncateClassName(String className, int maxLen) {
        if (className == null || className.isEmpty()) {
            return "";
        }
        if (className.length() <= maxLen) return className;
        return "..." + className.substring(className.length() - maxLen + 3);
    }
}
