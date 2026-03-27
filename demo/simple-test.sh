#!/bin/bash
# 简单测试脚本 - 逐个验证功能

echo "=========================================="
echo "MemDiag 功能验证"
echo "=========================================="
echo ""

PASSED=0
FAILED=0
TOTAL=0

test_pass() {
    echo -e "\033[32m[PASS]\033[0m $1"
    PASSED=$((PASSED + 1))
}

test_fail() {
    echo -e "\033[31m[FAIL]\033[0m $1"
    FAILED=$((FAILED + 1))
}

run_test() {
    local name="$1"
    local cmd="$2"
    TOTAL=$((TOTAL + 1))
    echo -n "测试 $TOTAL: $name ... "
    if eval "$cmd" > /tmp/testout 2>&1; then
        test_pass "$name"
    else
        test_fail "$name"
        echo "  命令: $cmd"
        echo "  输出:"
        sed 's/^/    /' /tmp/testout
    fi
}

test_contains() {
    local name="$1"
    local cmd="$2"
    local expected="$3"
    TOTAL=$((TOTAL + 1))
    echo -n "测试 $TOTAL: $name ... "
    if eval "$cmd" > /tmp/testout 2>&1; then
        if grep -q "$expected" /tmp/testout; then
            test_pass "$name"
        else
            test_fail "$name (期望内容未找到: '$expected')"
            echo "  输出:"
            sed 's/^/    /' /tmp/testout
        fi
    else
        test_fail "$name"
        echo "  命令: $cmd"
        echo "  输出:"
        sed 's/^/    /' /tmp/testout
    fi
}

echo "【1】基础命令测试"
echo "----------------------------------------"
test_contains "memdiag -h" "memdiag -h" "Usage:"
run_test "memdiag -V" "memdiag -V"
echo ""

echo "【2】启动模拟器"
echo "----------------------------------------"
java $JAVA_OPTS -Dmode=heap-high -Dlimit=50 MemDiagDemo &
SIM_PID=$!
sleep 2
echo "模拟器 PID: $SIM_PID"
echo ""

echo "【3】histogram 命令测试"
echo "----------------------------------------"
test_contains "histogram 默认" "memdiag histogram $SIM_PID" "CLASS NAME"
test_contains "histogram -l 5" "memdiag histogram -l 5 $SIM_PID" "OBJECTS"
echo ""

echo "【4】threads 命令测试"
echo "----------------------------------------"
test_contains "threads 默认" "memdiag threads $SIM_PID" "THREAD ANALYSIS"
test_contains "threads -s" "memdiag threads -s $SIM_PID" "Total:"
echo ""

echo "【5】diagnose 命令测试"
echo "----------------------------------------"
test_contains "diagnose 基本" "memdiag diagnose $SIM_PID" "DIAGNOSIS REPORT"
test_contains "diagnose 规则数" "memdiag diagnose $SIM_PID" "Rules executed: 5"
echo ""

echo "【6】snapshot 命令测试"
echo "----------------------------------------"
run_test "snapshot --save" "memdiag snapshot --save --id=test1 $SIM_PID"
test_contains "snapshot --list" "memdiag snapshot --list" "test1"
test_contains "snapshot --load" "memdiag snapshot --load=test1" "SNAPSHOT DETAILS"
echo ""

echo "【7】diff 命令测试"
echo "----------------------------------------"
run_test "保存第二个快照" "memdiag snapshot --save --id=test2 $SIM_PID"
test_contains "diff 两个快照" "memdiag diff --baseline=test1 --current=test2" "HEAP DIFF ANALYSIS"
echo ""

echo "【8】native 命令测试 (基础)"
echo "----------------------------------------"
test_contains "native --status" "memdiag native --status $SIM_PID" "NATIVE MEMORY"
test_contains "native --summary" "memdiag native --summary $SIM_PID" "Total Virtual"
echo ""

echo "【9】report 命令测试"
echo "----------------------------------------"
test_contains "report 文本" "memdiag report $SIM_PID" "内存诊断报告"
run_test "report HTML" "memdiag report --format=html --output=/tmp/report.html $SIM_PID"
test_contains "HTML 文件生成" "ls -la /tmp/report.html" "report.html"
echo ""

echo "【10】清理"
echo "----------------------------------------"
run_test "删除快照 test1" "memdiag snapshot --delete=test1"
run_test "删除快照 test2" "memdiag snapshot --delete=test2"
kill $SIM_PID 2>/dev/null
wait $SIM_PID 2>/dev/null
echo ""

echo "=========================================="
echo "验证总结"
echo "=========================================="
echo "总测试: $TOTAL"
echo -e "通过:   \033[32m$PASSED\033[0m"
echo -e "失败:   \033[31m$FAILED\033[0m"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "\033[32m✅ 所有功能验证通过！\033[0m"
    exit 0
else
    echo -e "\033[31m❌ 有 $FAILED 个测试失败\033[0m"
    exit 1
fi
