package com.memdiag.core.nativeapi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NativeDiagnosis {
    private final List<String> findings;
    private final List<String> warnings;
    private final List<String> recommendations;

    private NativeDiagnosis(Builder builder) {
        this.findings = Collections.unmodifiableList(new ArrayList<>(builder.findings));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
        this.recommendations = Collections.unmodifiableList(new ArrayList<>(builder.recommendations));
    }

    public List<String> getFindings() { return findings; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getRecommendations() { return recommendations; }

    public boolean hasFindings() { return !findings.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> findings = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<String> recommendations = new ArrayList<>();

        public Builder findings(List<String> findings) {
            this.findings = new ArrayList<>(findings);
            return this;
        }

        public Builder addFinding(String finding) {
            this.findings.add(finding);
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = new ArrayList<>(warnings);
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder recommendations(List<String> recommendations) {
            this.recommendations = new ArrayList<>(recommendations);
            return this;
        }

        public Builder addRecommendation(String recommendation) {
            this.recommendations.add(recommendation);
            return this;
        }

        public NativeDiagnosis build() {
            return new NativeDiagnosis(this);
        }
    }
}
