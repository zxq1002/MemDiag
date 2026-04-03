package com.memdiag.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "memdiag")
public class MemDiagProperties {

    private int agentPort = 6789;
    private int realtimeRate = 5000;
    private int defaultHistogramLimit = 10;
    private int snapshotHistogramLimit = 1000;

    public int getAgentPort() {
        return agentPort;
    }

    public void setAgentPort(int agentPort) {
        this.agentPort = agentPort;
    }

    public int getRealtimeRate() {
        return realtimeRate;
    }

    public void setRealtimeRate(int realtimeRate) {
        this.realtimeRate = realtimeRate;
    }

    public int getDefaultHistogramLimit() {
        return defaultHistogramLimit;
    }

    public void setDefaultHistogramLimit(int defaultHistogramLimit) {
        this.defaultHistogramLimit = defaultHistogramLimit;
    }

    public int getSnapshotHistogramLimit() {
        return snapshotHistogramLimit;
    }

    public void setSnapshotHistogramLimit(int snapshotHistogramLimit) {
        this.snapshotHistogramLimit = snapshotHistogramLimit;
    }
}
