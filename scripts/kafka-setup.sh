#!/bin/bash
# Kafka setup script - runs Kafka in KRaft mode (no ZooKeeper required)
# KRaft = Kafka Raft metadata mode (production-ready since Kafka 3.3+)

set -euo pipefail

KAFKA_CONTAINER_NAME="voyageai-kafka"
KAFKA_PORT=9092
KAFKA_IMAGE="apache/kafka:3.7.0"

echo "=== VoyageAI Kafka Setup (KRaft Mode) ==="

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "ERROR: Docker is not running. Please start Docker first."
    exit 1
fi

# Check if Kafka is already running
if docker ps --format '{{.Names}}' | grep -q "^${KAFKA_CONTAINER_NAME}$"; then
    echo "Kafka is already running on port ${KAFKA_PORT}"
    docker exec ${KAFKA_CONTAINER_NAME} /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --list 2>/dev/null || true
    exit 0
fi

# Remove stopped container if exists
docker rm -f ${KAFKA_CONTAINER_NAME} 2>/dev/null || true

echo "Starting Kafka (KRaft mode) on port ${KAFKA_PORT}..."

docker run -d \
    --name ${KAFKA_CONTAINER_NAME} \
    -p ${KAFKA_PORT}:9092 \
    -e KAFKA_NODE_ID=1 \
    -e KAFKA_PROCESS_ROLES=broker,controller \
    -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
    -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
    -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
    -e CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk \
    ${KAFKA_IMAGE}

echo "Waiting for Kafka to be ready..."
for i in $(seq 1 30); do
    if docker exec ${KAFKA_CONTAINER_NAME} /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --list > /dev/null 2>&1; then
        echo "Kafka is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: Kafka failed to start within 30 seconds"
        docker logs ${KAFKA_CONTAINER_NAME}
        exit 1
    fi
    sleep 1
done

# Create planning pipeline topics
echo "Creating planning pipeline topics..."

for topic in planning.request planning.progress planning.result; do
    docker exec ${KAFKA_CONTAINER_NAME} /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 \
        --create \
        --topic ${topic} \
        --partitions 3 \
        --replication-factor 1 \
        --if-not-exists
    echo "  Created topic: ${topic}"
done

echo ""
echo "=== Kafka Setup Complete ==="
echo "Bootstrap servers: localhost:${KAFKA_PORT}"
echo "Topics:"
docker exec ${KAFKA_CONTAINER_NAME} /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server localhost:9092 --list
