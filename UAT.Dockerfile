# 使用支持 Attach 的 JDK
FROM eclipse-temurin:17-jdk-focal

# 安装编译工具和必要的库
RUN apt-get update && apt-get install -y \
    g++ \
    cmake \
    make \
    procps \
    maven \
    && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /memdiag

# 将整个项目拷贝进容器（排除 .git）
COPY . .

# 编译项目（Java & Native）
RUN mvn clean package -DskipTests

# 编译测试模拟器
RUN javac tests/UAT/LeakSimulator.java -d classes

# 默认
CMD ["/bin/bash"]
