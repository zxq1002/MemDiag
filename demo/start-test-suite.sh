#!/bin/bash
# 启动完整测试套件

set -e

echo "=========================================="
echo "MemDiag 完整测试套件启动器"
echo "=========================================="
echo ""

# 检查 Docker 是否可用
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 第一步：构建项目
echo "步骤 1: 构建项目..."
mvn clean package -DskipTests -q
echo "✅ 项目构建完成"
echo ""

# 第二步：构建 Docker 镜像
echo "步骤 2: 构建测试 Docker 镜像..."
docker build -t memdiag-test -f demo/Dockerfile.test .
echo "✅ Docker 镜像构建完成"
echo ""

# 第三步：启动容器并运行测试
echo "步骤 3: 启动测试容器并运行测试..."
echo ""

# 复制测试脚本到容器并执行
docker run --name memdiag-test-container --rm \
    --cap-add=SYS_PTRACE \
    -v "$(pwd)/demo/test-full-suite.sh:/app/test-full-suite.sh" \
    memdiag-test \
    bash -c "chmod +x /app/test-full-suite.sh && /app/test-full-suite.sh"
