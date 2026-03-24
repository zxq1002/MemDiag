package com.memdiag.core.diagnose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Issue {
    private final Severity severity;
    private final String type;
    private final String title;
    private final String description;
    private final List<Recommendation> recommendations;
    private final String affectedClassName;
    private final Long affectedObjectCount;
    private final Long affectedBytes;

    private Issue(Builder builder) {
        this.severity = builder.severity;
        this.type = builder.type;
        this.title = builder.title;
        this.description = builder.description;
        this.recommendations = Collections.unmodifiableList(new ArrayList<>(builder.recommendations));
        this.affectedClassName = builder.affectedClassName;
        this.affectedObjectCount = builder.affectedObjectCount;
        this.affectedBytes = builder.affectedBytes;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public String getAffectedClassName() {
        return affectedClassName;
    }

    public Long getAffectedObjectCount() {
        return affectedObjectCount;
    }

    public Long getAffectedBytes() {
        return affectedBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Issue{" +
            "severity=" + severity +
            ", type='" + type + '\'' +
            ", title='" + title + '\'' +
            '}';
    }

    public static class Builder {
        private Severity severity = Severity.INFO;
        private String type;
        private String title;
        private String description;
        private List<Recommendation> recommendations = new ArrayList<>();
        private String affectedClassName;
        private Long affectedObjectCount;
        private Long affectedBytes;

        public Builder severity(Severity severity) {
            this.severity = severity;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder recommendations(List<Recommendation> recommendations) {
            this.recommendations = new ArrayList<>(recommendations);
            return this;
        }

        public Builder addRecommendation(Recommendation recommendation) {
            this.recommendations.add(recommendation);
            return this;
        }

        public Builder affectedClassName(String affectedClassName) {
            this.affectedClassName = affectedClassName;
            return this;
        }

        public Builder affectedObjectCount(Long affectedObjectCount) {
            this.affectedObjectCount = affectedObjectCount;
            return this;
        }

        public Builder affectedBytes(Long affectedBytes) {
            this.affectedBytes = affectedBytes;
            return this;
        }

        public Issue build() {
            return new Issue(this);
        }
    }
}
