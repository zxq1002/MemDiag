package com.memdiag.core.thread;

public enum ThreadState {
    NEW,
    RUNNABLE,
    BLOCKED,
    WAITING,
    TIMED_WAITING,
    TERMINATED,
    UNKNOWN
}
