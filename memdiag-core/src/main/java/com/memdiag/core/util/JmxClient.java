package com.memdiag.core.util;

import com.memdiag.core.exception.AnalysisException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class JmxClient {
    private final MemoryMXBean memoryMXBean;

    private JmxClient(MemoryMXBean memoryMXBean) {
        this.memoryMXBean = memoryMXBean;
    }

    public static JmxClient attachToCurrentJvm() {
        return new JmxClient(ManagementFactory.getMemoryMXBean());
    }

    public static JmxClient attachToPid(String pid) {
        throw new AnalysisException("Not implemented yet");
    }

    public MemoryUsage getHeapMemoryUsage() {
        return memoryMXBean.getHeapMemoryUsage();
    }
}
