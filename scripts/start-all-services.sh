#!/bin/bash

# Start All Services Script for VoyageAI Backend
# This script starts all required services for local development
#
# NoSQL Provider Options:
#   --mongodb   : Start MongoDB (default, platform-agnostic)
#   --dynamodb  : Start LocalStack DynamoDB (AWS-specific)
#   --all-nosql : Start both MongoDB and DynamoDB

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Parse arguments
NOSQL_PROVIDER="mongodb"  # Default to MongoDB
START_MONGODB=false
START_DYNAMODB=false

for arg in "$@"; do
    case $arg in
        --mongodb)
            START_MONGODB=true
            NOSQL_PROVIDER="mongodb"
            ;;
        --dynamodb)
            START_DYNAMODB=true
            NOSQL_PROVIDER="dynamodb"
            ;;
        --all-nosql)
            START_MONGODB=true
            START_DYNAMODB=true
            NOSQL_PROVIDER="both"
            ;;
        *)
            ;;
    esac
done

# If no NoSQL flag specified, default to MongoDB
if [ "$START_MONGODB" = false ] && [ "$START_DYNAMODB" = false ]; then
    START_MONGODB=true
fi

echo "VoyageAI - Starting All Services"
echo "===================================="
echo "NoSQL Provider: $NOSQL_PROVIDER"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "[ERROR] Docker is not installed. Please install Docker first."
    echo "   Visit: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "[ERROR] Docker is not running. Please start Docker first."
    exit 1
fi

echo "[OK] Docker is available"
echo ""

STEP=1
TOTAL_STEPS=3
if [ "$START_MONGODB" = true ] && [ "$START_DYNAMODB" = true ]; then
    TOTAL_STEPS=4
fi

# Start MySQL
echo "Step $STEP/$TOTAL_STEPS: Setting up MySQL..."
echo "--------------------------------"
"$SCRIPT_DIR/mysql-setup.sh"
echo ""
STEP=$((STEP + 1))

# Start Redis
echo "Step $STEP/$TOTAL_STEPS: Setting up Redis..."
echo "--------------------------------"
"$SCRIPT_DIR/redis-setup.sh"
echo ""
STEP=$((STEP + 1))

# Start MongoDB (if selected)
if [ "$START_MONGODB" = true ]; then
    echo "Step $STEP/$TOTAL_STEPS: Setting up MongoDB..."
    echo "--------------------------------"
    "$SCRIPT_DIR/mongodb-setup.sh"
    echo ""
    STEP=$((STEP + 1))
fi

# Start LocalStack DynamoDB (if selected)
if [ "$START_DYNAMODB" = true ]; then
    echo "Step $STEP/$TOTAL_STEPS: Setting up LocalStack (DynamoDB)..."
    echo "-------------------------------------------------"
    "$SCRIPT_DIR/localstack-setup.sh"
    echo ""
fi

echo "===================================="
echo "All services are ready!"
echo "===================================="
echo ""
echo "Running Services:"
echo "  MySQL:      localhost:3306  (database: voyageai)"
echo "  Redis:      localhost:6379"
if [ "$START_MONGODB" = true ]; then
    echo "  MongoDB:    localhost:27017 (database: voyageai)"
fi
if [ "$START_DYNAMODB" = true ]; then
    echo "  LocalStack: localhost:4566  (DynamoDB)"
fi
echo ""
echo "NoSQL Configuration (application.properties):"
if [ "$NOSQL_PROVIDER" = "mongodb" ]; then
    echo "  storage.nosql.provider=mongodb"
elif [ "$NOSQL_PROVIDER" = "dynamodb" ]; then
    echo "  storage.nosql.provider=dynamodb"
else
    echo "  storage.nosql.provider=mongodb   # or dynamodb"
fi
echo ""
echo "Next Steps:"
echo "  1. Make sure application-secrets.properties is configured"
echo "  2. Run the application: mvn spring-boot:run"
echo "  3. Check health: curl http://localhost:8081/api/health/nosql"
echo ""
echo "Useful Commands:"
CONTAINERS="mysql-voyage redis"
if [ "$START_MONGODB" = true ]; then
    CONTAINERS="$CONTAINERS mongodb-voyage"
fi
if [ "$START_DYNAMODB" = true ]; then
    CONTAINERS="$CONTAINERS localstack-voyageai"
fi
echo "  Stop all:    docker stop $CONTAINERS"
echo "  Start all:   docker start $CONTAINERS"
echo "  View status: docker ps"
