# MemDiag

JVM 内存诊断工具。

## 构建

```bash
mvn clean package
```

## 使用

```bash
# 查看当前 JVM 的堆直方图
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram

# 查看堆直方图（限制显示前 20 行）
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram -l 20

# 查看指定 PID 的 JVM 堆直方图（需要 JDK，目标 JVM 需要启用 JMX）
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram <pid>
```
