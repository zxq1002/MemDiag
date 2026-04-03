#!/bin/bash

# Default values
MODE=${1:-"heap-leak"}
LIMIT=${2:-"500"}
RATE=${3:-"10"}

# Print usage instructions if help requested
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
    echo "Usage: ./demo/start-uat.sh [mode] [limit_mb] [rate_mb_per_sec]"
    echo "Modes: heap-leak, heap-high, native-leak, native-high"
    echo "Example: ./demo/start-uat.sh native-leak 800 20"
    exit 0
fi

# Ensure project is compiled
echo "Step 1: Building project..."
mvn clean package -DskipTests -q

# Build Docker image (force amd64 for compatibility with precompiled .so files)
echo "Step 2: Building Docker image (linux/amd64)..."
docker build --platform linux/amd64 -t memdiag-uat -f demo/Dockerfile .

# Start container (force amd64 for compatibility with precompiled .so files)
echo "Step 3: Starting container in $MODE mode (Limit: ${LIMIT}MB, Rate: ${RATE}MB/s)..."
echo "----------------------------------------------------------"
echo "To interact with the container, run in a new terminal:"
echo "  docker exec -it memdiag-uat bash"
echo "----------------------------------------------------------"

docker run --name memdiag-uat --rm \
    --platform linux/amd64 \
    --cap-add=SYS_PTRACE \
    -p 6789:6789 \
    memdiag-uat "$MODE" "$LIMIT" "$RATE"
