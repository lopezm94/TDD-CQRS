#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
APP_HOST="${APP_HOST:-localhost}"
APP_PORT="${APP_PORT:-8080}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-telemetry}"
HEALTH_ENDPOINT="http://${APP_HOST}:${APP_PORT}/actuator/health"
MAX_WAIT_TIME="${MAX_WAIT_TIME:-60}"

echo "🚀 Bootstrapping application..."
echo "   Health endpoint: ${HEALTH_ENDPOINT}"
echo "   Max wait time: ${MAX_WAIT_TIME}s"

# Build JAR
echo -e "\n${YELLOW}📦 Building application JAR...${NC}"
./mvnw clean package -DskipTests || {
    echo -e "${RED}❌ Failed to build JAR${NC}"
    exit 1
}
echo -e "${GREEN}✅ JAR built successfully${NC}"

# Start services
echo -e "\n${YELLOW}🔨 Starting services with docker-compose...${NC}"
docker-compose up -d --build || {
    echo -e "${RED}❌ Failed to start docker-compose${NC}"
    exit 1
}
echo -e "${GREEN}✅ Services started${NC}"

# Wait for application health endpoint (checks every second for up to 60 seconds)
echo -e "\n${YELLOW}⏳ Waiting for application to become healthy (checking every second, max 60s)...${NC}"
timeout 60 bash -c "until curl -sf ${HEALTH_ENDPOINT} | grep -q '\"status\":\"UP\"'; do sleep 1; done" || {
    echo -e "${RED}❌ Application failed to become healthy within 60 seconds${NC}"
    echo -e "${YELLOW}Health endpoint response:${NC}"
    curl -s ${HEALTH_ENDPOINT} || echo "No response"
    exit 1
}

echo -e "${GREEN}✅ Application bootstrap complete${NC}"
