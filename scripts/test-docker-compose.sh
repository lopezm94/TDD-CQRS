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

# Call bootstrap script to wait for application health
./scripts/bootstrap-app.sh || {
    echo -e "${RED}❌ Application failed to become healthy${NC}"
    echo -e "${YELLOW}Application logs:${NC}"
    docker-compose logs app
    exit 1
}

# Show status
echo -e "\n${GREEN}🎉 Docker Compose setup is working!${NC}"
docker-compose ps

echo -e "\n${GREEN}✅ All checks passed${NC}"
