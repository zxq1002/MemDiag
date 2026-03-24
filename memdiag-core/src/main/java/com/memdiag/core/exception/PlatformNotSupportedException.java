package com.memdiag.core.exception;

public class PlatformNotSupportedException extends MemDiagException {
    public PlatformNotSupportedException(String message) {
        super(message);
    }

    public PlatformNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
