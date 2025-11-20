#!/bin/bash

set -e

echo "🧪 Testing docker-compose setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}🧹 Cleaning up...${NC}"
    docker-compose down -v
}

# Set trap to cleanup on exit
trap cleanup EXIT

# Build JAR
echo -e "${YELLOW}📦 Building application JAR...${NC}"
./mvnw clean package -DskipTests || {
    echo -e "${RED}❌ Failed to build JAR${NC}"
    exit 1
}
echo -e "${GREEN}✅ JAR built successfully${NC}"

# Build and start services
echo -e "${YELLOW}🔨 Building and starting services...${NC}"
docker-compose up -d --build

# Wait for PostgreSQL
echo -e "${YELLOW}⏳ Waiting for PostgreSQL...${NC}"
timeout 30 bash -c 'until docker-compose exec -T postgres pg_isready -U telemetry; do sleep 1; done' || {
    echo -e "${RED}❌ PostgreSQL failed to start${NC}"
    exit 1
}
echo -e "${GREEN}✅ PostgreSQL is ready${NC}"

# Wait for application (health endpoint will be implemented in future iteration)
echo -e "${YELLOW}⏳ Waiting for application...${NC}"
timeout 60 bash -c 'until curl -f http://localhost:8080/actuator/health 2>/dev/null; do sleep 2; done' || {
    echo -e "${YELLOW}⚠️  Health endpoint not yet implemented (will be added in Iteration 2)${NC}"
    echo -e "${YELLOW}   Checking if application is running...${NC}"

    # Check if app container is running
    if docker-compose ps app | grep -q "Up"; then
        echo -e "${GREEN}✅ Application container is running${NC}"
    else
        echo -e "${RED}❌ Application failed to start${NC}"
        echo -e "${YELLOW}Application logs:${NC}"
        docker-compose logs app
        exit 1
    fi
}

# Show status
echo -e "\n${GREEN}🎉 Docker Compose setup is working!${NC}"
docker-compose ps

echo -e "\n${GREEN}✅ All checks passed${NC}"
