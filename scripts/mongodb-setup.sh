#!/bin/bash

# MongoDB Setup Script for VoyageAI Backend
# This script helps set up MongoDB for local development

set -e

echo "VoyageAI MongoDB Setup"
echo "======================="

# Configuration
MONGO_CONTAINER_NAME="mongodb-voyage"
MONGO_PORT="27017"
MONGO_DATABASE="voyageai"

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

# Check if MongoDB container is already running
if docker ps | grep -q "$MONGO_CONTAINER_NAME"; then
    echo "[OK] MongoDB container is already running"
    echo "   MongoDB is available at localhost:$MONGO_PORT"
    echo ""
    echo "To stop MongoDB: docker stop $MONGO_CONTAINER_NAME"
    echo "To restart MongoDB: docker restart $MONGO_CONTAINER_NAME"
    exit 0
fi

# Check if MongoDB container exists but is stopped
if docker ps -a | grep -q "$MONGO_CONTAINER_NAME"; then
    echo "[INFO] Starting existing MongoDB container..."
    docker start $MONGO_CONTAINER_NAME
    echo "[OK] MongoDB container started"
else
    echo "[INFO] Creating new MongoDB container..."
    docker run -d \
        --name $MONGO_CONTAINER_NAME \
        -p $MONGO_PORT:27017 \
        -e MONGO_INITDB_DATABASE=$MONGO_DATABASE \
        mongo:7.0
    
    echo "[OK] MongoDB container created and started"
fi

# Wait for MongoDB to be ready
echo "[INFO] Waiting for MongoDB to be ready..."
sleep 5

# Test MongoDB connection
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec $MONGO_CONTAINER_NAME mongosh --eval "db.adminCommand('ping')" &> /dev/null; then
        echo "[OK] MongoDB is ready and responding"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "[INFO] Waiting for MongoDB to be ready... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "[ERROR] MongoDB is not responding. Please check the container logs:"
    echo "   docker logs $MONGO_CONTAINER_NAME"
    exit 1
fi

# Create indexes
echo ""
echo "[INFO] Creating indexes..."
docker exec $MONGO_CONTAINER_NAME mongosh $MONGO_DATABASE --eval '
db.travel_plans.createIndex({ "userId": 1, "planId": 1 }, { unique: true });
db.travel_plans.createIndex({ "userId": 1 });
db.travel_plans.createIndex({ "projectId": 1 });
db.travel_plans.createIndex({ "status": 1 });
print("Indexes created successfully");
' 2>/dev/null || echo "[INFO] Indexes may already exist"

echo ""
echo "MongoDB setup complete!"
echo ""
echo "Configuration:"
echo "  Host: localhost"
echo "  Port: $MONGO_PORT"
echo "  Database: $MONGO_DATABASE"
echo "  Collection: travel_plans"
echo ""
echo "Useful commands:"
echo "  Stop MongoDB:     docker stop $MONGO_CONTAINER_NAME"
echo "  Start MongoDB:    docker start $MONGO_CONTAINER_NAME"
echo "  Restart MongoDB:  docker restart $MONGO_CONTAINER_NAME"
echo "  View logs:        docker logs $MONGO_CONTAINER_NAME"
echo "  Mongo Shell:      docker exec -it $MONGO_CONTAINER_NAME mongosh $MONGO_DATABASE"
echo ""
echo "To connect from your application:"
echo "  spring.data.mongodb.uri=mongodb://localhost:$MONGO_PORT/$MONGO_DATABASE"
echo "  storage.nosql.provider=mongodb"

