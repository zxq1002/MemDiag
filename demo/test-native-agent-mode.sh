#!/bin/bash
# 测试 Agent 模式下 native 命令的脚本

set -e

echo "=========================================="
echo "MemDiag Agent 模式 Native 命令测试"
echo "=========================================="
echo ""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

TOTAL_TESTS=0
PASSED_TESTS=0

pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    PASSED_TESTS=$((PASSED_TESTS + 1))
}

fail() {
    echo -e "${RED}[FAIL]${NC} $1"
}

info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

test_native_agent() {
    local test_name="$1"
    local test_command="$2"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${YELLOW}[TEST ${TOTAL_TESTS}]${NC} ${test_name}"
    echo "  Command: ${test_command}"

    if eval "${test_command}" > /tmp/test_output.txt 2>&1; then
        echo "  Output:"
        sed 's/^/    /' /tmp/test_output.txt
        echo ""
        pass "${test_name}"
    else
        echo "  Output:"
        sed 's/^/    /' /tmp/test_output.txt
        echo ""
        fail "${test_name}"
    fi
}

# 步骤 1: 启动带 Agent 的 Demo
echo "步骤 1: 启动带 Agent 的 Demo 应用"
echo "----------------------------------------"
java -javaagent:/app/memdiag-agent.jar=port=6789 -Dmode=heap-high -Dlimit=100 MemDiagDemo &
AGENT_PID=$!
sleep 3
echo "带 Agent 的 Demo PID: ${AGENT_PID}"
echo ""

# 等待 Agent 启动
sleep 2

# 步骤 2: 使用 --agent 选项测试 native 命令
echo "步骤 2: 使用 --agent 选项测试 native 命令"
echo "----------------------------------------"

test_native_agent "native --status with --agent" "memdiag native --status --agent=localhost:6789"
test_native_agent "native --summary with --agent" "memdiag native --summary --agent=localhost:6789"
test_native_agent "native --regions with --agent" "memdiag native --regions --agent=localhost:6789"

# 步骤 3: 验证数据不是 0
echo ""
echo "步骤 3: 验证数据正确性"
echo "----------------------------------------"

echo "检查 native --summary 数据..."
memdiag native --summary --agent=localhost:6789 > /tmp/native_summary.txt

TOTAL_TESTS=$((TOTAL_TESTS + 1))
if grep -q "Total Virtual:.*0 bytes" /tmp/native_summary.txt; then
    echo -e "${YELLOW}[TEST ${TOTAL_TESTS}]${NC} 验证数据非零"
    fail "Total Virtual 仍然是 0"
else
    echo -e "${YELLOW}[TEST ${TOTAL_TESTS}]${NC} 验证数据非零"
    pass "Total Virtual 有数据"
fi

# 步骤 4: 测试其他命令通过 Agent
echo ""
echo "步骤 4: 测试其他命令通过 Agent"
echo "----------------------------------------"

test_native_agent "histogram via --agent" "memdiag histogram --agent=localhost:6789 -l 5"
test_native_agent "threads via --agent" "memdiag threads --agent=localhost:6789 -l 5"

# 清理
echo ""
echo "清理"
echo "----------------------------------------"
kill ${AGENT_PID} 2>/dev/null || true
wait ${AGENT_PID} 2>/dev/null || true

# 总结
echo ""
echo "=========================================="
echo "测试总结"
echo "=========================================="
echo "总测试数: ${TOTAL_TESTS}"
echo -e "通过:     ${GREEN}${PASSED_TESTS}${NC}"
echo -e "失败:     ${RED}$((TOTAL_TESTS - PASSED_TESTS))${NC}"
echo ""

if [ ${PASSED_TESTS} -eq ${TOTAL_TESTS} ]; then
    echo -e "${GREEN}所有测试通过!${NC}"
    echo ""
    exit 0
else
    echo -e "${RED}部分测试失败!${NC}"
    echo ""
    exit 1
fi
