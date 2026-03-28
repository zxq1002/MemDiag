#!/bin/bash
# Agent 模式专用测试脚本

set -e

echo "=========================================="
echo "MemDiag Agent 模式测试"
echo "=========================================="

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    exit 1
}

info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

# 步骤 1: 启动测试容器
info "启动测试容器..."
docker run -d --name memdiag-agent-test --platform linux/amd64 --cap-add=SYS_PTRACE memdiag-test tail -f /dev/null

sleep 3

# 步骤 2: 在容器内启动模拟器
info "在容器内启动模拟器..."
docker exec -d memdiag-agent-test java -Dmode=heap-high -Dlimit=100 MemDiagDemo

sleep 3

# 获取模拟器 PID
SIM_PID=$(docker exec memdiag-agent-test jps -l | grep MemDiagDemo | awk '{print $1}')
info "模拟器 PID: $SIM_PID"

if [ -z "$SIM_PID" ]; then
    fail "无法获取模拟器 PID"
fi

echo ""
echo "=========================================="
echo "测试 1: 基础功能验证 (ProcFS)"
echo "=========================================="

info "测试 native --status..."
docker exec memdiag-agent-test memdiag native --status $SIM_PID
pass "native --status 正常"

info "测试 native --summary..."
docker exec memdiag-agent-test memdiag native --summary $SIM_PID
pass "native --summary 正常"

echo ""
echo "=========================================="
echo "测试 2: Agent 模式动态 Attach"
echo "=========================================="

info "执行 agent attach..."
docker exec memdiag-agent-test memdiag native $SIM_PID --attach --agent-jar /app/memdiag-agent.jar

sleep 2

info "检查 agent 状态..."
docker exec memdiag-agent-test memdiag native $SIM_PID --status

pass "Agent attach 测试完成"

echo ""
echo "=========================================="
echo "测试 3: 通过 Agent 获取数据"
echo "=========================================="

info "通过 agent 获取 native summary..."
docker exec memdiag-agent-test memdiag native $SIM_PID --summary

pass "Agent 通信测试完成"

echo ""
echo "=========================================="
echo "测试 4: Agent detach"
echo "=========================================="

info "执行 agent detach..."
docker exec memdiag-agent-test memdiag native $SIM_PID --detach

sleep 1

pass "Agent detach 测试完成"

echo ""
echo "=========================================="
echo "测试 5: 验证基础功能仍然可用"
echo "=========================================="

info "再次测试 native --summary..."
docker exec memdiag-agent-test memdiag native --summary $SIM_PID
pass "Detach 后基础功能仍然正常"

echo ""
echo "=========================================="
echo "清理测试环境"
echo "=========================================="

docker stop memdiag-agent-test
docker rm memdiag-agent-test

echo ""
echo -e "${GREEN}=========================================="
echo "所有 Agent 模式测试通过!"
echo -e "==========================================${NC}"
