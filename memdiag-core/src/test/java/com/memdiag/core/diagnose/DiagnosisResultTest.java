package com.memdiag.core.diagnose;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DiagnosisResultTest {

    @Test
    void createEmptyDiagnosisResult() {
        DiagnosisResult result = DiagnosisResult.builder()
                .timestamp(Instant.now())
                .build();

        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getIssues()).isEmpty();
        assertThat(result.hasCriticalIssues()).isFalse();
        assertThat(result.hasWarningIssues()).isFalse();
    }

    @Test
    void createDiagnosisResultWithIssues() {
        List<Issue> issues = new ArrayList<>();
        issues.add(Issue.builder()
                .severity(Severity.CRITICAL)
                .type("CRITICAL_ISSUE")
                .title("Critical problem")
                .description("This is a critical issue")
                .build());
        issues.add(Issue.builder()
                .severity(Severity.WARNING)
                .type("WARNING_ISSUE")
                .title("Warning")
                .description("This is a warning")
                .build());
        issues.add(Issue.builder()
                .severity(Severity.INFO)
                .type("INFO_ISSUE")
                .title("Info")
                .description("This is info")
                .build());

        DiagnosisResult result = DiagnosisResult.builder()
                .timestamp(Instant.now())
                .issues(issues)
                .totalHeapUsed(1024 * 1024 * 100)
                .totalHeapCommitted(1024 * 1024 * 500)
                .threadCount(20)
                .summary("Test summary")
                .build();

        assertThat(result.getIssues()).hasSize(3);
        assertThat(result.getCriticalIssues()).hasSize(1);
        assertThat(result.getWarningIssues()).hasSize(1);
        assertThat(result.getInfoIssues()).hasSize(1);
        assertThat(result.hasCriticalIssues()).isTrue();
        assertThat(result.hasWarningIssues()).isTrue();
        assertThat(result.getTotalHeapUsed()).isEqualTo(1024 * 1024 * 100);
        assertThat(result.getTotalHeapCommitted()).isEqualTo(1024 * 1024 * 500);
        assertThat(result.getThreadCount()).isEqualTo(20);
        assertThat(result.getSummary()).isEqualTo("Test summary");
    }

    @Test
    void addIssueToBuilder() {
        DiagnosisResult result = DiagnosisResult.builder()
                .addIssue(Issue.builder()
                        .severity(Severity.INFO)
                        .type("TEST")
                        .title("Test")
                        .description("Test")
                        .build())
                .build();

        assertThat(result.getIssues()).hasSize(1);
    }

    @Test
    void createIssueWithAffectedDetails() {
        Issue issue = Issue.builder()
                .severity(Severity.WARNING)
                .type("LARGE_OBJECT")
                .title("Large object")
                .description("This object is large")
                .affectedClassName("com.example.BigObject")
                .affectedObjectCount(100L)
                .affectedBytes(1024 * 1024 * 100L)
                .build();

        assertThat(issue.getSeverity()).isEqualTo(Severity.WARNING);
        assertThat(issue.getType()).isEqualTo("LARGE_OBJECT");
        assertThat(issue.getTitle()).isEqualTo("Large object");
        assertThat(issue.getDescription()).isEqualTo("This object is large");
        assertThat(issue.getAffectedClassName()).isEqualTo("com.example.BigObject");
        assertThat(issue.getAffectedObjectCount()).isEqualTo(100);
        assertThat(issue.getAffectedBytes()).isEqualTo(1024 * 1024 * 100L);
    }

    @Test
    void createIssueWithRecommendations() {
        Recommendation rec1 = Recommendation.builder()
                .priority("HIGH")
                .title("Fix this")
                .description("You should fix this")
                .action("Run command X")
                .build();
        Recommendation rec2 = Recommendation.builder()
                .priority("MEDIUM")
                .title("Consider this")
                .build();

        Issue issue = Issue.builder()
                .severity(Severity.WARNING)
                .type("TEST")
                .title("Test")
                .description("Test")
                .addRecommendation(rec1)
                .addRecommendation(rec2)
                .build();

        assertThat(issue.getRecommendations()).hasSize(2);
        assertThat(issue.getRecommendations().get(0).getPriority()).isEqualTo("HIGH");
        assertThat(issue.getRecommendations().get(0).getTitle()).isEqualTo("Fix this");
        assertThat(issue.getRecommendations().get(0).getDescription()).isEqualTo("You should fix this");
        assertThat(issue.getRecommendations().get(0).getAction()).isEqualTo("Run command X");
    }

    @Test
    void severityEnumValues() {
        assertThat(Severity.values()).containsExactlyInAnyOrder(
                Severity.CRITICAL,
                Severity.WARNING,
                Severity.INFO
        );
    }
}
