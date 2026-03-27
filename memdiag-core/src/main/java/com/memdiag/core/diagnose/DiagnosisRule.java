package com.memdiag.core.diagnose;

import java.util.List;

/**
 * Interface for diagnosis rules.
 * Implementations should be stateless and thread-safe.
 */
public interface DiagnosisRule {

    /**
     * Get the unique identifier for this rule.
     * @return rule ID (e.g., "LARGE_CLASS")
     */
    String getId();

    /**
     * Get the human-readable name of this rule.
     * @return rule name
     */
    String getName();

    /**
     * Get the description of what this rule checks.
     * @return rule description
     */
    String getDescription();

    /**
     * Check if this rule is enabled.
     * @return true if enabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Evaluate this rule against the given context.
     * @param context the diagnosis context containing heap and thread data
     * @return list of issues found (empty list if none)
     */
    List<Issue> evaluate(DiagnosisContext context);
}
