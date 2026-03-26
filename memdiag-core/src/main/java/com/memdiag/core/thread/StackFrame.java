package com.memdiag.core.thread;

import java.io.Serializable;

public class StackFrame implements Serializable {
    private static final long serialVersionUID = 1L;

    private String className;
    private String methodName;
    private String fileName;
    private int lineNumber;
    private boolean nativeMethod;

    public StackFrame() {
    }

    public StackFrame(String className, String methodName, String fileName, int lineNumber) {
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.nativeMethod = false;
    }

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

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isNativeMethod() {
        return nativeMethod;
    }

    public void setNativeMethod(boolean nativeMethod) {
        this.nativeMethod = nativeMethod;
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
