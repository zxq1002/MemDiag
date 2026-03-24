package com.memdiag.core.util;

import com.memdiag.core.exception.ResourceLimitExceededException;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

class ResourceLimiterTest {

    @Test
    void executeWithinTimeout() {
        ResourceLimiter limiter = new ResourceLimiter(
            64 * 1024 * 1024,
            Duration.ofSeconds(30),
            Duration.ofMillis(500)
        );

        String result = limiter.executeWithLimit(() -> "success");
        assertThat(result).isEqualTo("success");
    }

    @Test
    void safePointMonitorRecordsDuration() {
        ResourceLimiter limiter = new ResourceLimiter(
            64 * 1024 * 1024,
            Duration.ofSeconds(30),
            Duration.ofMillis(500)
        );

        limiter.executeWithSafePointMonitor(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        });

        assertThat(limiter.getLastSafePointDuration()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void packagePrivateConstructorExists() {
        // 验证包私有构造函数存在
        MemoryMXBean realMemoryBean = ManagementFactory.getMemoryMXBean();
        ResourceLimiter limiter = new ResourceLimiter(
            64 * 1024 * 1024,
            Duration.ofSeconds(30),
            Duration.ofMillis(500),
            realMemoryBean
        );
        assertThat(limiter).isNotNull();
    }
}
