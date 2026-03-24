package com.memdiag.core.diagnose;

public class Recommendation {
    private final String priority;
    private final String title;
    private final String description;
    private final String action;

    private Recommendation(Builder builder) {
        this.priority = builder.priority;
        this.title = builder.title;
        this.description = builder.description;
        this.action = builder.action;
    }

    public String getPriority() {
        return priority;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getAction() {
        return action;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Recommendation{" +
            "priority='" + priority + '\'' +
            ", title='" + title + '\'' +
            '}';
    }

    public static class Builder {
        private String priority = "MEDIUM";
        private String title;
        private String description;
        private String action;

        public Builder priority(String priority) {
            this.priority = priority;
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

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Recommendation build() {
            return new Recommendation(this);
        }
    }
}
