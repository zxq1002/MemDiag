#!/bin/bash
# MemDiag 完整测试套件
# 在 Docker 容器内运行

set -e

echo "=========================================="
echo "MemDiag 完整测试套件"
echo "=========================================="
echo ""

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
FAILED_CASES=()

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
run_test() {
    local test_name="$1"
    local test_command="$2"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${YELLOW}[TEST ${TOTAL_TESTS}]${NC} ${test_name}"
    echo "  Command: ${test_command}"

    if eval "${test_command}" > /tmp/test_output.txt 2>&1; then
        echo -e "  ${GREEN}[PASS]${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "  ${RED}[FAIL]${NC}"
        echo "  Output:"
        sed 's/^/    /' /tmp/test_output.txt
        FAILED_TESTS=$((FAILED_TESTS + 1))
        FAILED_CASES+=("${test_name}")
    fi
    echo ""
}

# 检查输出包含特定内容
test_contains() {
    local test_name="$1"
    local test_command="$2"
    local expected_content="$3"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo -e "${YELLOW}[TEST ${TOTAL_TESTS}]${NC} ${test_name}"
    echo "  Command: ${test_command}"
    echo "  Expect: ${expected_content}"

    if eval "${test_command}" > /tmp/test_output.txt 2>&1; then
        if grep -q "${expected_content}" /tmp/test_output.txt; then
            echo -e "  ${GREEN}[PASS]${NC}"
            PASSED_TESTS=$((PASSED_TESTS + 1))
        else
            echo -e "  ${RED}[FAIL - Content not found]${NC}"
            echo "  Output:"
            sed 's/^/    /' /tmp/test_output.txt
            FAILED_TESTS=$((FAILED_TESTS + 1))
            FAILED_CASES+=("${test_name}")
        fi
    else
        echo -e "  ${RED}[FAIL - Command failed]${NC}"
        echo "  Output:"
        sed 's/^/    /' /tmp/test_output.txt
        FAILED_TESTS=$((FAILED_TESTS + 1))
        FAILED_CASES+=("${test_name}")
    fi
    echo ""
}

# 启动模拟器
echo "步骤 1: 启动模拟器程序"
echo "----------------------------------------"
java $JAVA_OPTS -Dmode=heap-high -Dlimit=100 MemDiagDemo &
SIM_PID=$!
sleep 3
echo "模拟器 PID: ${SIM_PID}"
echo ""

# 确保 memdiag 命令可用 (使用 shaded jar)
alias memdiag='java -jar /app/memdiag-cli.jar'

echo "步骤 2: 开始执行测试"
echo "=========================================="
echo ""

# ========== P0: 基础命令测试 ==========
echo "【P0 优先级】基础命令测试"
echo "----------------------------------------"

run_test "主命令帮助" "memdiag -h"
run_test "主命令版本" "memdiag -V"

# ========== P0: histogram 命令测试 ==========
echo ""
echo "【P0 优先级】histogram 命令测试"
echo "----------------------------------------"

test_contains "histogram 默认输出" "memdiag histogram ${SIM_PID}" "CLASS NAME"
test_contains "histogram 限制行数" "memdiag histogram -l 5 ${SIM_PID}" "Total"
test_contains "histogram --limit 选项" "memdiag histogram --limit 10 ${SIM_PID}" "OBJECTS"


# ========== P0: --pid 选项测试 ==========
echo ""
echo "【P0 优先级】--pid 选项测试"
echo "----------------------------------------"

test_contains "histogram --pid 选项" "memdiag histogram --pid ${SIM_PID}" "CLASS NAME"
test_contains "threads --pid 选项" "memdiag threads --pid ${SIM_PID}" "THREAD ANALYSIS"
test_contains "diagnose --pid 选项" "memdiag diagnose --pid ${SIM_PID}" "DIAGNOSIS REPORT"
test_contains "native --status --pid 选项" "memdiag native --status --pid ${SIM_PID}" "NATIVE MEMORY"
test_contains "native --summary --pid 选项" "memdiag native --summary --pid ${SIM_PID}" "Total Virtual"

# ========== P0: threads 命令测试 ==========
echo ""
echo "【P0 优先级】threads 命令测试"
echo "----------------------------------------"

test_contains "threads 默认输出" "memdiag threads ${SIM_PID}" "THREAD ANALYSIS"
test_contains "threads --stacks" "memdiag threads -s ${SIM_PID}" "Stack:"
test_contains "threads --limit" "memdiag threads -l 5 ${SIM_PID}" "Total"

# ========== P0: diagnose 命令测试 ==========
echo ""
echo "【P0 优先级】diagnose 命令测试"
echo "----------------------------------------"

test_contains "diagnose 基本功能" "memdiag diagnose ${SIM_PID}" "DIAGNOSIS REPORT"
test_contains "diagnose 规则执行统计" "memdiag diagnose ${SIM_PID}" "Rules executed:"

# ========== P0: nmt 命令测试 ==========
echo ""
echo "【P0 优先级】nmt 命令测试"
echo "----------------------------------------"

test_contains "nmt 命令帮助" "memdiag nmt -h" "Native Memory Tracking"

# ========== P0: gc-roots 命令测试 ==========
echo ""
echo "【P0 优先级】gc-roots 命令测试"
echo "----------------------------------------"

test_contains "gc-roots 命令帮助" "memdiag gc-roots -h" "GC Root analysis"

# ========== P0: snapshot 命令测试 ==========
echo ""
echo "【P0 优先级】snapshot 命令测试"
echo "----------------------------------------"

run_test "snapshot --save" "memdiag snapshot --save --id=test-suite ${SIM_PID}"
test_contains "snapshot --list" "memdiag snapshot --list" "test-suite"
test_contains "snapshot --load" "memdiag snapshot --load=test-suite" "SNAPSHOT DETAILS"

# ========== P0: diff 命令测试 ==========
echo ""
echo "【P0 优先级】diff 命令测试"
echo "----------------------------------------"

# 再保存一个快照用于对比
run_test "保存第二个快照" "memdiag snapshot --save --id=test-suite-2 ${SIM_PID}"
test_contains "diff --baseline" "memdiag diff --baseline=test-suite --current=test-suite-2" "HEAP DIFF ANALYSIS"
test_contains "diff 实时对比" "memdiag diff --baseline=test-suite ${SIM_PID}" "Baseline:"

# ========== P1: native 命令测试 (基础) ==========
echo ""
echo "【P1 优先级】native 命令测试"
echo "----------------------------------------"

test_contains "native --status" "memdiag native --status ${SIM_PID}" "NATIVE MEMORY"
test_contains "native --summary" "memdiag native --summary ${SIM_PID}" "Total Virtual"
test_contains "native --regions" "memdiag native --regions ${SIM_PID}" "MEMORY REGIONS"
test_contains "native --diagnose" "memdiag native --diagnose ${SIM_PID}" "NATIVE MEMORY DIAGNOSIS"

# ========== P1: 多架构原生库支持测试 ==========
echo ""
echo "【P1 优先级】多架构原生库支持测试"
echo "----------------------------------------"

# 检查架构信息
test_contains "系统架构检测" "java -XshowSettings:properties -version 2>&1 | grep os.arch" "os.arch"

# 检查 JAR 包中是否包含多架构库文件
run_test "检查 x86_64 库文件存在" "jar tf /app/memdiag-cli.jar | grep -E 'libmemdiag-agent(-x86_64|-amd64)?\.so'"
test_contains "检查通用库文件存在" "jar tf /app/memdiag-cli.jar" "libmemdiag-agent.so"

# 测试 NativeLoader 类是否可用
cat > /tmp/TestNativeLoader.java << 'EOF'
import com.memdiag.nativeimpl.NativeLoader;

public class TestNativeLoader {
    public static void main(String[] args) {
        System.out.println("NativeLoader class loaded successfully");
        System.out.println("isLoaded(): " + NativeLoader.isLoaded());
        System.out.println("os.arch: " + System.getProperty("os.arch"));
        System.out.println("os.name: " + System.getProperty("os.name"));
    }
}
EOF
javac -cp /app/memdiag-cli.jar /tmp/TestNativeLoader.java
test_contains "NativeLoader 类加载测试" "java -cp /tmp:/app/memdiag-cli.jar TestNativeLoader" "NativeLoader class loaded successfully"

# ========== P1: report 命令测试 ==========
echo ""
echo "【P1 优先级】report 命令测试"
echo "----------------------------------------"

run_test "report 文本格式" "memdiag report ${SIM_PID}"
run_test "report HTML 格式" "memdiag report --format=html --output=/tmp/report.html ${SIM_PID}"
test_contains "report HTML 文件生成" "ls -la /tmp/report.html" "report.html"
run_test "report JSON 格式" "memdiag report --format=json --output=/tmp/report.json ${SIM_PID}"

# ========== 清理快照 ==========
echo ""
echo "清理测试数据"
echo "----------------------------------------"
run_test "删除测试快照" "memdiag snapshot --delete=test-suite"
run_test "删除测试快照2" "memdiag snapshot --delete=test-suite-2"

# ========== 停止模拟器 ==========
echo ""
echo "停止模拟器"
kill ${SIM_PID} 2>/dev/null || true
wait ${SIM_PID} 2>/dev/null || true

# ========== P1: Enhanced Agent 功能测试 ==========
echo ""
echo "【P1 优先级】Enhanced Agent 功能测试"
echo "=========================================="
echo ""

echo "步骤 A: 启动带 Agent 的 Demo 应用"
echo "----------------------------------------"
java -javaagent:/app/memdiag-agent.jar=port=6789 -Dmode=heap-high -Dlimit=100 MemDiagDemo &
AGENT_SIM_PID=$!
sleep 5
echo "带 Agent 的模拟器 PID: ${AGENT_SIM_PID}"
echo ""

# ========== Phase 1: Agent 基础设施测试 ==========
echo ""
echo "Phase 1: Agent 基础设施测试"
echo "----------------------------------------"

test_contains "agent status 命令" "memdiag agent status" "AGENT STATUS"
test_contains "agent config 命令" "memdiag agent config" "AGENT CONFIGURATION"
test_contains "agent metrics 命令" "memdiag agent metrics" "AGENT METRICS"
test_contains "agent 命令帮助" "memdiag agent -h" "Interact with a running MemDiag agent"

# ========== Phase 2: 数据采集层测试 ==========
echo ""
echo "Phase 2: 数据采集层测试"
echo "----------------------------------------"

test_contains "agent allocations --summary" "memdiag agent allocations --summary" "ALLOCATION ANALYSIS"
test_contains "agent allocations --stats" "memdiag agent allocations --stats" "ALLOCATION ANALYSIS"
test_contains "agent allocations --top" "memdiag agent allocations --top" "ALLOCATION ANALYSIS"
test_contains "agent allocations --rate" "memdiag agent allocations --rate" "ALLOCATION ANALYSIS"
test_contains "agent methods --stats" "memdiag agent methods --stats" "METHOD ANALYSIS"

# ========== Phase 3: 仪器控制测试 ==========
echo ""
echo "Phase 3: 仪器控制测试"
echo "----------------------------------------"

test_contains "agent enable 命令帮助" "memdiag agent enable -h" "Enable instrumentation features"
test_contains "agent disable 命令帮助" "memdiag agent disable -h" "Disable instrumentation features"

# ========== Phase 4: 新增顶层命令测试 ==========
echo ""
echo "Phase 4: 新增顶层命令测试"
echo "----------------------------------------"

test_contains "allocations 命令帮助" "memdiag allocations -h" "allocations"
test_contains "methods 命令帮助" "memdiag methods -h" "methods"

# 测试 allocations 命令 (使用 agent 模式)
test_contains "allocations --agent 选项" "memdiag allocations --agent=localhost:6789" "Allocation Summary" 2>/dev/null || true
test_contains "methods --agent 选项" "memdiag methods --agent=localhost:6789" "Method Monitoring" 2>/dev/null || true

# ========== Phase 5: JVMTI 集成测试 ==========
echo ""
echo "Phase 5: JVMTI 集成测试"
echo "----------------------------------------"

test_contains "agent jvmti 命令" "memdiag agent jvmti" "JVMTI STATUS"

# ========== 停止带 Agent 的模拟器 ==========
echo ""
echo "停止带 Agent 的模拟器"
kill ${AGENT_SIM_PID} 2>/dev/null || true
wait ${AGENT_SIM_PID} 2>/dev/null || true

# ========== 测试总结 ==========
echo ""
echo "=========================================="
echo "测试总结"
echo "=========================================="
echo "总测试数: ${TOTAL_TESTS}"
echo -e "通过:     ${GREEN}${PASSED_TESTS}${NC}"
echo -e "失败:     ${RED}${FAILED_TESTS}${NC}"
echo ""

if [ ${FAILED_TESTS} -gt 0 ]; then
    echo "失败的测试用例:"
    for case in "${FAILED_CASES[@]}"; do
        echo -e "  ${RED}- ${case}${NC}"
    done
    echo ""
    exit 1
else
    echo -e "${GREEN}所有测试通过!${NC}"
    echo ""
    exit 0
fi
