package com.memdiag.core.util;

import com.memdiag.core.exception.ResourceLimitExceededException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ResourceLimiter {
    private final long maxMemoryBytes;
    private final Duration analysisTimeout;
    private final Duration maxSafePointTime;
    private final AtomicLong lastSafePointDuration = new AtomicLong(0);

    public ResourceLimiter(long maxMemoryBytes, Duration analysisTimeout, Duration maxSafePointTime) {
        this.maxMemoryBytes = maxMemoryBytes;
        this.analysisTimeout = analysisTimeout;
        this.maxSafePointTime = maxSafePointTime;
    }

    public <T> T executeWithLimit(Supplier<T> task) {
        return task.get();
    }

    public <T> T executeWithSafePointMonitor(Supplier<T> task) {
        long start = System.currentTimeMillis();
        try {
            return task.get();
        } finally {
            long duration = System.currentTimeMillis() - start;
            lastSafePointDuration.set(duration);

            if (duration > maxSafePointTime.toMillis()) {
                // 记录警告，暂不主动中止
            }
        }
    }

    public long getLastSafePointDuration() {
        return lastSafePointDuration.get();
    }
}
