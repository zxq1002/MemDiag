package com.memdiag.core.util;

import com.memdiag.core.exception.AnalysisException;
import com.memdiag.core.exception.PlatformNotSupportedException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.util.Properties;

public class JmxClient {
    private final MBeanServerConnection connection;
    private final MemoryMXBean memoryMXBean;
    private final ObjectName hotSpotDiagnosticName;

    private JmxClient(MBeanServerConnection connection, MemoryMXBean memoryMXBean) {
        this.connection = connection;
        this.memoryMXBean = memoryMXBean;
        try {
            this.hotSpotDiagnosticName = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
        } catch (Exception e) {
            throw new AnalysisException("Failed to create HotSpotDiagnostic ObjectName", e);
        }
    }

    public static JmxClient attachToCurrentJvm() {
        return new JmxClient(
            ManagementFactory.getPlatformMBeanServer(),
            ManagementFactory.getMemoryMXBean()
        );
    }

    public static JmxClient attachToPid(String pid) {
        try {
            // 尝试使用 Attach API
            String connectorAddress = getConnectorAddress(pid);
            JMXServiceURL url = new JMXServiceURL(connectorAddress);
            JMXConnector connector = JMXConnectorFactory.connect(url);
            MBeanServerConnection conn = connector.getMBeanServerConnection();

            // 获取 MemoryMXBean 代理
            MemoryMXBean memoryBean = ManagementFactory.newPlatformMXBeanProxy(
                conn, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class
            );

            return new JmxClient(conn, memoryBean);
        } catch (Exception e) {
            throw new AnalysisException("Failed to attach to PID " + pid, e);
        }
    }

    private static String getConnectorAddress(String pid) throws Exception {
        // 尝试加载 Attach API
        Class<?> virtualMachineClass;
        try {
            virtualMachineClass = Class.forName("com.sun.tools.attach.VirtualMachine");
        } catch (ClassNotFoundException e) {
            // 尝试从 tools.jar 加载
            String javaHome = System.getProperty("java.home");
            File toolsJar = new File(javaHome, "../lib/tools.jar");
            if (!toolsJar.exists()) {
                toolsJar = new File(javaHome, "lib/tools.jar");
            }
            if (!toolsJar.exists()) {
                throw new PlatformNotSupportedException("tools.jar not found. Please use a JDK, not a JRE.");
            }
            // 使用 URLClassLoader 加载（简化处理，实际项目中可能需要更复杂的类加载策略）
            throw new PlatformNotSupportedException("Attach API not available. Please run with JDK and add tools.jar to classpath.");
        }

        // 调用 VirtualMachine.attach(pid)
        Method attachMethod = virtualMachineClass.getMethod("attach", String.class);
        Object vm = attachMethod.invoke(null, pid);

        try {
            // 检查是否已有 JMX 代理
            Method getAgentPropertiesMethod = virtualMachineClass.getMethod("getAgentProperties");
            Properties agentProps = (Properties) getAgentPropertiesMethod.invoke(vm);
            String connectorAddress = agentProps.getProperty("com.sun.management.jmxremote.localConnectorAddress");

            if (connectorAddress != null) {
                return connectorAddress;
            }

            // 没有 JMX 代理，尝试加载管理代理
            Method startLocalManagementAgentMethod = virtualMachineClass.getMethod("startLocalManagementAgent");
            connectorAddress = (String) startLocalManagementAgentMethod.invoke(vm);
            if (connectorAddress != null) {
                return connectorAddress;
            }

            // 最后尝试再次获取
            agentProps = (Properties) getAgentPropertiesMethod.invoke(vm);
            connectorAddress = agentProps.getProperty("com.sun.management.jmxremote.localConnectorAddress");
            if (connectorAddress != null) {
                return connectorAddress;
            }

            throw new AnalysisException("Failed to start JMX management agent for PID " + pid);
        } finally {
            Method detachMethod = virtualMachineClass.getMethod("detach");
            detachMethod.invoke(vm);
        }
    }

    public MemoryUsage getHeapMemoryUsage() {
        return memoryMXBean.getHeapMemoryUsage();
    }

    public MBeanServerConnection getConnection() {
        return connection;
    }

    public void dumpHeap(String outputFile, boolean live) {
        try {
            connection.invoke(
                hotSpotDiagnosticName,
                "dumpHeap",
                new Object[]{outputFile, live},
                new String[]{"java.lang.String", "boolean"}
            );
        } catch (Exception e) {
            throw new AnalysisException("Failed to dump heap", e);
        }
    }

    public Object getDiagnosticOption(String name) {
        try {
            return connection.invoke(
                hotSpotDiagnosticName,
                "getVMOption",
                new Object[]{name},
                new String[]{"java.lang.String"}
            );
        } catch (Exception e) {
            throw new AnalysisException("Failed to get diagnostic option: " + name, e);
        }
    }
}
