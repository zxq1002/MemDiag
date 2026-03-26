package com.memdiag.core.nativeapi;

public class NativeStackFrame {
    private final String libraryName;
    private final String functionName;
    private final String sourceFile;
    private final int lineNumber;
    private final long instructionAddress;

    private NativeStackFrame(Builder builder) {
        this.libraryName = builder.libraryName;
        this.functionName = builder.functionName;
        this.sourceFile = builder.sourceFile;
        this.lineNumber = builder.lineNumber;
        this.instructionAddress = builder.instructionAddress;
    }

    public String getLibraryName() { return libraryName; }
    public String getFunctionName() { return functionName; }
    public String getSourceFile() { return sourceFile; }
    public int getLineNumber() { return lineNumber; }
    public long getInstructionAddress() { return instructionAddress; }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (functionName != null) {
            sb.append(functionName);
        } else {
            sb.append(String.format("0x%016x", instructionAddress));
        }
        if (libraryName != null) {
            sb.append(" (").append(libraryName);
            if (sourceFile != null) {
                sb.append(":").append(sourceFile);
                if (lineNumber > 0) {
                    sb.append(":").append(lineNumber);
                }
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public static class Builder {
        private String libraryName;
        private String functionName;
        private String sourceFile;
        private int lineNumber;
        private long instructionAddress;

        public Builder libraryName(String libraryName) {
            this.libraryName = libraryName;
            return this;
        }

        public Builder functionName(String functionName) {
            this.functionName = functionName;
            return this;
        }

        public Builder sourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder instructionAddress(long instructionAddress) {
            this.instructionAddress = instructionAddress;
            return this;
        }

        public NativeStackFrame build() {
            return new NativeStackFrame(this);
        }
    }
}
