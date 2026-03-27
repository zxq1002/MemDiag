# 使用支持 Attach 的 JDK 环境
FROM eclipse-temurin:17-jdk-focal

# 安装必要的系统工具
RUN apt-get update && apt-get install -y \
    g++ \
    cmake \
    make \
    procps \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 拷贝项目编译产物 (假设已经 mvn clean package)
COPY memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar /app/memdiag-cli.jar
COPY memdiag-agent/target/memdiag-agent-1.0.0-SNAPSHOT.jar /app/memdiag-agent.jar
# 注意：memdiag-native 编译出的 .so 文件应该在 classpath 下或由 Agent 加载逻辑指定

# 拷贝并编译 Demo
COPY demo/MemDiagDemo.java /app/
RUN javac MemDiagDemo.java

# 预设默认 JVM 参数（设置堆大小方便模拟溢出）
ENV JAVA_OPTS="-Xmx1G -Xms128M"

# 创建 memdiag 快捷命令
RUN echo '#!/bin/bash' > /usr/local/bin/memdiag && \
    echo 'java -jar /app/memdiag-cli.jar "$@"' >> /usr/local/bin/memdiag && \
    chmod +x /usr/local/bin/memdiag

# 启动脚本
COPY demo/run-scenario.sh /app/
RUN chmod +x /app/run-scenario.sh

ENTRYPOINT ["/app/run-scenario.sh"]
