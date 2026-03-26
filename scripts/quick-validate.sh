#!/bin/bash
# Quick validation script for MemDiag
# Performs fast checks without Docker

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "=========================================="
echo "MemDiag Quick Validation"
echo "=========================================="

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

print_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

FAILED=0

# Check 1: Build the project
echo ""
echo "[1/4] Building project..."
if mvn clean compile -DskipTests -q; then
    print_success "Project compiled"
else
    print_error "Failed to compile"
    FAILED=1
fi

# Check 2: Run unit tests
echo ""
echo "[2/4] Running unit tests..."
if mvn test -pl memdiag-core -q; then
    print_success "Unit tests passed"
else
    print_error "Unit tests failed"
    FAILED=1
fi

# Check 3: Verify new files exist
echo ""
echo "[3/4] Verifying new files..."

CHECK_FILES=(
    "memdiag-core/src/main/java/com/memdiag/core/config/MemDiagConfig.java"
    "memdiag-core/src/main/java/com/memdiag/core/util/EnvironmentPrecheck.java"
    "memdiag-core/src/test/java/com/memdiag/core/util/ResourceLimiterDestructiveTest.java"
    "memdiag-core/src/main/resources/memdiag.properties"
    "scripts/native-smoke-test.sh"
)

for file in "${CHECK_FILES[@]}"; do
    if [ -f "$file" ]; then
        print_success "Found: $file"
    else
        print_error "Missing: $file"
        FAILED=1
    fi
done

# Check 4: Verify ResourceLimiter is used in JmxHeapAnalyzer
echo ""
echo "[4/4] Verifying JmxHeapAnalyzer changes..."
if grep -q "ResourceLimiter" memdiag-core/src/main/java/com/memdiag/core/heap/JmxHeapAnalyzer.java; then
    print_success "JmxHeapAnalyzer uses ResourceLimiter"
else
    print_error "JmxHeapAnalyzer missing ResourceLimiter"
    FAILED=1
fi

if ! grep -q "getFallbackHistogram" memdiag-core/src/main/java/com/memdiag/core/heap/JmxHeapAnalyzer.java; then
    print_success "getFallbackHistogram removed"
else
    print_error "getFallbackHistogram still present"
    FAILED=1
fi

# Summary
echo ""
echo "=========================================="
if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}All checks passed!${NC}"
    echo ""
    echo "To run full smoke tests with Docker:"
    echo "  ./scripts/native-smoke-test.sh"
    exit 0
else
    echo -e "${RED}Some checks failed!${NC}"
    exit 1
fi
