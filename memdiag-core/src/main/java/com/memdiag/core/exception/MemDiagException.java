package com.memdiag.core.exception;

public class MemDiagException extends RuntimeException {
    public MemDiagException(String message) {
        super(message);
    }

    public MemDiagException(String message, Throwable cause) {
        super(message, cause);
    }
}
