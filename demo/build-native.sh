#!/bin/bash
# Build libmemdiag-agent.so using Docker

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

echo "========================================"
echo "Building libmemdiag-agent.so"
echo "========================================"

# Build Docker image
echo "Building Docker image..."
docker build -f demo/Dockerfile.native-build -t memdiag-native-builder .

# Run Docker container to build
echo "Running build in Docker..."
docker run --rm \
    -v "$PROJECT_ROOT/target:/build/target" \
    -v "$PROJECT_ROOT/memdiag-native/target:/build/memdiag-native/target" \
    memdiag-native-builder \
    mvn clean install -DskipTests -Dcompile.native=true

# Check if the library was built
LIBRARY_PATH="$PROJECT_ROOT/memdiag-native/target/native/libmemdiag-agent.so"
if [ -f "$LIBRARY_PATH" ]; then
    echo "========================================"
    echo "✅ Build successful!"
    echo "Library: $LIBRARY_PATH"
    echo "========================================"

    # Copy to resources directory so it can be loaded from classpath
    RESOURCES_DIR="$PROJECT_ROOT/memdiag-native/src/main/resources"
    mkdir -p "$RESOURCES_DIR"
    cp "$LIBRARY_PATH" "$RESOURCES_DIR/"
    echo "Copied to: $RESOURCES_DIR/libmemdiag-agent.so"

    # Also copy to memdiag-cli resources for convenience
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
