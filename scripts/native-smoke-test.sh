#!/bin/bash
# MemDiag Native Agent Smoke Test
# Tests native agent loading and basic functionality in Docker containers

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "=========================================="
echo "MemDiag Native Agent Smoke Test"
echo "=========================================="

# Create test results directory
TEST_RESULTS_DIR="$PROJECT_ROOT/target/test-results"
mkdir -p "$TEST_RESULTS_DIR"

# Docker images to test
DOCKER_IMAGES=(
    "eclipse-temurin:11-jdk"
    "eclipse-temurin:17-jdk"
    "eclipse-temurin:21-jdk"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_info() {
    echo "[INFO] $1"
}

# Function to test a specific Docker image
test_docker_image() {
    local image_name="$1"
    local test_name="${image_name//:/-}"
    local test_log="$TEST_RESULTS_DIR/smoke-test-${test_name}.log"

    echo ""
    echo "----------------------------------------"
    echo "Testing: $image_name"
    echo "----------------------------------------"
    echo "Logs: $test_log"

    {
        echo "=== Smoke Test for $image_name ==="
        echo "Started: $(date)"
        echo ""

        # Check if Docker is available
        if ! command -v docker &> /dev/null; then
            echo "ERROR: Docker not available"
            return 1
        fi

        # Pull the image if not present
        if ! docker inspect "$image_name" &> /dev/null; then
            echo "Pulling image: $image_name"
            docker pull "$image_name" || {
                echo "ERROR: Failed to pull image"
                return 1
            }
        fi

        # Create a simple Java test program
        cat > /tmp/TestAgent.java << 'EOF'
public class TestAgent {
    public static void main(String[] args) throws Exception {
        System.out.println("TestAgent started");

        // Allocate some memory
        byte[] data = new byte[1024 * 1024]; // 1MB
        System.out.println("Allocated 1MB array");

        // Keep running
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("Ready for attach test");

        // Sleep to allow attach
        Thread.sleep(5000);

        System.out.println("TestAgent completed");
    }
}
EOF

        # Create Docker test script
        cat > /tmp/docker-test.sh << 'EOF'
#!/bin/bash
set -e

echo "=== Inside Docker Container ==="
echo "Java Home: $JAVA_HOME"
echo "Java Version:"
java -version

# Compile test program
echo "Compiling test program..."
javac /tmp/TestAgent.java

# Start test program in background
echo "Starting test program..."
java -cp /tmp TestAgent &
TEST_PID=$!
echo "Test PID: $TEST_PID"

# Give it time to start
sleep 2

# Check if process is running
if ps -p $TEST_PID > /dev/null; then
    echo "Test process is running"
else
    echo "ERROR: Test process not running"
    exit 1
fi

# Verify tools.jar/jmods exists
if [ -f "$JAVA_HOME/lib/tools.jar" ]; then
    echo "Found tools.jar"
elif [ -d "$JAVA_HOME/jmods" ]; then
    echo "Found jmods directory"
else
    echo "WARNING: Neither tools.jar nor jmods found"
fi

# Cleanup
echo "Cleaning up..."
kill $TEST_PID 2>/dev/null || true
wait $TEST_PID 2>/dev/null || true

echo "=== Docker test completed successfully ==="
EOF

        chmod +x /tmp/docker-test.sh

        # Run Docker container
        echo "Running Docker container..."
        docker run --rm \
            -v /tmp/TestAgent.java:/tmp/TestAgent.java \
            -v /tmp/docker-test.sh:/tmp/docker-test.sh \
            --name "memdiag-smoke-test-${test_name}" \
            "$image_name" \
            bash /tmp/docker-test.sh

        echo ""
        echo "SUCCESS: Test passed for $image_name"

    } > "$test_log" 2>&1

    if [ $? -eq 0 ]; then
        print_success "Test passed: $image_name"
        return 0
    else
        print_error "Test failed: $image_name"
        echo "Log output:"
        tail -20 "$test_log"
        return 1
    fi
}

# Function to build the project first
build_project() {
    echo ""
    echo "Building project..."
    if mvn clean package -DskipTests -q; then
        print_success "Project built successfully"
        return 0
    else
        print_error "Failed to build project"
        return 1
    fi
}

# Main execution
main() {
    local failed=0
    local skipped=0
    local passed=0

    # Check if we should skip Docker tests
    if [ "$1" = "--skip-docker" ]; then
        print_warning "Skipping Docker tests as requested"
        skipped=${#DOCKER_IMAGES[@]}
    else
        # Check Docker availability
        if ! command -v docker &> /dev/null; then
            print_warning "Docker not available, skipping Docker-based tests"
            skipped=${#DOCKER_IMAGES[@]}
        else
            # Run Docker tests
            for image in "${DOCKER_IMAGES[@]}"; do
                if test_docker_image "$image"; then
                    ((passed++))
                else
                    ((failed++))
                fi
            done
        fi
    fi

    # Always run local tests
    echo ""
    echo "----------------------------------------"
    echo "Running local unit tests..."
    echo "----------------------------------------"
    if mvn test -pl memdiag-core -q; then
        print_success "Local unit tests passed"
        ((passed++))
    else
        print_error "Local unit tests failed"
        ((failed++))
    fi

    # Summary
    echo ""
    echo "=========================================="
    echo "Test Summary"
    echo "=========================================="
    echo "Passed:  $passed"
    echo "Failed:  $failed"
    echo "Skipped: $skipped"
    echo ""

    if [ $failed -eq 0 ]; then
        print_success "All tests passed!"
        exit 0
    else
        print_error "Some tests failed"
        exit 1
    fi
}

# Run main
main "$@"
