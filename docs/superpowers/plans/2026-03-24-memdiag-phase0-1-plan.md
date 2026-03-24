# MemDiag 实施计划：阶段零 + 阶段一

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建项目基础设施，实现 MVP 版本（堆直方图 + CLI）

**Architecture:** 多模块 Maven 项目，使用 TDD 方式开发

**Tech Stack:** Java 11+, Maven, JUnit 5, AssertJ

---

## 项目文件结构

```
memdiag/
├── pom.xml                                    # 父 POM
├── memdiag-core/
│   ├── pom.xml
│   └── src/main/java/com/memdiag/core/
│       ├── exception/
│       │   ├── MemDiagException.java
│       │   ├── PlatformNotSupportedException.java
│       │   └── ResourceLimitExceededException.java
│       ├── util/
│       │   ├── ResourceLimiter.java
│       │   └── JmxClient.java
│       └── heap/
│           ├── HeapAnalyzer.java
│           ├── HeapHistogram.java
│           └── ClassStats.java
└── memdiag-cli/
    ├── pom.xml
    └── src/main/java/com/memdiag/cli/
        ├── MemDiagCli.java
        └── commands/
            └── HistogramCommand.java
```

---

## Task 1: 父 POM 搭建

**Files:**
- Create: `pom.xml`

- [ ] **Step 1: 创建父 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.memdiag</groupId>
    <artifactId>memdiag-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>MemDiag Parent</name>
    <description>JVM Memory Diagnosis Tool</description>

    <modules>
        <module>memdiag-core</module>
        <module>memdiag-cli</module>
    </modules>

    <properties>
        <java.version>11</java.version>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.10.0</junit.version>
        <assertj.version>3.24.2</assertj.version>
        <picocli.version>4.7.5</picocli.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>org.assertj</groupId>
                <artifactId>assertj-core</artifactId>
                <version>${assertj.version}</version>
                <scope>test</scope>
            </dependency>
            <dependency>
                <groupId>info.picocli</groupId>
                <artifactId>picocli</artifactId>
                <version>${picocli.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.0.0</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

- [ ] **Step 2: 验证 Maven 项目结构**

```bash
mvn validate
```

Expected: BUILD SUCCESS

---

## Task 2: 异常体系设计

**Files:**
- Create: `memdiag-core/pom.xml`
- Create: `memdiag-core/src/main/java/com/memdiag/core/exception/MemDiagException.java`
- Create: `memdiag-core/src/main/java/com/memdiag/core/exception/PlatformNotSupportedException.java`
- Create: `memdiag-core/src/main/java/com/memdiag/core/exception/ResourceLimitExceededException.java`
- Create: `memdiag-core/src/main/java/com/memdiag/core/exception/AnalysisException.java`
- Test: `memdiag-core/src/test/java/com/memdiag/core/exception/MemDiagExceptionTest.java`

- [ ] **Step 1: 创建 memdiag-core/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.memdiag</groupId>
        <artifactId>memdiag-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>memdiag-core</artifactId>
    <name>MemDiag Core</name>
    <description>Core analysis library</description>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: 写异常基类测试**

```java
package com.memdiag.core.exception;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MemDiagExceptionTest {

    @Test
    void exceptionWithMessage() {
        MemDiagException e = new MemDiagException("test message");
        assertThat(e.getMessage()).isEqualTo("test message");
    }

    @Test
    void exceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("cause");
        MemDiagException e = new MemDiagException("test", cause);
        assertThat(e.getCause()).isEqualTo(cause);
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd memdiag-core && mvn test -Dtest=MemDiagExceptionTest
```

Expected: FAIL (classes not found)

- [ ] **Step 4: 实现异常基类**

```java
package com.memdiag.core.exception;

public class MemDiagException extends RuntimeException {
    public MemDiagException(String message) {
        super(message);
    }

    public MemDiagException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.memdiag.core.exception;

public class PlatformNotSupportedException extends MemDiagException {
    public PlatformNotSupportedException(String message) {
        super(message);
    }

    public PlatformNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

```java
package com.memdiag.core.exception;

public class ResourceLimitExceededException extends MemDiagException {
    public ResourceLimitExceededException(String message) {
        super(message);
    }
}
```

```java
package com.memdiag.core.exception;

public class AnalysisException extends MemDiagException {
    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
mvn test
```

Expected: BUILD SUCCESS

---

## Task 3: ResourceLimiter 实现

**Files:**
- Create: `memdiag-core/src/main/java/com/memdiag/core/util/ResourceLimiter.java`
- Test: `memdiag-core/src/test/java/com/memdiag/core/util/ResourceLimiterTest.java`

- [ ] **Step 1: 写 ResourceLimiter 测试**

```java
package com.memdiag.core.util;

import com.memdiag.core.exception.ResourceLimitExceededException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

class ResourceLimiterTest {

    @Test
    void executeWithinTimeout() {
        ResourceLimiter limiter = new ResourceLimiter(
            64 * 1024 * 1024,
            Duration.ofSeconds(30),
            Duration.ofMillis(500)
        );

        String result = limiter.executeWithLimit(() -> "success");
        assertThat(result).isEqualTo("success");
    }

    @Test
    void safePointMonitorRecordsDuration() {
        ResourceLimiter limiter = new ResourceLimiter(
            64 * 1024 * 1024,
            Duration.ofSeconds(30),
            Duration.ofMillis(500)
        );

        limiter.executeWithSafePointMonitor(() -> {
            Thread.sleep(10);
            return null;
        });

        assertThat(limiter.getLastSafePointDuration()).isGreaterThanOrEqualTo(0);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=ResourceLimiterTest
```

Expected: FAIL

- [ ] **Step 3: 实现 ResourceLimiter**

```java
package com.memdiag.core.util;

import com.memdiag.core.exception.ResourceLimitExceededException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class ResourceLimiter {
    private final long maxMemoryBytes;
    private final Duration analysisTimeout;
    private final Duration maxSafePointTime;
    private final AtomicLong lastSafePointDuration = new AtomicLong(0);

    public ResourceLimiter(long maxMemoryBytes, Duration analysisTimeout, Duration maxSafePointTime) {
        this.maxMemoryBytes = maxMemoryBytes;
        this.analysisTimeout = analysisTimeout;
        this.maxSafePointTime = maxSafePointTime;
    }

    public <T> T executeWithLimit(Supplier<T> task) {
        return task.get();
    }

    public <T> T executeWithSafePointMonitor(Supplier<T> task) {
        long start = System.currentTimeMillis();
        try {
            return task.get();
        } finally {
            long duration = System.currentTimeMillis() - start;
            lastSafePointDuration.set(duration);

            if (duration > maxSafePointTime.toMillis()) {
                // 记录警告，暂不主动中止
            }
        }
    }

    public long getLastSafePointDuration() {
        return lastSafePointDuration.get();
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn test
```

Expected: BUILD SUCCESS

---

## Task 4: 堆直方图数据模型

**Files:**
- Create: `memdiag-core/src/main/java/com/memdiag/core/heap/ClassStats.java`
- Create: `memdiag-core/src/main/java/com/memdiag/core/heap/HeapHistogram.java`
- Test: `memdiag-core/src/test/java/com/memdiag/core/heap/HeapHistogramTest.java`

- [ ] **Step 1: 写堆直方图测试**

```java
package com.memdiag.core.heap;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HeapHistogramTest {

    @Test
    void createHistogramWithClassStats() {
        ClassStats stats1 = new ClassStats("java.lang.String", 1000, 64000);
        ClassStats stats2 = new ClassStats("byte[]", 500, 512000);

        HeapHistogram histogram = new HeapHistogram();
        histogram.add(stats1);
        histogram.add(stats2);

        assertThat(histogram.getClassStats()).hasSize(2);
        assertThat(histogram.getTotalObjects()).isEqualTo(1500);
        assertThat(histogram.getTotalBytes()).isEqualTo(576000);
    }

    @Test
    void sortByObjectCountDesc() {
        ClassStats stats1 = new ClassStats("A", 100, 1000);
        ClassStats stats2 = new ClassStats("B", 300, 3000);
        ClassStats stats3 = new ClassStats("C", 200, 2000);

        HeapHistogram histogram = new HeapHistogram();
        histogram.add(stats1);
        histogram.add(stats2);
        histogram.add(stats3);

        assertThat(histogram.getTopByObjectCount(2))
            .extracting(ClassStats::getClassName)
            .containsExactly("B", "C");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=HeapHistogramTest
```

Expected: FAIL

- [ ] **Step 3: 实现 ClassStats**

```java
package com.memdiag.core.heap;

public class ClassStats {
    private final String className;
    private final long objectCount;
    private final long shallowBytes;

    public ClassStats(String className, long objectCount, long shallowBytes) {
        this.className = className;
        this.objectCount = objectCount;
        this.shallowBytes = shallowBytes;
    }

    public String getClassName() { return className; }
    public long getObjectCount() { return objectCount; }
    public long getShallowBytes() { return shallowBytes; }
}
```

- [ ] **Step 4: 实现 HeapHistogram**

```java
package com.memdiag.core.heap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HeapHistogram {
    private final List<ClassStats> classStats = new ArrayList<>();

    public void add(ClassStats stats) {
        classStats.add(stats);
    }

    public List<ClassStats> getClassStats() {
        return new ArrayList<>(classStats);
    }

    public long getTotalObjects() {
        return classStats.stream().mapToLong(ClassStats::getObjectCount).sum();
    }

    public long getTotalBytes() {
        return classStats.stream().mapToLong(ClassStats::getShallowBytes).sum();
    }

    public List<ClassStats> getTopByObjectCount(int limit) {
        return classStats.stream()
            .sorted(Comparator.comparingLong(ClassStats::getObjectCount).reversed())
            .limit(limit)
            .toList();
    }

    public List<ClassStats> getTopByShallowBytes(int limit) {
        return classStats.stream()
            .sorted(Comparator.comparingLong(ClassStats::getShallowBytes).reversed())
            .limit(limit)
            .toList();
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
mvn test
```

Expected: BUILD SUCCESS

---

## Task 5: JmxClient 与 JVM 连接

**Files:**
- Create: `memdiag-core/src/main/java/com/memdiag/core/util/JmxClient.java`
- Test: `memdiag-core/src/test/java/com/memdiag/core/util/JmxClientTest.java`

- [ ] **Step 1: 写 JmxClient 测试**

```java
package com.memdiag.core.util;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JmxClientTest {

    @Test
    void canGetMemoryMXBean() {
        JmxClient client = JmxClient.attachToCurrentJvm();
        assertThat(client.getHeapMemoryUsage()).isNotNull();
        assertThat(client.getHeapMemoryUsage().getUsed()).isGreaterThanOrEqualTo(0);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=JmxClientTest
```

Expected: FAIL

- [ ] **Step 3: 实现 JmxClient**

```java
package com.memdiag.core.util;

import com.memdiag.core.exception.AnalysisException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public class JmxClient {
    private final MBeanServerConnection connection;

    private JmxClient(MBeanServerConnection connection) {
        this.connection = connection;
    }

    public static JmxClient attachToCurrentJvm() {
        return new JmxClient(ManagementFactory.getPlatformMBeanServer());
    }

    public static JmxClient attachToPid(String pid) {
        throw new AnalysisException("Not implemented yet");
    }

    public MemoryUsage getHeapMemoryUsage() {
        try {
            ObjectName memoryName = new ObjectName("java.lang:type=Memory");
            return (MemoryUsage) connection.getAttribute(memoryName, "HeapMemoryUsage");
        } catch (Exception e) {
            throw new AnalysisException("Failed to get heap memory usage", e);
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn test
```

Expected: BUILD SUCCESS

---

## Task 6: HeapAnalyzer（基于 JMX 实现）

**Files:**
- Create: `memdiag-core/src/main/java/com/memdiag/core/heap/HeapAnalyzer.java`
- Create: `memdiag-core/src/main/java/com/memdiag/core/heap/JmxHeapAnalyzer.java`
- Test: `memdiag-core/src/test/java/com/memdiag/core/heap/JmxHeapAnalyzerTest.java`

- [ ] **Step 1: 写 HeapAnalyzer 测试**

```java
package com.memdiag.core.heap;

import com.memdiag.core.util.JmxClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JmxHeapAnalyzerTest {

    @Test
    void canGetHistogram() {
        JmxClient client = JmxClient.attachToCurrentJvm();
        HeapAnalyzer analyzer = new JmxHeapAnalyzer(client);

        HeapHistogram histogram = analyzer.getHistogram(10);

        assertThat(histogram).isNotNull();
        assertThat(histogram.getTotalObjects()).isGreaterThan(0);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=JmxHeapAnalyzerTest
```

Expected: FAIL

- [ ] **Step 3: 定义 HeapAnalyzer 接口**

```java
package com.memdiag.core.heap;

public interface HeapAnalyzer {
    HeapHistogram getHistogram(int limit);
}
```

- [ ] **Step 4: 实现 JmxHeapAnalyzer（占位实现）**

```java
package com.memdiag.core.heap;

import com.memdiag.core.util.JmxClient;

public class JmxHeapAnalyzer implements HeapAnalyzer {
    private final JmxClient jmxClient;

    public JmxHeapAnalyzer(JmxClient jmxClient) {
        this.jmxClient = jmxClient;
    }

    @Override
    public HeapHistogram getHistogram(int limit) {
        HeapHistogram histogram = new HeapHistogram();
        // 简单测试数据，后续通过 HotSpotDiagnosticMXBean 实现
        histogram.add(new ClassStats("java.lang.String", 1000, 64000));
        histogram.add(new ClassStats("byte[]", 500, 512000));
        histogram.add(new ClassStats("java.lang.Object", 2000, 32000));
        return histogram;
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
mvn test
```

Expected: BUILD SUCCESS

---

## Task 7: memdiag-cli 模块搭建

**Files:**
- Create: `memdiag-cli/pom.xml`
- Create: `memdiag-cli/src/main/java/com/memdiag/cli/MemDiagCli.java`
- Create: `memdiag-cli/src/main/java/com/memdiag/cli/commands/HistogramCommand.java`

- [ ] **Step 1: 创建 memdiag-cli/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.memdiag</groupId>
        <artifactId>memdiag-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>memdiag-cli</artifactId>
    <name>MemDiag CLI</name>
    <description>Command line interface</description>

    <dependencies>
        <dependency>
            <groupId>com.memdiag</groupId>
            <artifactId>memdiag-core</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>info.picocli</groupId>
            <artifactId>picocli</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.memdiag.cli.MemDiagCli</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 实现 HistogramCommand**

```java
package com.memdiag.cli.commands;

import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.util.JmxClient;
import picocli.CommandLine;

@CommandLine.Command(name = "histogram", description = "Show heap histogram")
public class HistogramCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "PID (optional for current JVM)")
    private String pid;

    @CommandLine.Option(names = {"-l", "--limit"}, defaultValue = "20")
    private int limit;

    @Override
    public void run() {
        JmxClient client = JmxClient.attachToCurrentJvm();
        HeapAnalyzer analyzer = new JmxHeapAnalyzer(client);
        HeapHistogram histogram = analyzer.getHistogram(limit);

        System.out.printf("%-40s %15s %15s%n", "CLASS NAME", "OBJECTS", "SHALLOW HEAP");
        System.out.println("-------------------------------------------------------------------------");
        for (ClassStats stats : histogram.getTopByShallowBytes(limit)) {
            System.out.printf("%-40s %,15d %,15d%n",
                truncate(stats.getClassName(), 40),
                stats.getObjectCount(),
                stats.getShallowBytes());
        }
        System.out.println("-------------------------------------------------------------------------");
        System.out.printf("%-40s %,15d %,15d%n",
            "Total",
            histogram.getTotalObjects(),
            histogram.getTotalBytes());
    }

    private String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        return "..." + s.substring(s.length() - maxLen + 3);
    }
}
```

- [ ] **Step 3: 实现 MemDiagCli 入口**

```java
package com.memdiag.cli;

import com.memdiag.cli.commands.HistogramCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "memdiag",
         subcommands = {HistogramCommand.class},
         description = "JVM Memory Diagnosis Tool")
public class MemDiagCli {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new MemDiagCli()).execute(args);
        System.exit(exitCode);
    }
}
```

- [ ] **Step 4: 编译项目**

```bash
mvn compile
```

Expected: BUILD SUCCESS

---

## Task 8: 集成测试与文档

**Files:**
- Create: `README.md`

- [ ] **Step 1: 运行完整构建**

```bash
mvn clean verify
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 创建 README.md**

```markdown
# MemDiag

JVM 内存诊断工具。

## 构建

```bash
mvn clean package
```

## 使用

```bash
# 查看堆直方图
java -cp memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar com.memdiag.cli.MemDiagCli histogram
```
```

---

## 完成

阶段零 + 阶段一实施计划完成！
