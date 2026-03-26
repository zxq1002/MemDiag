#!/bin/bash

MODE=${1:-"heap-leak"}
LIMIT=${2:-"500"}
RATE=${3:-"10"}

echo "Starting scenario: $MODE"
echo "JVM Options: $JAVA_OPTS"

# 启动模拟器进程
java $JAVA_OPTS -Dmode=$MODE -Dlimit=$LIMIT -Drate=$RATE MemDiagDemo &
SIM_PID=$!

sleep 2

echo "Simulator is running with PID: $SIM_PID"
echo "You can now run MemDiag commands against this PID."
echo "Examples:"
echo "  java -jar memdiag-cli.jar histogram $SIM_PID"
echo "  java -jar memdiag-cli.jar native $SIM_PID --summary"
echo "  java -jar memdiag-cli.jar diagnose $SIM_PID"

# 保持前台运行输出模拟器日志
wait $SIM_PID
