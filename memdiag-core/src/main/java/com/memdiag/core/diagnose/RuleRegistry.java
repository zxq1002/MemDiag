package com.memdiag.core.diagnose;

import com.memdiag.core.diagnose.rules.BlockedThreadsRule;
import com.memdiag.core.diagnose.rules.HeapLeakDetectionRule;
import com.memdiag.core.diagnose.rules.LargeClassRule;
import com.memdiag.core.diagnose.rules.LargeCollectionRule;
import com.memdiag.core.diagnose.rules.ManyInstancesRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Registry for diagnosis rules.
 * Supports both programmatic registration and ServiceLoader discovery.
 */
public class RuleRegistry {

    private final Map<String, DiagnosisRule> rules = new LinkedHashMap<>();

    /**
     * Create a new empty registry.
     */
    public RuleRegistry() {
    }

    /**
     * Create a registry with the default built-in rules.
     * @return registry with default rules
     */
    public static RuleRegistry withDefaults() {
        RuleRegistry registry = new RuleRegistry();
        registry.registerDefaultRules();
        return registry;
    }

    /**
     * Register the default built-in rules.
     */
    public void registerDefaultRules() {
        register(new LargeClassRule());
        register(new ManyInstancesRule());
        register(new BlockedThreadsRule());
        register(new LargeCollectionRule());
        register(new HeapLeakDetectionRule());
    }

    /**
     * Register a single rule.
     * @param rule the rule to register
     */
    public void register(DiagnosisRule rule) {
        if (rule != null && rule.getId() != null) {
            rules.put(rule.getId(), rule);
        }
    }

    /**
     * Register multiple rules.
     * @param rules the rules to register
     */
    public void registerAll(Iterable<DiagnosisRule> rules) {
        if (rules != null) {
            for (DiagnosisRule rule : rules) {
                register(rule);
            }
        }
    }

    /**
     * Discover and register rules using ServiceLoader.
     */
    public void discoverRules() {
        ServiceLoader<DiagnosisRule> loader = ServiceLoader.load(DiagnosisRule.class);
        for (DiagnosisRule rule : loader) {
            register(rule);
        }
    }

    /**
     * Get a rule by ID.
     * @param ruleId the rule ID
     * @return the rule, or null if not found
     */
    public DiagnosisRule getRule(String ruleId) {
        return rules.get(ruleId);
    }

    /**
     * Get all registered rules.
     * @return unmodifiable list of rules
     */
    public List<DiagnosisRule> getRules() {
        return Collections.unmodifiableList(new ArrayList<>(rules.values()));
    }

    /**
     * Get all enabled rules.
     * @return unmodifiable list of enabled rules
     */
    public List<DiagnosisRule> getEnabledRules() {
        List<DiagnosisRule> enabled = new ArrayList<>();
        for (DiagnosisRule rule : rules.values()) {
            if (rule.isEnabled()) {
                enabled.add(rule);
            }
        }
        return Collections.unmodifiableList(enabled);
    }

    /**
     * Remove a rule by ID.
     * @param ruleId the rule ID
     * @return the removed rule, or null if not found
     */
    public DiagnosisRule remove(String ruleId) {
        return rules.remove(ruleId);
    }

    /**
     * Clear all rules.
     */
    public void clear() {
        rules.clear();
    }

    /**
     * Check if a rule is registered.
     * @param ruleId the rule ID
     * @return true if registered
     */
    public boolean hasRule(String ruleId) {
        return rules.containsKey(ruleId);
    }

    /**
     * Get the number of registered rules.
     * @return rule count
     */
    public int size() {
        return rules.size();
    }
}
