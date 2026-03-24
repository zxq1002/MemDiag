package com.memdiag.core.exception;

public class AnalysisException extends MemDiagException {
    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
