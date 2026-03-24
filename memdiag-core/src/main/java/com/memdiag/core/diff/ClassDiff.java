package com.memdiag.core.diff;

public class ClassDiff {
    private final ClassKey classKey;
    private final long objectCountDelta;
    private final long bytesDelta;
    private final double growthRate;
    private final Long baselineObjectCount;
    private final Long baselineBytes;
    private final Long currentObjectCount;
    private final Long currentBytes;

    private ClassDiff(Builder builder) {
        this.classKey = builder.classKey;
        this.objectCountDelta = builder.objectCountDelta;
        this.bytesDelta = builder.bytesDelta;
        this.growthRate = builder.growthRate;
        this.baselineObjectCount = builder.baselineObjectCount;
        this.baselineBytes = builder.baselineBytes;
        this.currentObjectCount = builder.currentObjectCount;
        this.currentBytes = builder.currentBytes;
    }

    public ClassKey getClassKey() {
        return classKey;
    }

    public long getObjectCountDelta() {
        return objectCountDelta;
    }

    public long getBytesDelta() {
        return bytesDelta;
    }

    public double getGrowthRate() {
        return growthRate;
    }

    public Long getBaselineObjectCount() {
        return baselineObjectCount;
    }

    public Long getBaselineBytes() {
        return baselineBytes;
    }

    public Long getCurrentObjectCount() {
        return currentObjectCount;
    }

    public Long getCurrentBytes() {
        return currentBytes;
    }

    public boolean isGrowing() {
        return bytesDelta > 0;
    }

    public boolean isShrinking() {
        return bytesDelta < 0;
    }

    public boolean isNewClass() {
        return baselineObjectCount == null;
    }

    public boolean isDisappeared() {
        return currentObjectCount == null;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ClassDiff{" +
            "classKey=" + classKey +
            ", objectsDelta=" + objectCountDelta +
            ", bytesDelta=" + bytesDelta +
            ", growthRate=" + growthRate +
            '}';
    }

    public static class Builder {
        private ClassKey classKey;
        private long objectCountDelta;
        private long bytesDelta;
        private double growthRate = 0.0;
        private Long baselineObjectCount;
        private Long baselineBytes;
        private Long currentObjectCount;
        private Long currentBytes;

        public Builder classKey(ClassKey classKey) {
            this.classKey = classKey;
            return this;
        }

        public Builder objectCountDelta(long objectCountDelta) {
            this.objectCountDelta = objectCountDelta;
            return this;
        }

        public Builder bytesDelta(long bytesDelta) {
            this.bytesDelta = bytesDelta;
            return this;
        }

        public Builder growthRate(double growthRate) {
            this.growthRate = growthRate;
            return this;
        }

        public Builder baselineObjectCount(Long baselineObjectCount) {
            this.baselineObjectCount = baselineObjectCount;
            return this;
        }

        public Builder baselineBytes(Long baselineBytes) {
            this.baselineBytes = baselineBytes;
            return this;
        }

        public Builder currentObjectCount(Long currentObjectCount) {
            this.currentObjectCount = currentObjectCount;
            return this;
        }

        public Builder currentBytes(Long currentBytes) {
            this.currentBytes = currentBytes;
            return this;
        }

        public ClassDiff build() {
            return new ClassDiff(this);
        }
    }
}
