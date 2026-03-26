package com.memdiag.core.nmt;

import com.memdiag.core.exception.AnalysisException;
import com.memdiag.core.util.JmxClient;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 基于 JMX 的 NMT 分析器
 * 使用 DiagnosticCommandMBean 获取 NMT 数据
 */
public class JmxNmtAnalyzer {

    private final JmxClient jmxClient;
    private final NmtParser parser;

    public JmxNmtAnalyzer(JmxClient jmxClient) {
        this.jmxClient = jmxClient;
        this.parser = new NmtParser();
    }

    /**
     * 检查 NMT 是否启用
     */
    public boolean isNmtEnabled() {
        try {
            // 尝试获取 NMT 数据，如果失败则表示未启用
            getSummarySnapshot();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 NMT 概要快照
     */
    public NmtSnapshot getSummarySnapshot() {
        try {
            String output = executeDiagnosticCommand("VM.native_memory", "summary");
            return parser.parse(output);
        } catch (Exception e) {
            throw new AnalysisException("Failed to get NMT summary", e);
        }
    }

    /**
     * 获取 NMT 详细快照
     */
    public NmtSnapshot getDetailSnapshot() {
        try {
            String output = executeDiagnosticCommand("VM.native_memory", "detail");
            return parser.parseDetail(output);
        } catch (Exception e) {
            throw new AnalysisException("Failed to get NMT detail", e);
        }
    }

    /**
     * 获取 VM 选项，检查 NMT 配置
     */
    public String getNmtLevel() {
        try {
            List<String> args = getVmArguments();
            for (String arg : args) {
                if (arg.startsWith("-XX:NativeMemoryTracking=")) {
                    return arg.substring("-XX:NativeMemoryTracking=".length());
                }
            }
            return "off";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private List<String> getVmArguments() {
        List<String> args = new ArrayList<>();
        try {
            MBeanServerConnection connection = jmxClient.getConnection();
            ObjectName runtimeName = new ObjectName("java.lang:type=Runtime");
            @SuppressWarnings("unchecked")
            List<String> inputArgs = (List<String>) connection.getAttribute(runtimeName, "InputArguments");
            args.addAll(inputArgs);
        } catch (Exception e) {
            // 忽略错误，返回空列表
        }
        return args;
    }

    private String executeDiagnosticCommand(String command, String... args) throws Exception {
        MBeanServerConnection connection = jmxClient.getConnection();

        // 查找 DiagnosticCommandMBean
        ObjectName diagnosticName;
        try {
            diagnosticName = new ObjectName("com.sun.management:type=DiagnosticCommand");
        } catch (Exception e) {
            // 尝试备用名称
            Set<ObjectName> names = connection.queryNames(
                new ObjectName("com.sun.management:*"), null);
            if (names.isEmpty()) {
                throw new AnalysisException("DiagnosticCommandMBean not available");
            }
            diagnosticName = names.iterator().next();
        }

        // 调用诊断命令
        Object[] params;
        String[] signature;

        if (args.length > 0) {
            params = new Object[]{args};
            signature = new String[]{String[].class.getName()};
        } else {
            params = new Object[]{new String[0]};
            signature = new String[]{String[].class.getName()};
        }

        // 将命令名转换为 JMX 方法名（驼峰式）
        String methodName = toCamelCase(command);

        return (String) connection.invoke(diagnosticName, methodName, params, signature);
    }

    private String toCamelCase(String name) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
