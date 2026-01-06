#!/bin/bash

# LocalStack DynamoDB Setup Script for VoyageAI

set -e

LOCALSTACK_ENDPOINT="http://localhost:4566"
AWS_REGION="us-east-1"

echo "VoyageAI LocalStack Setup"
echo "=============================="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "[ERROR] Docker is not running. Please start Docker first."
    exit 1
fi

# Check if LocalStack container exists
if docker ps -a --format '{{.Names}}' | grep -q "^localstack"; then
    CONTAINER_ID=$(docker ps -a --format '{{.ID}} {{.Names}}' | grep localstack | head -1 | awk '{print $1}')
    CONTAINER_STATUS=$(docker inspect -f '{{.State.Status}}' $CONTAINER_ID)
    
    if [ "$CONTAINER_STATUS" = "running" ]; then
        echo "[OK] LocalStack is already running"
    else
        echo "[INFO] Starting existing LocalStack container..."
        docker start $CONTAINER_ID
        sleep 3
        echo "[OK] LocalStack started"
    fi
else
    echo "[INFO] Creating new LocalStack container..."
    docker run -d \
        --name localstack-voyageai \
        -p 4566:4566 \
        -e SERVICES=dynamodb \
        -e DEBUG=1 \
        localstack/localstack:latest
    
    echo "[INFO] Waiting for LocalStack to be ready..."
    sleep 5
    echo "[OK] LocalStack started"
fi

# Check if TravelPlans table exists
echo ""
echo "Checking DynamoDB tables..."
TABLES=$(aws dynamodb list-tables --endpoint-url $LOCALSTACK_ENDPOINT --region $AWS_REGION --output text --query 'TableNames[0]' 2>/dev/null || echo "")

if [ "$TABLES" = "TravelPlans" ]; then
    echo "[OK] TravelPlans table already exists"
else
    echo "[INFO] Creating TravelPlans table..."
    aws dynamodb create-table \
        --table-name TravelPlans \
        --attribute-definitions \
            AttributeName=userId,AttributeType=S \
            AttributeName=planId,AttributeType=S \
        --key-schema \
            AttributeName=userId,KeyType=HASH \
            AttributeName=planId,KeyType=RANGE \
        --billing-mode PAY_PER_REQUEST \
        --endpoint-url $LOCALSTACK_ENDPOINT \
        --region $AWS_REGION \
        --no-cli-pager > /dev/null 2>&1
    
    echo "[OK] TravelPlans table created"
fi

echo ""
echo "Table Details:"
aws dynamodb describe-table \
    --table-name TravelPlans \
    --endpoint-url $LOCALSTACK_ENDPOINT \
    --region $AWS_REGION \
    --query 'Table.{Name:TableName,Status:TableStatus,Keys:KeySchema}' \
    --output json | jq .

echo ""
echo "[OK] LocalStack Setup Complete!"
echo ""
echo "Useful Commands:"
echo "  List tables:   aws dynamodb list-tables --endpoint-url $LOCALSTACK_ENDPOINT --region $AWS_REGION"
echo "  Describe:      aws dynamodb describe-table --table-name TravelPlans --endpoint-url $LOCALSTACK_ENDPOINT --region $AWS_REGION"
echo "  Stop:          docker stop \$(docker ps -q --filter name=localstack)"
echo "  Remove:        docker rm \$(docker ps -aq --filter name=localstack)"
echo ""
echo "Application Health Check:"
echo "  curl http://localhost:8081/api/health/dynamodb | jq ."
