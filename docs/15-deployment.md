# Module 15: Deployment - From Docker Compose to Kubernetes to AWS EKS

## Table of Contents

1. [Overview](#1-overview)
2. [Theoretical Foundations](#2-theoretical-foundations)
3. [Docker Multi-Stage Builds](#3-docker-multi-stage-builds)
4. [Docker Compose Local Stack](#4-docker-compose-local-stack)
5. [Kubernetes Architecture](#5-kubernetes-architecture)
6. [K8s Manifests Deep Dive](#6-k8s-manifests-deep-dive)
7. [AWS EKS with CDK](#7-aws-eks-with-cdk)
8. [Security Considerations](#8-security-considerations)
9. [Monitoring and Observability](#9-monitoring-and-observability)
10. [Cost Management](#10-cost-management)
11. [Hands-On Exercises](#11-hands-on-exercises)

---

## 1. Overview

### The Deployment Progression

```
┌─────────────────────────────────────────────────────────────────┐
│              Deployment Maturity Levels                           │
│                                                                  │
│  Level 1: Docker Compose (this module)                           │
│  ├── Single machine, all services                                │
│  ├── Good for: Development, demos, small teams                   │
│  ├── Limitations: No HA, no auto-scaling, single point of failure│
│  └── Command: docker-compose up -d                               │
│                                                                  │
│  Level 2: Self-hosted Kubernetes (this module)                   │
│  ├── Multi-node cluster (minikube, k3s, kubeadm)                 │
│  ├── Good for: Staging, medium scale, learning K8s               │
│  ├── Has: Auto-restart, rolling updates, health probes           │
│  └── Commands: kubectl apply -f k8s/base/                        │
│                                                                  │
│  Level 3: AWS EKS (this module - CDK only)                       │
│  ├── Managed Kubernetes on AWS                                    │
│  ├── Good for: Production, high availability, auto-scaling       │
│  ├── Has: Managed control plane, ALB ingress, RDS, ElastiCache   │
│  └── Commands: cdk deploy --all                                   │
└─────────────────────────────────────────────────────────────────┘
```

### System Architecture

VoyageAI is a polyglot microservice system with eight services across three repositories:

```
┌─────────────────────────────────────────────────────────────────┐
│                  VoyageAI Full Stack Architecture                 │
│                                                                  │
│  Repositories:                                                   │
│  ├── voyageai-backend     (Java Spring Boot + Docker Compose)    │
│  ├── voyageai-web         (React + Vite + Nginx)                 │
│  └── voyageai-python-service (FastAPI + Kafka Worker)            │
│                                                                  │
│  Application Services (4):                                       │
│  ├── frontend       React chatbot UI, served by Nginx            │
│  │                  Proxies /api, /oauth2, /actuator → backend   │
│  ├── java-backend   Spring Boot REST API + SSE + OAuth2          │
│  │                  Manages users, projects, tasks, conversations │
│  ├── python-service FastAPI AI service (direct HTTP)             │
│  └── python-worker  Kafka consumer → OpenAI → result events      │
│                                                                  │
│  Infrastructure Services (4):                                    │
│  ├── kafka          Message broker (KRaft mode, no ZooKeeper)    │
│  ├── redis          Task state cache + conversation cache        │
│  ├── mongodb        Travel plan document store                   │
│  └── mysql          User, project, conversation persistence      │
└─────────────────────────────────────────────────────────────────┘
```

### Module Learning Objectives

By the end of this module, you will:

- Build multi-stage Docker images for Java, Python, and React services
- Configure Docker Compose for a complete local development stack
- Understand Nginx reverse proxy configuration for SPA + API + OAuth2
- Design K8s manifests with Deployments, StatefulSets, Services, and Ingress
- Implement health probes (liveness, readiness, startup) for K8s
- Create AWS CDK stacks for VPC, EKS, RDS, ElastiCache, and ECR
- Write deploy/destroy/nuke scripts for infrastructure lifecycle management
- Understand the security implications of each deployment method

---

## 2. Theoretical Foundations

### 2.1 Container Orchestration

```
┌─────────────────────────────────────────────────────────────────┐
│              Why Containers?                                      │
│                                                                  │
│  Without containers:                                              │
│  ├── "Works on my machine" problem                                │
│  ├── Dependency conflicts between services                        │
│  ├── Manual environment setup per developer                       │
│  └── Different versions of Java/Python/Node per machine           │
│                                                                  │
│  With containers:                                                 │
│  ├── Identical environment everywhere (dev/staging/prod)          │
│  ├── Each service isolated with its own dependencies              │
│  ├── Reproducible builds from Dockerfile                          │
│  └── `docker-compose up` → entire stack running                   │
│                                                                  │
│  With orchestration (K8s):                                        │
│  ├── Auto-restart crashed containers                              │
│  ├── Rolling updates with zero downtime                           │
│  ├── Horizontal scaling (more replicas under load)                │
│  ├── Health-based traffic routing                                 │
│  └── Declarative desired state (vs imperative commands)           │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 Reverse Proxy Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              Nginx as Frontend Gateway                            │
│                                                                  │
│  Browser → :3000 (Nginx) → routes requests by path:              │
│                                                                  │
│  /                → serve index.html (SPA)                       │
│  /assets/*        → serve static files (JS, CSS, images)         │
│  /api/*           → proxy to java-backend:8081                   │
│  /oauth2/*        → proxy to java-backend:8081 (OAuth2 start)    │
│  /login/oauth2/*  → proxy to java-backend:8081 (OAuth2 callback) │
│  /actuator/*      → proxy to java-backend:8081 (health checks)   │
│                                                                  │
│  Why?                                                            │
│  ├── Single origin for browser (no CORS)                         │
│  ├── SSE streaming requires proxy_buffering off                  │
│  ├── OAuth2 redirects must share same origin as frontend         │
│  ├── Static asset caching with immutable headers                 │
│  └── Security headers (X-Frame-Options, X-Content-Type-Options)  │
│                                                                  │
│  In development (Vite dev server):                               │
│  ├── Same proxy rules via vite.config.ts server.proxy            │
│  └── localhost:5173 proxies /api, /oauth2, /login/oauth2 → 8081  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Deployment vs StatefulSet

```
┌─────────────────────────────────────────────────────────────────┐
│              K8s Workload Types                                   │
│                                                                  │
│  Deployment (stateless services):                                 │
│  ├── Java Backend, Python Service, Python Worker, Frontend        │
│  ├── Pods are interchangeable (any replica can handle request)    │
│  ├── No persistent state in the pod                               │
│  ├── Scale up/down freely                                         │
│  └── Rolling update: create new → verify → destroy old            │
│                                                                  │
│  StatefulSet (stateful services):                                 │
│  ├── Kafka, MongoDB, MySQL, Redis                                 │
│  ├── Each pod has stable network identity (kafka-0, kafka-1)      │
│  ├── Persistent volumes survive pod restarts                      │
│  ├── Ordered startup/shutdown                                     │
│  └── Used for databases and message brokers                       │
│                                                                  │
│  Key difference:                                                  │
│  Deployment pods: cattle (replaceable, numbered randomly)         │
│  StatefulSet pods: pets (named, ordered, with persistent data)    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.4 K8s Probe Types

```
┌─────────────────────────────────────────────────────────────────┐
│              Probe Types and When They Fire                       │
│                                                                  │
│  Timeline:  Start → ··· → Running → ··· → Degraded → ···        │
│                                                                  │
│  Startup Probe:                                                   │
│  ├── When: Only during initial startup                            │
│  ├── If fails: Pod keeps trying (up to failureThreshold)          │
│  ├── If succeeds: Liveness/readiness probes begin                 │
│  └── Use for: Slow-starting apps (Java with 30s+ startup)        │
│                                                                  │
│  Liveness Probe:                                                  │
│  ├── When: Continuously after startup probe succeeds              │
│  ├── If fails: K8s KILLS and RESTARTS the pod                     │
│  └── Use for: Detecting deadlocks, OOM, hung processes            │
│                                                                  │
│  Readiness Probe:                                                 │
│  ├── When: Continuously after startup probe succeeds              │
│  ├── If fails: K8s REMOVES pod from Service (no traffic)          │
│  ├── Pod is NOT killed, just removed from load balancer            │
│  └── Use for: Checking downstream dependencies (DB, Kafka)        │
│                                                                  │
│  Our config:                                                      │
│  Java Backend:                                                    │
│    startupProbe:   /actuator/health/liveness   (15s init, 5s per) │
│    livenessProbe:  /actuator/health/liveness   (60s init, 10s per)│
│    readinessProbe: /actuator/health/readiness  (30s init, 15s per)│
│                                                                  │
│  Frontend (Nginx):                                                │
│    healthcheck: wget --spider http://127.0.0.1:80/ (30s interval) │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. Docker Multi-Stage Builds

### 3.1 Why Multi-Stage?

```
┌─────────────────────────────────────────────────────────────────┐
│              Single-Stage vs Multi-Stage                          │
│                                                                  │
│  Single-stage (bad):                                              │
│  FROM eclipse-temurin:21-jdk      (400MB+)                       │
│  COPY . .                                                         │
│  RUN mvn package                   (+200MB Maven cache)           │
│  CMD ["java", "-jar", "app.jar"]                                  │
│  → Final image: ~700MB (includes compiler, Maven, source code)   │
│                                                                  │
│  Multi-stage (good):                                              │
│  FROM eclipse-temurin:21-jdk AS builder                           │
│  COPY . .                                                         │
│  RUN mvn package                                                  │
│                                                                  │
│  FROM eclipse-temurin:21-jre-alpine  (60MB)                       │
│  COPY --from=builder app.jar .                                    │
│  → Final image: ~120MB (JRE + JAR only, no compiler/source)     │
│                                                                  │
│  Benefits: Smaller image, faster pull, smaller attack surface    │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Java Backend Dockerfile

```dockerfile
# docker/Dockerfile.java
# Stage 1: Build with JDK + Maven
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S voyageai && adduser -S voyageai -G voyageai
COPY --from=builder /app/target/*.jar app.jar
USER voyageai

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
```

### 3.3 Frontend Dockerfile

```dockerfile
# voyageai-web/Dockerfile
# Stage 1: Build React app with Node.js
FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q --spider http://127.0.0.1:80/ || exit 1

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 3.4 JVM Container Tuning

```dockerfile
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport",    # Detect container memory limits
    "-XX:MaxRAMPercentage=75.0",   # Use 75% of container memory for heap
    "-XX:+UseG1GC",                # G1 garbage collector
    "-Djava.security.egd=file:/dev/./urandom",  # Faster startup
    "-jar", "app.jar"]
```

Without `-XX:+UseContainerSupport`, the JVM reads the host's total memory and may allocate more heap than the container's memory limit, causing OOMKills.

### 3.5 Non-Root User

```dockerfile
RUN addgroup -S voyageai && adduser -S voyageai -G voyageai
USER voyageai
```

Running as root inside a container is a security risk. If the container is compromised, the attacker has root access to the container filesystem.

---

## 4. Docker Compose Local Stack

### 4.1 Service Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│              Docker Compose Service Dependencies                  │
│                                                                  │
│  Browser :3000                                                   │
│    │                                                             │
│    ▼                                                             │
│  frontend (Nginx) ──proxy──▶ java-backend ──▶ mysql              │
│    │  ┌─ /api/*                   │        ──▶ redis             │
│    │  ├─ /oauth2/*                │        ──▶ mongodb           │
│    │  ├─ /login/oauth2/*          │        ──▶ kafka             │
│    │  └─ /actuator/*              │                              │
│    │                              │                              │
│    │  python-service ──▶ kafka, redis, mongodb                   │
│    │  python-worker  ──▶ kafka, redis, mongodb                   │
│    │                                                             │
│    │  depends_on with condition: service_healthy ensures          │
│    │  infrastructure is ready before app services start.          │
│                                                                  │
│  Data Flow:                                                      │
│  frontend → Nginx → java-backend → Kafka → python-worker        │
│                  ↑                                ↓              │
│                  └── SSE stream ← Redis ← Kafka result ──┘      │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 Nginx Proxy Configuration (Production)

The frontend Nginx config handles SPA routing, API proxying, and OAuth2 forwarding:

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;

    # SPA fallback - all routes serve index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API proxy with SSE support
    location /api/ {
        proxy_pass http://java-backend:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # Critical for SSE streaming:
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 300s;
    }

    # OAuth2 authorization (start Google login)
    location /oauth2/ {
        proxy_pass http://java-backend:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # OAuth2 callback (Google redirects back)
    location /login/oauth2/ {
        proxy_pass http://java-backend:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Actuator health checks
    location /actuator/ {
        proxy_pass http://java-backend:8081;
    }

    # Static asset caching (1 year, immutable)
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
}
```

**Key insight**: `proxy_buffering off` is essential for SSE. Without it, Nginx buffers the entire response before sending to the client, defeating the purpose of streaming.

### 4.3 Health Checks in Compose

```yaml
kafka:
  healthcheck:
    test: ["CMD-SHELL", "kafka-topics.sh --bootstrap-server localhost:9092 --list || exit 1"]
    interval: 15s
    timeout: 10s
    retries: 5
    start_period: 30s  # Don't check for first 30s (Kafka needs time to start)
```

The `start_period` is critical for slow-starting services. Without it, the health check fails immediately and dependent services won't start.

### 4.4 Environment Variable Strategy

The Docker Compose file uses a layered configuration approach:

```yaml
java-backend:
  environment:
    # Database connection (Docker internal DNS)
    SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/voyageai
    # Frontend URL for CORS and OAuth2 redirect
    FRONTEND_URL: http://localhost:3000
    # Override application.properties for Docker context
    SPRING_APPLICATION_JSON: '{"frontend.url":"http://localhost:3000"}'
    # Secrets from .env file
    OPENAI_API_KEY: ${OPENAI_API_KEY:-}
    JWT_SECRET: ${JWT_SECRET:-your-super-secret-jwt-key-for-development-only}
```

**Why `SPRING_APPLICATION_JSON`?** Spring Boot's `application.properties` sets `frontend.url=http://localhost:5173` for Vite development. In Docker, the frontend is at `http://localhost:3000`. The `SPRING_APPLICATION_JSON` env var overrides specific properties without modifying the file.

### 4.5 One-Click Startup

```bash
# Start everything
docker-compose up -d

# Watch logs
docker-compose logs -f java-backend python-worker

# Verify health
docker-compose ps     # All services should be "healthy"
curl http://localhost:3000          # Frontend
curl http://localhost:8081/actuator/health  # Backend

# Stop everything
docker-compose down

# Nuclear: stop + remove volumes (all data lost)
docker-compose down -v
```

### 4.6 Service Port Map

| Service | Container Port | Host Port | URL |
|---|---|---|---|
| frontend (Nginx) | 80 | 3000 | http://localhost:3000 |
| java-backend | 8081 | 8081 | http://localhost:8081 |
| python-service | 8000 | 8000 | http://localhost:8000 |
| kafka | 9092 | 9092 | localhost:9092 |
| redis | 6379 | 6379 | localhost:6379 |
| mongodb | 27017 | 27017 | localhost:27017 |
| mysql | 3306 | 3306 | localhost:3306 |

---

## 5. Kubernetes Architecture

### 5.1 Our K8s Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Namespace: voyageai                                              │
│                                                                  │
│  ┌──────────────── Deployments ─────────────────────────┐       │
│  │                                                        │       │
│  │  java-backend (2 replicas)    python-service (1)       │       │
│  │  python-worker (2 replicas)   frontend (2)             │       │
│  └────────────────────────────────────────────────────────┘       │
│                                                                  │
│  ┌──────────────── StatefulSets ────────────────────────┐       │
│  │                                                        │       │
│  │  kafka (1, PVC: 5Gi)   mongodb (1, PVC: 5Gi)          │       │
│  │  mysql (1, PVC: 10Gi)  redis (1, Deployment)          │       │
│  └────────────────────────────────────────────────────────┘       │
│                                                                  │
│  ┌──────────────── Networking ──────────────────────────┐       │
│  │                                                        │       │
│  │  Ingress (voyageai.local)                              │       │
│  │  ├── /              → frontend:80                      │       │
│  │  ├── /api           → java-backend:8081                │       │
│  │  ├── /oauth2        → java-backend:8081                │       │
│  │  ├── /login/oauth2  → java-backend:8081                │       │
│  │  └── /actuator      → java-backend:8081                │       │
│  │                                                        │       │
│  │  Services (ClusterIP):                                 │       │
│  │  java-backend:8081  python-service:8000                │       │
│  │  kafka:9092  redis:6379  mongodb:27017  mysql:3306     │       │
│  └────────────────────────────────────────────────────────┘       │
│                                                                  │
│  ┌──────────────── Config ──────────────────────────────┐       │
│  │  ConfigMap: voyageai-config (all non-secret env vars)  │       │
│  │  Secret: voyageai-secrets (passwords, API keys)        │       │
│  └────────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 K8s Manifest Files

```
k8s/base/
├── namespace.yaml          # voyageai namespace
├── configmap.yaml          # Non-secret configuration
├── secrets.yaml            # Passwords, API keys, JWT secret
├── java-backend.yaml       # Deployment (2 replicas) + Service
├── python-service.yaml     # Deployment (1 replica) + Service
├── python-worker.yaml      # Deployment (2 replicas)
├── frontend.yaml           # Deployment (2 replicas) + Service
├── mysql.yaml              # StatefulSet + Headless Service (10Gi)
├── mongodb.yaml            # StatefulSet + Headless Service (5Gi)
├── redis.yaml              # Deployment + Service
├── kafka.yaml              # StatefulSet + Headless Service (5Gi)
└── ingress.yaml            # NGINX Ingress (voyageai.local)
```

---

## 6. K8s Manifests Deep Dive

### 6.1 Resource Requests and Limits

```yaml
resources:
  requests:    # Guaranteed minimum
    cpu: 500m      # 0.5 CPU core
    memory: 512Mi  # 512 MB RAM
  limits:      # Maximum allowed
    cpu: "1"       # 1 CPU core
    memory: 1Gi    # 1 GB RAM
```

**Why both?**
- `requests` = scheduling guarantee. K8s places pod on a node with this much free.
- `limits` = hard cap. If pod exceeds memory limit → OOMKilled. If exceeds CPU → throttled.

Our resource allocation:

| Service | CPU Request | Memory Request | CPU Limit | Memory Limit |
|---|---|---|---|---|
| java-backend | 500m | 512Mi | 1000m | 1Gi |
| python-worker | 500m | 512Mi | 1000m | 1Gi |
| python-service | 250m | 256Mi | 500m | 512Mi |
| frontend | 100m | 64Mi | 200m | 128Mi |

### 6.2 Graceful Shutdown in K8s

```yaml
spec:
  terminationGracePeriodSeconds: 60  # K8s waits 60s before SIGKILL
```

K8s shutdown sequence:
1. Pod marked for termination
2. SIGTERM sent to container
3. Our graceful shutdown handler runs (close SSE, flush Kafka)
4. If not terminated within 60s → SIGKILL

### 6.3 Headless Services for StatefulSets

```yaml
apiVersion: v1
kind: Service
metadata:
  name: kafka
spec:
  clusterIP: None  # Headless!
```

Headless services don't load-balance. Instead, DNS returns all pod IPs, allowing direct connection to specific pods (e.g., `kafka-0.kafka.voyageai.svc.cluster.local`).

### 6.4 Ingress with OAuth2 Routes

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: voyageai-ingress
  annotations:
    nginx.ingress.kubernetes.io/proxy-read-timeout: "300"
    nginx.ingress.kubernetes.io/proxy-buffering: "off"
spec:
  rules:
    - host: voyageai.local
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service: { name: java-backend, port: { number: 8081 } }
          - path: /oauth2
            pathType: Prefix
            backend:
              service: { name: java-backend, port: { number: 8081 } }
          - path: /login/oauth2
            pathType: Prefix
            backend:
              service: { name: java-backend, port: { number: 8081 } }
          - path: /
            pathType: Prefix
            backend:
              service: { name: frontend, port: { number: 80 } }
```

**Important**: The Ingress `proxy-buffering: "off"` annotation is critical for SSE streaming to work through the NGINX Ingress controller.

---

## 7. AWS EKS with CDK

### 7.1 CDK Stack Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              CDK Stack Dependencies                               │
│                                                                  │
│  VpcStack ──▶ DatabaseStack                                      │
│           ──▶ EksStack                                            │
│                                                                  │
│  EcrStack (independent)                                           │
│                                                                  │
│  Deploy order: VPC → ECR → Database → EKS                        │
│  Destroy order: EKS → Database → ECR → VPC                       │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Why CDK over CloudFormation/Terraform?

| Tool | Language | Type Safety | AWS Native | Learning Curve |
|---|---|---|---|---|
| CloudFormation | YAML/JSON | No | Yes | Medium |
| Terraform | HCL | No | No (provider) | Medium |
| **CDK** | **TypeScript** | **Yes** | **Yes** | **Low for devs** |
| Pulumi | TypeScript | Yes | No (provider) | Medium |

CDK compiles to CloudFormation, so you get AWS-native deployment with TypeScript type safety.

### 7.3 Production vs Development

In production, you would replace self-hosted databases with managed services:

| Development (K8s pods) | Production (AWS Managed) |
|---|---|
| MySQL StatefulSet | Amazon RDS MySQL |
| Redis Deployment | Amazon ElastiCache Redis |
| MongoDB StatefulSet | Amazon DocumentDB |
| Kafka StatefulSet | Amazon MSK |

Managed services provide: automated backups, multi-AZ failover, patching, monitoring, and scaling — things you'd have to build yourself with StatefulSets.

---

## 8. Security Considerations

### 8.1 Container Security
- Non-root user in all Dockerfiles
- Multi-stage builds exclude source code and build tools
- Minimal base images (Alpine, slim) reduce attack surface
- No secrets in Docker images (use env vars at runtime)

### 8.2 Frontend Security
- Nginx adds security headers (X-Frame-Options, X-Content-Type-Options, X-XSS-Protection)
- OAuth2 routes proxied to backend (tokens never exposed in browser URL bar for long)
- Static assets served with immutable cache headers
- SPA fallback prevents directory listing

### 8.3 K8s Security
- Secrets stored in K8s Secrets (encrypted at rest with etcd encryption)
- In production: use sealed-secrets, external-secrets-operator, or AWS Secrets Manager
- RBAC for cluster access control
- Network policies to restrict pod-to-pod communication

### 8.4 AWS Security
- Private subnets for EKS nodes and databases
- Security groups restrict access to VPC CIDR only
- IAM roles for EKS nodes (principle of least privilege)
- RDS deletion protection in production
- ECR image scanning for vulnerabilities

---

## 9. Monitoring and Observability

### 9.1 Monitoring Stack

```
┌─────────────────────────────────────────────────────────────────┐
│              Observability Stack                                  │
│                                                                  │
│  Metrics:  Prometheus → Grafana                                   │
│  ├── JVM metrics (Micrometer + Actuator)                         │
│  ├── Kafka consumer lag                                           │
│  ├── Request latency (P50, P95, P99)                             │
│  └── Cost per task                                                │
│                                                                  │
│  Logs:     Structured JSON → ELK / CloudWatch                     │
│  ├── Log correlation via taskId                                   │
│  ├── Error alerting                                               │
│  └── Search across services                                       │
│                                                                  │
│  Traces:   OpenTelemetry (future)                                 │
│  ├── Distributed tracing across Java ↔ Kafka ↔ Python            │
│  ├── Request waterfall visualization                              │
│  └── Latency bottleneck identification                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 10. Cost Management

### 10.1 AWS Cost Breakdown

| Resource | Instance | Monthly Cost (est.) |
|---|---|---|
| EKS Control Plane | Managed | $73 |
| EKS Node Group | 3x t3.medium | ~$100 |
| RDS MySQL | t3.small | ~$25 |
| ElastiCache Redis | t3.small | ~$25 |
| NAT Gateway | 1 AZ | ~$32 |
| ALB | 1 | ~$16 |
| **Total** | | **~$270/mo** |

### 10.2 Cost Optimization
- Use `npm run destroy` when not using the cluster
- Use Spot instances for non-critical workloads
- Right-size instances based on actual usage
- Use reserved instances for long-running production workloads

---

## 11. Hands-On Exercises

### Exercise 1: Docker Compose Full Stack
```bash
cd voyageai-backend
docker-compose up -d
# Verify: curl http://localhost:3000 (frontend chatbot UI)
# Verify: curl http://localhost:8081/actuator/health (backend)
# Verify: curl http://localhost:3000/api/planning/status/test (API proxy)
docker-compose down
```

### Exercise 2: Rebuild Individual Services
```bash
# Rebuild only the frontend after code changes
docker-compose build --no-cache frontend
docker-compose up -d frontend

# Rebuild only the Java backend
docker-compose build --no-cache java-backend
docker-compose up -d java-backend
```

### Exercise 3: Deploy to Minikube
```bash
minikube start
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/
kubectl -n voyageai get pods
# Port-forward: kubectl -n voyageai port-forward svc/frontend 3000:80
```

### Exercise 4: Add HPA (Horizontal Pod Autoscaler)
Create an HPA that scales `java-backend` from 2 to 10 replicas based on CPU usage:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: java-backend-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: java-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

### Exercise 5: Add Prometheus Monitoring
Install Prometheus Operator and Grafana via Helm:
```bash
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
```
Create a ServiceMonitor for the Java backend's Actuator metrics endpoint.

### Exercise 6: CI/CD Pipeline
Create a GitHub Actions workflow that:
1. Builds Docker images on push to main
2. Pushes to ECR
3. Updates K8s deployments with new image tags
4. Runs smoke tests against the deployed service

### Exercise 7: SSL/TLS with cert-manager
Install cert-manager and create a ClusterIssuer for Let's Encrypt. Update the Ingress to use HTTPS with automatic certificate renewal.

---

*Module 15 brings the entire VoyageAI system from development to deployment-ready. Docker Compose provides instant local development with Nginx-proxied frontend and OAuth2 support, Kubernetes manifests enable production-grade orchestration, and AWS CDK creates cloud infrastructure with a single command. The progression from `docker-compose up` to `cdk deploy` mirrors the real journey from prototype to production.*
