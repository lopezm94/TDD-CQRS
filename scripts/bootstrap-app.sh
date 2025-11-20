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

# Wait for PostgreSQL
echo -e "\n${YELLOW}⏳ Waiting for PostgreSQL at ${POSTGRES_HOST}:${POSTGRES_PORT}...${NC}"
timeout 30 bash -c "until docker-compose exec -T postgres pg_isready -U ${POSTGRES_USER} 2>/dev/null || pg_isready -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U ${POSTGRES_USER} 2>/dev/null; do sleep 1; done" || {
    echo -e "${RED}❌ PostgreSQL failed to start${NC}"
    exit 1
}
echo -e "${GREEN}✅ PostgreSQL is ready${NC}"

# Wait for application health endpoint
echo -e "\n${YELLOW}⏳ Waiting for application health endpoint...${NC}"
timeout ${MAX_WAIT_TIME} bash -c "until curl -sf ${HEALTH_ENDPOINT} | grep -q '\"status\":\"UP\"'; do sleep 2; done" || {
    echo -e "${RED}❌ Application health check failed${NC}"
    echo -e "${YELLOW}Health endpoint response:${NC}"
    curl -s ${HEALTH_ENDPOINT} || echo "No response"
    exit 1
}
echo -e "${GREEN}✅ Application is healthy${NC}"

# Show health details
echo -e "\n${GREEN}📊 Health status:${NC}"
curl -s ${HEALTH_ENDPOINT} | python3 -m json.tool 2>/dev/null || curl -s ${HEALTH_ENDPOINT}

echo -e "\n${GREEN}✅ Application bootstrap complete${NC}"
