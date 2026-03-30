#!/bin/bash
# 启动完整测试套件
# 支持两种模式:
#   - 默认模式: 快速测试，不构建原生库
#   - 完整模式: --full，构建原生库并执行完整测试

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "=========================================="
echo "MemDiag 完整测试套件启动器"
echo "=========================================="
echo ""

# 检查 Docker 是否可用
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 解析命令行参数
FULL_MODE=false
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --full) FULL_MODE=true ;;
        *) echo "未知选项: $1"; exit 1 ;;
    esac
    shift
done

if [ "$FULL_MODE" = true ]; then
    echo "模式: 完整测试（包含原生库构建）"
else
    echo "模式: 快速测试（不构建原生库）"
fi
echo ""

# ========== 完整模式：构建原生库 ==========
if [ "$FULL_MODE" = true ]; then
    echo "========================================"
    echo "步骤 0: 构建 JVMTI 原生库"
    echo "========================================"
    echo ""

    if [ -f "$SCRIPT_DIR/build-final.sh" ]; then
        bash "$SCRIPT_DIR/build-final.sh"

        if [ ! -f "$PROJECT_ROOT/memdiag-native/src/main/resources/libmemdiag-agent.so" ]; then
            echo "❌ 原生库构建失败"
            exit 1
        fi

        echo "✅ 原生库构建成功"
        ls -lh "$PROJECT_ROOT/memdiag-native/src/main/resources/"*.so
        echo ""

        echo "重新打包 memdiag-native 模块..."
        mvn install -pl memdiag-native -DskipTests -q

        echo "重新打包 memdiag-agent 模块..."
        mvn install -pl memdiag-agent -DskipTests -q
        echo ""
    else
        echo "⚠️  build-final.sh 不存在，跳过原生库构建"
        echo ""
    fi
fi

# ========== 第一步：构建项目 ==========
echo "步骤 1: 构建项目..."
mvn clean package -DskipTests -q
echo "✅ 项目构建完成"
echo ""

# ========== 第二步：构建 Docker 镜像 ==========
echo "步骤 2: 构建测试 Docker 镜像 (linux/amd64)..."
docker build --platform linux/amd64 -t memdiag-test -f demo/Dockerfile.test .
echo "✅ Docker 镜像构建完成"
echo ""

# ========== 第三步：启动容器并运行测试 ==========
echo "步骤 3: 启动测试容器并运行测试..."
echo ""

# 复制测试脚本到容器并执行
echo "启动容器: memdiag-demo (端口映射: 6789:6789)"
docker run --name memdiag-demo \
    --platform linux/amd64 \
    --cap-add=SYS_PTRACE \
    -p 6789:6789 \
    -v "$(pwd)/demo/test-full-suite.sh:/app/test-full-suite.sh" \
    -d memdiag-test \
    bash -c "chmod +x /app/test-full-suite.sh && /app/test-full-suite.sh"

echo "测试套件正在后台运行。你可以通过以下方式查看日志:"
echo "  docker logs -f memdiag-demo"
echo ""
echo "Agent 接口现已暴露在宿主机的 6789 端口，可用于 Web 模块验证。"

