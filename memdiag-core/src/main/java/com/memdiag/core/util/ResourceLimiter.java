package com.memdiag.core.util;

import com.memdiag.core.exception.ResourceLimitExceededException;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ResourceLimiter {
    private final long maxMemoryBytes;
    private final Duration analysisTimeout;
    private final Duration maxSafePointTime;
    private final AtomicLong lastSafePointDuration = new AtomicLong(0);
    private final MemoryMXBean memoryMXBean;

    public ResourceLimiter(long maxMemoryBytes, Duration analysisTimeout, Duration maxSafePointTime) {
        this(maxMemoryBytes, analysisTimeout, maxSafePointTime, ManagementFactory.getMemoryMXBean());
    }

    // 用于测试的构造函数
    ResourceLimiter(long maxMemoryBytes, Duration analysisTimeout, Duration maxSafePointTime, MemoryMXBean memoryMXBean) {
        this.maxMemoryBytes = maxMemoryBytes;
        this.analysisTimeout = analysisTimeout;
        this.maxSafePointTime = maxSafePointTime;
        this.memoryMXBean = memoryMXBean;
    }

    public <T> T executeWithLimit(Supplier<T> task) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task::get);

        try {
            // 检查内存使用
            checkMemoryUsage();

            // 在超时时间内执行任务
            T result = future.get(analysisTimeout.toMillis(), TimeUnit.MILLISECONDS);

            // 再次检查内存使用
            checkMemoryUsage();

            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ResourceLimitExceededException("Analysis timed out after " + analysisTimeout.toMillis() + "ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Task execution failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new RuntimeException("Task interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private void checkMemoryUsage() {
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
        if (usedMemory > maxMemoryBytes) {
            throw new ResourceLimitExceededException(
                String.format("Memory limit exceeded: used %,d bytes, limit %,d bytes",
                    usedMemory, maxMemoryBytes)
            );
        }
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
                System.err.printf("Warning: Safe point duration %,dms exceeded limit %,dms%n",
                    duration, maxSafePointTime.toMillis());
            }
        }
    }

    public long getLastSafePointDuration() {
        return lastSafePointDuration.get();
    }
}
