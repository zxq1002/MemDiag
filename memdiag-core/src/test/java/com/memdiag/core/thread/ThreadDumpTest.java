package com.memdiag.core.thread;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ThreadDumpTest {

    @Test
    void createThreadDumpWithStats() {
        ThreadDump dump = new ThreadDump();
        dump.setTimestamp(Instant.now());

        List<ThreadStats> stats = new ArrayList<>();
        ThreadStats thread1 = new ThreadStats();
        thread1.setThreadId(1);
        thread1.setThreadName("main");
        thread1.setState(ThreadState.RUNNABLE);
        stats.add(thread1);

        dump.setThreadStats(stats);

        assertThat(dump.getTimestamp()).isNotNull();
        assertThat(dump.getThreadStats()).hasSize(1);
        assertThat(dump.getThreadStats().get(0).getThreadName()).isEqualTo("main");
    }

    @Test
    void threadStatsWithStackTrace() {
        ThreadStats stats = new ThreadStats();
        stats.setThreadId(1);
        stats.setThreadName("test-thread");
        stats.setState(ThreadState.WAITING);

        List<StackFrame> stackTrace = new ArrayList<>();
        stackTrace.add(new StackFrame("java.lang.Object", "wait", "Object.java", 100));
        stackTrace.add(new StackFrame("com.example.MyClass", "myMethod", "MyClass.java", 50));
        stats.setStackTrace(stackTrace);

        assertThat(stats.getStackTrace()).hasSize(2);
        assertThat(stats.getStackTrace().get(0).getClassName()).isEqualTo("java.lang.Object");
        assertThat(stats.getStackTrace().get(0).getMethodName()).isEqualTo("wait");
    }

    @Test
    void stackFrameNativeMethod() {
        StackFrame frame = new StackFrame();
        frame.setClassName("java.lang.Thread");
        frame.setMethodName("sleep");
        frame.setNativeMethod(true);

        assertThat(frame.isNativeMethod()).isTrue();
    }

    @Test
    void stackFrameWithLineNumber() {
        StackFrame frame = new StackFrame(
                "com.example.Test",
                "testMethod",
                "Test.java",
                42,
                false
        );

        assertThat(frame.getLineNumber()).isEqualTo(42);
        assertThat(frame.getFileName()).isEqualTo("Test.java");
    }

    @Test
    void threadStateEnumValues() {
        assertThat(ThreadState.values()).containsExactlyInAnyOrder(
                ThreadState.NEW,
                ThreadState.RUNNABLE,
                ThreadState.BLOCKED,
                ThreadState.WAITING,
                ThreadState.TIMED_WAITING,
                ThreadState.TERMINATED,
                ThreadState.UNKNOWN
        );
    }

    @Test
    void addMultipleThreads() {
        ThreadDump dump = new ThreadDump();
        List<ThreadStats> stats = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            ThreadStats thread = new ThreadStats();
            thread.setThreadId(i + 1);
            thread.setThreadName("Thread-" + i);
            thread.setState(i % 2 == 0 ? ThreadState.RUNNABLE : ThreadState.WAITING);
            stats.add(thread);
        }

        dump.setThreadStats(stats);

        assertThat(dump.getThreadStats()).hasSize(10);
    }

    @Test
    void deprecatedThreadInfoSupport() {
        ThreadDump dump = new ThreadDump();

        List<ThreadStats> statsList = new ArrayList<>();
        ThreadStats stats = new ThreadStats();
        stats.setThreadId(1);
        stats.setThreadName("test");
        stats.setState(ThreadState.RUNNABLE);
        statsList.add(stats);
        dump.setThreadStats(statsList);

        assertThat(dump.getThreadCount()).isEqualTo(1);
        assertThat(dump.getThreadInfo(1)).isNotNull();
    }

    @Test
    void threadStatsBuilder() {
        ThreadStats stats = ThreadStats.builder()
                .threadId(123)
                .threadName("builder-thread")
                .state(ThreadState.BLOCKED)
                .blockedCount(5)
                .waitedCount(10)
                .build();

        assertThat(stats.getThreadId()).isEqualTo(123);
        assertThat(stats.getThreadName()).isEqualTo("builder-thread");
        assertThat(stats.getState()).isEqualTo(ThreadState.BLOCKED);
        assertThat(stats.getBlockedCount()).isEqualTo(5);
        assertThat(stats.getWaitedCount()).isEqualTo(10);
    }

    @Test
    void toStringContainsRelevantInfo() {
        ThreadStats stats = new ThreadStats();
        stats.setThreadId(42);
        stats.setThreadName("my-thread");
        stats.setState(ThreadState.RUNNABLE);

        String str = stats.toString();

        assertThat(str).contains("42");
        assertThat(str).contains("my-thread");
        assertThat(str).contains("RUNNABLE");
    }
}
