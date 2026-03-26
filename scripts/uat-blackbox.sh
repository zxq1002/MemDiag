#!/bin/bash
set -e

echo "Starting LeakSimulator..."
java -cp classes LeakSimulator > /tmp/simulator.log 2>&1 &
SIM_PID=$!
sleep 5

echo "Scenario 1: Basic Heap Analysis"
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram $SIM_PID --limit=5

echo -e "\nScenario 2: Native Memory Attachment & Tracing"
# 1. 检查状态
echo "Checking native status..."
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native $SIM_PID --status

# 2. 挂载 Agent (新增加的功能)
echo "Attaching native agent..."
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native $SIM_PID --attach

# 3. 启动追踪 (新增加的功能)
echo "Starting allocation tracing..."
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native $SIM_PID --start-trace

# 4. 验证分配追踪结果
echo "Waiting for allocation tracking (5s)..."
sleep 5
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native $SIM_PID --allocation-sites --limit=5

echo -e "\nScenario 3: Detach and Cleanup"
# 5. 停止追踪
echo "Stopping allocation tracing..."
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native $SIM_PID --stop-trace

# 6. 卸载 Agent
echo "Detaching native agent..."
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native $SIM_PID --detach

# 7. 检查模拟器状态
if kill -0 $SIM_PID; then
    echo "SUCCESS: Simulator process is still alive after detach."
else
    echo "FAILURE: Simulator process crashed after detach!"
    exit 1
fi

kill $SIM_PID
echo "UAT Blackbox finished successfully."
