package com.memdiag.core.thread;

import java.io.Serializable;

public enum ThreadState implements Serializable {
    NEW,
    RUNNABLE,
    BLOCKED,
    WAITING,
    TIMED_WAITING,
    TERMINATED,
    UNKNOWN
}
