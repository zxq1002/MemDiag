#!/bin/bash
# Final build script with correct include paths

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "========================================"
echo "Final build with correct includes"
echo "========================================"

mkdir -p memdiag-native/target/native

docker run --rm \
    --platform linux/amd64 \
    -v "$PROJECT_ROOT:/workspace" \
    -w /workspace \
    gcc:10 \
    bash -c '
set -e

# Install JDK
apt-get update && apt-get install -y --no-install-recommends openjdk-11-jdk-headless

JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
echo "JAVA_HOME: $JAVA_HOME"

cd /workspace

# Compile with ALL include paths
g++ -std=c++17 -fPIC -shared \
    -I"$JAVA_HOME/include" \
    -I"$JAVA_HOME/include/linux" \
    -I"/workspace/memdiag-native/src/main/c" \
    -I"/workspace/memdiag-native/src/main/c/jvmti" \
    -I"/workspace/memdiag-native/src/main/c/shared" \
    -I"/workspace/memdiag-native/src/main/c/linux" \
    -o memdiag-native/target/native/libmemdiag-agent.so \
    memdiag-native/src/main/c/jvmti/agent.cpp \
    memdiag-native/src/main/c/jvmti/class_transformer.cpp \
    memdiag-native/src/main/c/jvmti/allocation_tracker.cpp \
    memdiag-native/src/main/c/linux/proc_parser.cpp \
    memdiag-native/src/main/c/shared/symbol_cache.cpp \
    -lpthread -ldl

echo "Done!"
ls -lh memdiag-native/target/native/
'

# Check result
LIBRARY_PATH="$PROJECT_ROOT/memdiag-native/target/native/libmemdiag-agent.so"
if [ -f "$LIBRARY_PATH" ]; then
    echo "========================================"
    echo "✅ Build successful!"
    echo "Library: $LIBRARY_PATH"
    echo "========================================"

    # Copy to resources
    RESOURCES_DIR="$PROJECT_ROOT/memdiag-native/src/main/resources"
    mkdir -p "$RESOURCES_DIR"
    cp "$LIBRARY_PATH" "$RESOURCES_DIR/"
    echo "Copied to: $RESOURCES_DIR/libmemdiag-agent.so"

    CLI_RESOURCES_DIR="$PROJECT_ROOT/memdiag-cli/src/main/resources"
    mkdir -p "$CLI_RESOURCES_DIR"
    cp "$LIBRARY_PATH" "$CLI_RESOURCES_DIR/"
    echo "Copied to: $CLI_RESOURCES_DIR/libmemdiag-agent.so"

    ls -lh "$RESOURCES_DIR/"
else
    echo "========================================"
    echo "❌ Build failed: Library not found at $LIBRARY_PATH"
    echo "========================================"
    exit 1
fi
