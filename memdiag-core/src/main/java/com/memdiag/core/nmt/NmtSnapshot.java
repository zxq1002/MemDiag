package com.memdiag.core.nmt;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NMT 快照数据
 */
public class NmtSnapshot {
    private final Instant timestamp;
    private final List<NmtMemoryUsage> usages;
    private final Map<NmtCategory, NmtMemoryUsage> usageByCategory;

    private NmtSnapshot(Builder builder) {
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.usages = Collections.unmodifiableList(new ArrayList<>(builder.usages));

        Map<NmtCategory, NmtMemoryUsage> map = new HashMap<>();
        for (NmtMemoryUsage usage : usages) {
            map.put(usage.getCategory(), usage);
        }
        this.usageByCategory = Collections.unmodifiableMap(map);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<NmtMemoryUsage> getUsages() {
        return usages;
    }

    public NmtMemoryUsage getUsage(NmtCategory category) {
        return usageByCategory.get(category);
    }

    public long getTotalReserved() {
        return usages.stream().mapToLong(NmtMemoryUsage::getReserved).sum();
    }

    public long getTotalCommitted() {
        return usages.stream().mapToLong(NmtMemoryUsage::getCommitted).sum();
    }

    public long getTotalMalloced() {
        return usages.stream().mapToLong(NmtMemoryUsage::getMalloced).sum();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "NmtSnapshot{" +
            "timestamp=" + timestamp +
            ", usageCount=" + usages.size() +
            ", totalReserved=" + getTotalReserved() +
            ", totalCommitted=" + getTotalCommitted() +
            '}';
    }

    public static class Builder {
        private Instant timestamp;
        private List<NmtMemoryUsage> usages = new ArrayList<>();

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder usages(List<NmtMemoryUsage> usages) {
            this.usages = new ArrayList<>(usages);
            return this;
        }

        public Builder addUsage(NmtMemoryUsage usage) {
            this.usages.add(usage);
            return this;
        }

        public NmtSnapshot build() {
            return new NmtSnapshot(this);
        }
    }
}
