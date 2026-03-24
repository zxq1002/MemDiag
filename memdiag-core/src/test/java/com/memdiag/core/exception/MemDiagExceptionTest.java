package com.memdiag.core.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MemDiagExceptionTest {

    @Test
    void exceptionWithMessage() {
        MemDiagException e = new MemDiagException("test message");
        assertThat(e.getMessage()).isEqualTo("test message");
    }

    @Test
    void exceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("cause");
        MemDiagException e = new MemDiagException("test", cause);
        assertThat(e.getCause()).isEqualTo(cause);
    }
}
