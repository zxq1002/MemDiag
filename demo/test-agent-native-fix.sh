#!/bin/bash
# 简单测试：验证 Agent 模式下 native 命令修复

set -e

echo "=========================================="
echo "测试 Agent 模式 Native 命令修复"
echo "=========================================="
echo ""

# 启动带 Agent 的 Demo
echo "启动带 Agent 的 Demo..."
java -javaagent:/app/memdiag-agent.jar=port=6789 -Dmode=heap-high -Dlimit=100 MemDiagDemo &
AGENT_PID=$!
sleep 3
echo "Demo PID: ${AGENT_PID}"
echo ""

# 测试 1: ProcFS 模式（不使用 --agent）
echo "测试 1: ProcFS 模式（直接 PID）"
echo "----------------------------------------"
memdiag native --summary ${AGENT_PID}
echo ""

# 测试 2: Agent 模式（使用 --agent）
echo "测试 2: Agent 模式（使用 --agent）"
echo "----------------------------------------"
memdiag native --summary --agent=localhost:6789
echo ""

# 测试 3: 对比两者的数据
echo "测试 3: 验证 Agent 模式数据正确性"
echo "----------------------------------------"
memdiag native --summary --agent=localhost:6789 > /tmp/agent_summary.txt

echo "Agent 模式输出："
cat /tmp/agent_summary.txt
echo ""

if grep -q "Total Virtual:.*0 bytes" /tmp/agent_summary.txt; then
    echo "❌ FAIL: Agent 模式数据仍然是 0"
else
    echo "✅ PASS: Agent 模式数据正确"
fi
echo ""

# 清理
echo "清理..."
kill ${AGENT_PID} 2>/dev/null || true
wait ${AGENT_PID} 2>/dev/null || true

echo ""
echo "测试完成！"
