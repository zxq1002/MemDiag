package com.memdiag.core.thread;

public class StackFrame {
    private final String className;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;
    private final boolean nativeMethod;

    public StackFrame(String className, String methodName, String fileName, int lineNumber, boolean nativeMethod) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.nativeMethod = nativeMethod;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isNativeMethod() {
        return nativeMethod;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append(".").append(methodName);
        if (nativeMethod) {
            sb.append("(Native Method)");
        } else if (fileName != null) {
            sb.append("(").append(fileName);
            if (lineNumber >= 0) {
                sb.append(":").append(lineNumber);
            }
            sb.append(")");
        } else {
            sb.append("(Unknown Source)");
        }
        return sb.toString();
    }
}
