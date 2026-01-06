#!/bin/bash

# Redis Setup Script for VoyageAI Backend
# This script helps set up Redis for local development

set -e

echo "VoyageAI Redis Setup"
echo "======================="

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

# Check if Redis container is already running
if docker ps | grep -q "redis"; then
    echo "[OK] Redis container is already running"
    echo "   Redis is available at localhost:6379"
    echo ""
    echo "To stop Redis: docker stop redis"
    echo "To restart Redis: docker restart redis"
    exit 0
fi

# Check if Redis container exists but is stopped
if docker ps -a | grep -q "redis"; then
    echo "[INFO] Starting existing Redis container..."
    docker start redis
    echo "[OK] Redis container started"
else
    echo "[INFO] Creating new Redis container..."
    docker run -d \
        --name redis \
        -p 6379:6379 \
        redis:7-alpine \
        redis-server --appendonly yes
    
    echo "[OK] Redis container created and started"
fi

# Wait for Redis to be ready
echo "[INFO] Waiting for Redis to be ready..."
sleep 3

# Test Redis connection
if docker exec redis redis-cli ping | grep -q "PONG"; then
    echo "[OK] Redis is ready and responding"
else
    echo "[ERROR] Redis is not responding. Please check the container logs:"
    echo "   docker logs redis"
    exit 1
fi

echo ""
echo "Redis setup complete!"
echo ""
echo "Configuration:"
echo "  Host: localhost"
echo "  Port: 6379"
echo "  Database: 0 (default)"
echo "  Persistence: AOF enabled"
echo ""
echo "Useful commands:"
echo "  Stop Redis:     docker stop redis"
echo "  Start Redis:    docker start redis"
echo "  Restart Redis:  docker restart redis"
echo "  View logs:      docker logs redis"
echo "  Redis CLI:      docker exec -it redis redis-cli"
echo ""
echo "To connect from your application:"
echo "  spring.data.redis.host=localhost"
echo "  spring.data.redis.port=6379"
echo ""
echo "Next steps:"
echo "  1. Start your Spring Boot application"
echo "  2. Submit a planning request"
echo "  3. Check Redis: docker exec -it redis redis-cli keys 'task:*'"
