#!/bin/bash

# MySQL Setup Script for VoyageAI Backend
# This script helps set up MySQL for local development

set -e

echo "VoyageAI MySQL Setup"
echo "======================="

# Configuration
MYSQL_CONTAINER_NAME="mysql-voyage"
MYSQL_ROOT_PASSWORD="RootPassword123"
MYSQL_DATABASE="voyageai"
MYSQL_PORT="3306"

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

# Check if MySQL container is already running
if docker ps | grep -q "$MYSQL_CONTAINER_NAME"; then
    echo "[OK] MySQL container is already running"
    echo "   MySQL is available at localhost:$MYSQL_PORT"
    echo ""
    echo "To stop MySQL: docker stop $MYSQL_CONTAINER_NAME"
    echo "To restart MySQL: docker restart $MYSQL_CONTAINER_NAME"
    exit 0
fi

# Check if MySQL container exists but is stopped
if docker ps -a | grep -q "$MYSQL_CONTAINER_NAME"; then
    echo "[INFO] Starting existing MySQL container..."
    docker start $MYSQL_CONTAINER_NAME
    echo "[OK] MySQL container started"
else
    echo "[INFO] Creating new MySQL container..."
    docker run -d \
        --name $MYSQL_CONTAINER_NAME \
        -p $MYSQL_PORT:3306 \
        -e MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD \
        -e MYSQL_DATABASE=$MYSQL_DATABASE \
        mysql:8.0 \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci
    
    echo "[OK] MySQL container created and started"
fi

# Wait for MySQL to be ready
echo "[INFO] Waiting for MySQL to be ready..."
sleep 10

# Test MySQL connection
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec $MYSQL_CONTAINER_NAME mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "SELECT 1" &> /dev/null; then
        echo "[OK] MySQL is ready and responding"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo "[INFO] Waiting for MySQL to be ready... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "[ERROR] MySQL is not responding. Please check the container logs:"
    echo "   docker logs $MYSQL_CONTAINER_NAME"
    exit 1
fi

echo ""
echo "MySQL setup complete!"
echo ""
echo "Configuration:"
echo "  Host: localhost"
echo "  Port: $MYSQL_PORT"
echo "  Database: $MYSQL_DATABASE"
echo "  Username: root"
echo "  Password: $MYSQL_ROOT_PASSWORD"
echo ""
echo "Useful commands:"
echo "  Stop MySQL:     docker stop $MYSQL_CONTAINER_NAME"
echo "  Start MySQL:    docker start $MYSQL_CONTAINER_NAME"
echo "  Restart MySQL:  docker restart $MYSQL_CONTAINER_NAME"
echo "  View logs:      docker logs $MYSQL_CONTAINER_NAME"
echo "  MySQL CLI:      docker exec -it $MYSQL_CONTAINER_NAME mysql -uroot -p$MYSQL_ROOT_PASSWORD"
echo ""
echo "To connect from your application:"
echo "  spring.datasource.url=jdbc:mysql://localhost:$MYSQL_PORT/$MYSQL_DATABASE"
echo "  spring.datasource.username=root"
echo "  spring.datasource.password=$MYSQL_ROOT_PASSWORD"
