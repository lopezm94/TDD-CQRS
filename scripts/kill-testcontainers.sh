#!/bin/bash

# Kill reusable TestContainers
# TestContainers with .withReuse(true) persist after tests complete
# This script finds and removes them

echo "🧹 Cleaning up reusable TestContainers..."

# Find containers with testcontainers labels
CONTAINERS=$(docker ps -a --filter "label=org.testcontainers" --format "{{.ID}}")

if [ -z "$CONTAINERS" ]; then
    echo "✅ No TestContainers found"
    exit 0
fi

echo "Found TestContainers:"
docker ps -a --filter "label=org.testcontainers" --format "table {{.ID}}\t{{.Image}}\t{{.Status}}"

echo ""
echo "Stopping and removing containers..."
echo "$CONTAINERS" | xargs docker rm -f

echo "✅ TestContainers cleaned up"
