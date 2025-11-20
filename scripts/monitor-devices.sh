#!/bin/bash

# Script to continuously monitor device temperatures
# Queries the API every 100ms and updates the display

set -e

API_URL="${API_URL:-http://localhost:8080}"
ENDPOINT="$API_URL/devices/temperatures"
REFRESH_INTERVAL=1  # 1 second

# Colors for better visibility
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Trap Ctrl+C for graceful exit
trap 'echo -e "\n${YELLOW}Monitoring stopped.${NC}"; exit 0' INT TERM

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   Device Temperature Monitor${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}Endpoint:${NC} $ENDPOINT"
echo -e "${GREEN}Refresh:${NC} Every ${REFRESH_INTERVAL}s (100ms)"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Check if jq is available
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is required but not installed.${NC}"
    echo "Install with: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
fi

# Wait for API to be ready
echo "Waiting for API to be available..."
until curl -sf "$API_URL/actuator/health" > /dev/null 2>&1; do
    sleep 1
done
echo -e "${GREEN}API is ready!${NC}\n"

# Continuous monitoring loop
while true; do
    # Clear screen and position cursor at top
    clear

    # Header
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}   Device Temperature Monitor${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "Updated: $(date '+%Y-%m-%d %H:%M:%S.%3N')"
    echo -e "${BLUE}========================================${NC}\n"

    # Fetch and display data
    RESPONSE=$(curl -sf "$ENDPOINT" 2>/dev/null || echo '[]')

    if [ "$RESPONSE" = "[]" ] || [ -z "$RESPONSE" ]; then
        echo -e "${YELLOW}No devices found. Send some telemetry data:${NC}"
        echo -e "${GREEN}curl -X POST $API_URL/telemetry \\${NC}"
        echo -e "${GREEN}  -H 'Content-Type: application/json' \\${NC}"
        echo -e "${GREEN}  -d '{\"deviceId\": 1, \"temperature\": 22.5, \"date\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}'${NC}"
    else
        # Parse and display devices
        echo -e "${GREEN}Device ID | Measurement     | Last Update${NC}"
        echo -e "${BLUE}----------+-----------------+-------------------------${NC}"

        echo "$RESPONSE" | jq -r '.[] | "\(.deviceId)\t\(.measurement)\t\(.date)"' | \
        while IFS=$'\t' read -r device_id temp last_update; do
            # Format timestamp
            formatted_date=$(date -j -f "%Y-%m-%dT%H:%M:%S" "${last_update%.*}" "+%Y-%m-%d %H:%M:%S" 2>/dev/null || echo "$last_update")
            printf "%-10s| %-16s| %s\n" "$device_id" "$temp" "$formatted_date"
        done

        echo -e "\n${BLUE}Total devices: $(echo "$RESPONSE" | jq 'length')${NC}"
    fi

    echo -e "\n${YELLOW}Press Ctrl+C to stop monitoring${NC}"

    # Wait before next refresh
    sleep $REFRESH_INTERVAL
done
