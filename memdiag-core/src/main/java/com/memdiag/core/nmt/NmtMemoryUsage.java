package com.memdiag.core.nmt;

/**
 * NMT 内存使用统计
 */
public class NmtMemoryUsage {
    private final NmtCategory category;
    private final long reserved;
    private final long committed;
    private final long malloced;
    private final long mallocCount;

    private NmtMemoryUsage(Builder builder) {
        this.category = builder.category;
        this.reserved = builder.reserved;
        this.committed = builder.committed;
        this.malloced = builder.malloced;
        this.mallocCount = builder.mallocCount;
    }

    public NmtCategory getCategory() {
        return category;
    }

    public long getReserved() {
        return reserved;
    }

    public long getCommitted() {
        return committed;
    }

    public long getMalloced() {
        return malloced;
    }

    public long getMallocCount() {
        return mallocCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "NmtMemoryUsage{" +
            "category=" + category +
            ", reserved=" + reserved +
            ", committed=" + committed +
            ", malloced=" + malloced +
            ", mallocCount=" + mallocCount +
            '}';
    }

    public static class Builder {
        private NmtCategory category = NmtCategory.UNKNOWN;
        private long reserved;
        private long committed;
        private long malloced;
        private long mallocCount;

        public Builder category(NmtCategory category) {
            this.category = category;
            return this;
        }

        public Builder reserved(long reserved) {
            this.reserved = reserved;
            return this;
        }

        public Builder committed(long committed) {
            this.committed = committed;
            return this;
        }

        public Builder malloced(long malloced) {
            this.malloced = malloced;
            return this;
        }

        public Builder mallocCount(long mallocCount) {
            this.mallocCount = mallocCount;
            return this;
        }

        public NmtMemoryUsage build() {
            return new NmtMemoryUsage(this);
        }
    }
}
