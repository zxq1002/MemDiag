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
