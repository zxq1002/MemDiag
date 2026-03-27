package com.memdiag.core.diagnose.rules;

import com.memdiag.core.diagnose.DiagnosisContext;
import com.memdiag.core.diagnose.DiagnosisRule;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Severity;

import java.util.ArrayList;
import java.util.List;

/**
 * Example of a custom diagnosis rule.
 * <p>
 * To add your own custom rule:
 * <ol>
 *   <li>Implement the {@link DiagnosisRule} interface</li>
 *   <li>Register it programmatically with {@link RuleRegistry#register(DiagnosisRule)}</li>
 *   <li>Or add it to META-INF/services/com.memdiag.core.diagnose.DiagnosisRule</li>
 * </ol>
 */
public class CustomRuleExample extends AbstractDiagnosisRule {

    public static final String RULE_ID = "CUSTOM_EXAMPLE";

    public CustomRuleExample() {
        super(RULE_ID,
              "Custom Rule Example",
              "Example rule showing how to create custom diagnosis rules");
    }

    @Override
    public List<Issue> evaluate(DiagnosisContext context) {
        List<Issue> issues = new ArrayList<>();

        // Add your custom logic here
        // Example: Check if total heap used is above a threshold
        if (context.getTotalHeapUsed() > 1_000_000_000L) { // 1GB
            issues.add(Issue.builder()
                .severity(Severity.INFO)
                .type(RULE_ID)
                .title("Heap usage above 1GB")
                .description("Total heap usage is " + context.getTotalHeapUsed() + " bytes")
                .build());
        }

        return issues;
    }

    @Override
    public boolean isEnabled() {
        // Disable by default, users must explicitly enable
        return false;
    }
}
