#!/bin/bash

# Puppies Ecosystem - Test Execution and Coverage Report Script
# 
# This script runs unit tests for all modules and generates coverage reports
# Usage: ./run-tests.sh [module] [test-type]
#   module: command-api, query-api, sync-worker, or all (default: all)
#   test-type: unit, integration, or all (default: unit)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
MODULE=${1:-all}
TEST_TYPE=${2:-unit}

echo -e "${BLUE}🚀 Puppies Ecosystem - Test Execution Script${NC}"
echo -e "${BLUE}=================================================${NC}"
echo -e "Module: ${YELLOW}$MODULE${NC}"
echo -e "Test Type: ${YELLOW}$TEST_TYPE${NC}"
echo ""

# Function to run tests for a specific module
run_module_tests() {
    local module_name=$1
    local module_path=$2
    
    echo -e "${BLUE}📦 Testing $module_name...${NC}"
    echo -e "${BLUE}------------------------------------------${NC}"
    
    cd "$module_path"
    
    case $TEST_TYPE in
        "unit")
            echo -e "${YELLOW}🧪 Running unit tests...${NC}"
            mvn clean test jacoco:report
            ;;
        "integration")
            echo -e "${YELLOW}🔗 Running integration tests...${NC}"
            mvn clean verify -P integration-tests
            ;;
        "all")
            echo -e "${YELLOW}🧪 Running all tests...${NC}"
            mvn clean verify jacoco:report
            ;;
        *)
            echo -e "${RED}❌ Invalid test type: $TEST_TYPE${NC}"
            exit 1
            ;;
    esac
    
    # Check if tests passed
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ $module_name tests passed!${NC}"
        
        # Display coverage report location
        if [ -f "target/site/jacoco/index.html" ]; then
            echo -e "${GREEN}📊 Coverage report: $(pwd)/target/site/jacoco/index.html${NC}"
        fi
    else
        echo -e "${RED}❌ $module_name tests failed!${NC}"
        exit 1
    fi
    
    cd - > /dev/null
    echo ""
}

# Function to display coverage summary
display_coverage_summary() {
    echo -e "${BLUE}📊 Coverage Summary${NC}"
    echo -e "${BLUE}===================${NC}"
    
    for module in "puppies-command-api" "puppies-query-api" "puppies-sync-worker"; do
        if [ -f "$module/target/site/jacoco/index.html" ]; then
            echo -e "${GREEN}📈 $module coverage report available${NC}"
            
            # Try to extract coverage percentage (basic approach)
            if command -v grep &> /dev/null && command -v awk &> /dev/null; then
                coverage_file="$module/target/site/jacoco/index.html"
                if [ -f "$coverage_file" ]; then
                    # This is a simple extraction - in real scenarios, you might want more sophisticated parsing
                    echo -e "   Report: file://$(pwd)/$module/target/site/jacoco/index.html"
                fi
            fi
        else
            echo -e "${YELLOW}⚠️  $module coverage report not found${NC}"
        fi
    done
    echo ""
}

# Function to check prerequisites
check_prerequisites() {
    echo -e "${BLUE}🔍 Checking prerequisites...${NC}"
    
    # Check if Maven is installed
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}❌ Maven is not installed or not in PATH${NC}"
        exit 1
    fi
    
    # Check if Java is installed
    if ! command -v java &> /dev/null; then
        echo -e "${RED}❌ Java is not installed or not in PATH${NC}"
        exit 1
    fi
    
    # Check if Docker is running (for integration tests)
    if [ "$TEST_TYPE" = "integration" ] || [ "$TEST_TYPE" = "all" ]; then
        if ! docker info &> /dev/null; then
            echo -e "${YELLOW}⚠️  Docker is not running. Integration tests may fail.${NC}"
            echo -e "${YELLOW}   Please start Docker: docker-compose up -d${NC}"
        else
            echo -e "${GREEN}✅ Docker is running${NC}"
        fi
    fi
    
    echo -e "${GREEN}✅ Prerequisites check completed${NC}"
    echo ""
}

# Function to start infrastructure for integration tests
start_infrastructure() {
    if [ "$TEST_TYPE" = "integration" ] || [ "$TEST_TYPE" = "all" ]; then
        echo -e "${BLUE}🏗️  Starting test infrastructure...${NC}"
        
        if [ -f "docker-compose.yml" ]; then
            docker-compose up -d
            
            # Wait for services to be ready
            echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}"
            sleep 10
            
            echo -e "${GREEN}✅ Infrastructure started${NC}"
        else
            echo -e "${YELLOW}⚠️  docker-compose.yml not found in current directory${NC}"
        fi
        echo ""
    fi
}

# Main execution
main() {
    # Change to the ecosystem directory
    if [ ! -d "puppies-command-api" ] && [ ! -d "puppies-query-api" ] && [ ! -d "puppies-sync-worker" ]; then
        echo -e "${RED}❌ Please run this script from the puppies-ecosystem directory${NC}"
        exit 1
    fi
    
    check_prerequisites
    start_infrastructure
    
    case $MODULE in
        "command-api")
            run_module_tests "Command API" "puppies-command-api"
            ;;
        "query-api")
            run_module_tests "Query API" "puppies-query-api"
            ;;
        "sync-worker")
            run_module_tests "Sync Worker" "puppies-sync-worker"
            ;;
        "all")
            run_module_tests "Command API" "puppies-command-api"
            run_module_tests "Query API" "puppies-query-api"
            run_module_tests "Sync Worker" "puppies-sync-worker"
            ;;
        *)
            echo -e "${RED}❌ Invalid module: $MODULE${NC}"
            echo -e "${YELLOW}Valid modules: command-api, query-api, sync-worker, all${NC}"
            exit 1
            ;;
    esac
    
    display_coverage_summary
    
    echo -e "${GREEN}🎉 Test execution completed successfully!${NC}"
    echo -e "${BLUE}📋 Next steps:${NC}"
    echo -e "   1. Open coverage reports in your browser"
    echo -e "   2. Review test results and coverage metrics"
    echo -e "   3. Address any failing tests or low coverage areas"
    echo -e "   4. Run integration tests if you only ran unit tests"
}

# Script usage help
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    echo "Usage: $0 [module] [test-type]"
    echo ""
    echo "Parameters:"
    echo "  module     Module to test (command-api, query-api, sync-worker, all) [default: all]"
    echo "  test-type  Type of tests (unit, integration, all) [default: unit]"
    echo ""
    echo "Examples:"
    echo "  $0                          # Run unit tests for all modules"
    echo "  $0 command-api             # Run unit tests for command API only"
    echo "  $0 all integration         # Run integration tests for all modules"
    echo "  $0 query-api all           # Run all tests for query API only"
    echo ""
    echo "Requirements:"
    echo "  - Maven 3.6+"
    echo "  - Java 17+"
    echo "  - Docker (for integration tests)"
    exit 0
fi

# Run main function
main