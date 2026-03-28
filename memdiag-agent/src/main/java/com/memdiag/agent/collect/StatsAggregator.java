package com.memdiag.agent.collect;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Aggregates statistics over time and provides trend analysis.
 */
public class StatsAggregator {

    private final DataCollector dataCollector;

    // Historical data points for trend analysis
    private final ConcurrentLinkedDeque<StatsSnapshot> history = new ConcurrentLinkedDeque<>();
    private static final int MAX_HISTORY_POINTS = 60; // Keep 60 data points
    private static final long SNAPSHOT_INTERVAL_MS = 1000; // 1 second

    private volatile long lastSnapshotTime = 0;

    public StatsAggregator(DataCollector dataCollector) {
        this.dataCollector = dataCollector;
    }

    /**
     * Take a snapshot of current statistics (call periodically).
     */
    public void takeSnapshot() {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotTime < SNAPSHOT_INTERVAL_MS) {
            return;
        }

        StatsSnapshot snapshot = new StatsSnapshot(
                now,
                dataCollector.getTotalAllocated(),
                dataCollector.getAllocationCount(),
                dataCollector.getWindowStats()
        );

        history.addLast(snapshot);

        // Trim history
        while (history.size() > MAX_HISTORY_POINTS) {
            history.pollFirst();
        }

        lastSnapshotTime = now;
    }

    /**
     * Get the current allocation rate in bytes per second.
     */
    public long getCurrentRateBytesPerSec() {
        if (history.size() < 2) {
            return dataCollector.getAllocationRateBytesPerSec(60_000);
        }

        StatsSnapshot latest = history.getLast();
        StatsSnapshot previous = findSnapshotBefore(latest.timestamp - 5000);

        if (previous == null) {
            return dataCollector.getAllocationRateBytesPerSec(60_000);
        }

        long timeDeltaSec = (latest.timestamp - previous.timestamp) / 1000;
        if (timeDeltaSec == 0) {
            return 0;
        }

        return (latest.totalAllocated - previous.totalAllocated) / timeDeltaSec;
    }

    /**
     * Get the trend (increasing, decreasing, stable).
     */
    public Trend getTrend() {
        if (history.size() < 10) {
            return Trend.UNKNOWN;
        }

        // Calculate linear regression slope
        List<StatsSnapshot> points = new ArrayList<>(history);
        int n = points.size();

        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;

        long firstTimestamp = points.get(0).timestamp;

        for (int i = 0; i < n; i++) {
            StatsSnapshot point = points.get(i);
            double x = (point.timestamp - firstTimestamp) / 1000.0; // seconds
            double y = point.totalAllocated;

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);

        double threshold = getCurrentRateBytesPerSec() * 0.1; // 10% of current rate

        if (slope > threshold) {
            return Trend.INCREASING;
        } else if (slope < -threshold) {
            return Trend.DECREASING;
        } else {
            return Trend.STABLE;
        }
    }

    /**
     * Predict when the given limit will be reached at current rate.
     * Returns seconds, or -1 if rate is <= 0.
     */
    public long predictTimeToLimit(long currentUsage, long limit) {
        long rate = getCurrentRateBytesPerSec();
        if (rate <= 0) {
            return -1;
        }

        long remaining = limit - currentUsage;
        if (remaining <= 0) {
            return 0;
        }

        return remaining / rate;
    }

    /**
     * Get allocation rate history.
     */
    public List<RatePoint> getRateHistory(int windowSec) {
        List<RatePoint> result = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - windowSec * 1000L;

        StatsSnapshot previous = null;
        for (StatsSnapshot snapshot : history) {
            if (snapshot.timestamp < cutoff) {
                previous = snapshot;
                continue;
            }

            if (previous != null) {
                long timeDeltaSec = (snapshot.timestamp - previous.timestamp) / 1000;
                if (timeDeltaSec > 0) {
                    long rate = (snapshot.totalAllocated - previous.totalAllocated) / timeDeltaSec;
                    result.add(new RatePoint(snapshot.timestamp, Math.max(0, rate)));
                }
            }
            previous = snapshot;
        }

        return result;
    }

    private StatsSnapshot findSnapshotBefore(long timestamp) {
        StatsSnapshot result = null;
        for (StatsSnapshot snapshot : history) {
            if (snapshot.timestamp >= timestamp) {
                break;
            }
            result = snapshot;
        }
        return result;
    }

    /**
     * Get a summary of all statistics.
     */
    public Map<String, Object> getSummary() {
        takeSnapshot();

        Map<String, Object> summary = new HashMap<>();
        summary.put("currentRateBytesPerSec", getCurrentRateBytesPerSec());
        summary.put("currentRateMBPerSec", getCurrentRateBytesPerSec() / (1024 * 1024));
        summary.put("trend", getTrend().name());
        summary.put("historyPoints", history.size());

        // Top types
        summary.put("topTypesBySize", convertTypeStatsToMapList(
                dataCollector.getTopTypesBySize(10)));
        summary.put("topTypesByCount", convertTypeStatsToMapList(
                dataCollector.getTopTypesByCount(10)));

        // Window stats
        summary.put("windowStats", dataCollector.getWindowStats());

        // Base collector stats
        summary.putAll(dataCollector.toMap());

        return summary;
    }

    private List<Map<String, Object>> convertTypeStatsToMapList(List<DataCollector.TypeStats> stats) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (DataCollector.TypeStats stat : stats) {
            result.add(stat.toMap());
        }
        return result;
    }

    /**
     * Clear history.
     */
    public void clear() {
        history.clear();
        lastSnapshotTime = 0;
    }

    // ========== Helper classes ==========

    public enum Trend {
        INCREASING,
        DECREASING,
        STABLE,
        UNKNOWN
    }

    private static class StatsSnapshot {
        final long timestamp;
        final long totalAllocated;
        final long allocationCount;
        final Map<String, Long> windowStats;

        StatsSnapshot(long timestamp, long totalAllocated, long allocationCount, Map<String, Long> windowStats) {
            this.timestamp = timestamp;
            this.totalAllocated = totalAllocated;
            this.allocationCount = allocationCount;
            this.windowStats = new HashMap<>(windowStats);
        }
    }

    public static class RatePoint {
        public final long timestamp;
        public final long rateBytesPerSec;

        public RatePoint(long timestamp, long rateBytesPerSec) {
            this.timestamp = timestamp;
            this.rateBytesPerSec = rateBytesPerSec;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", timestamp);
            map.put("rateBytesPerSec", rateBytesPerSec);
            map.put("rateMBPerSec", rateBytesPerSec / (1024 * 1024));
            return map;
        }
    }
}
