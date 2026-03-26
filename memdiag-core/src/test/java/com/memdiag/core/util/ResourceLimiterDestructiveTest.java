package com.memdiag.core.util;

import com.memdiag.core.exception.ResourceLimitExceededException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceLimiterDestructiveTest {

    @Test
    void testTimeoutExceeded() {
        // SLA: 500ms
        ResourceLimiter limiter = new ResourceLimiter(
            Long.MAX_VALUE, 
            Duration.ofMillis(500), 
            Duration.ofMillis(500)
        );

        // 模拟 600ms 的慢任务
        assertThatThrownBy(() -> {
            limiter.executeWithLimit(() -> {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "completed";
            });
        }).isInstanceOf(ResourceLimitExceededException.class)
          .hasMessageContaining("Analysis timed out after 500ms");
    }
}
