#!/bin/bash
# DocIx Graceful Shutdown Test Script

echo "🧪 Testing DocIx Graceful Shutdown"
echo "=================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
DOCIX_URL="http://localhost:8081"
ACTUATOR_URL="$DOCIX_URL/actuator"

# Function to check if application is running
check_app_status() {
    local url=$1
    local expected_status=$2

    response=$(curl -s -o /dev/null -w "%{http_code}" $url 2>/dev/null)
    if [ "$response" = "$expected_status" ]; then
        return 0
    else
        return 1
    fi
}

# Function to get system status
get_system_status() {
    curl -s "$DOCIX_URL/api/system/status" 2>/dev/null | jq -r '.status' 2>/dev/null || echo "UNKNOWN"
}

# Function to get active processing tasks
get_active_tasks() {
    curl -s "$DOCIX_URL/api/system/status" 2>/dev/null | jq -r '.activeProcessingTasks' 2>/dev/null || echo "0"
}

echo -e "${YELLOW}Step 1: Checking initial application status${NC}"
if check_app_status "$DOCIX_URL/api/system/status" "200"; then
    status=$(get_system_status)
    tasks=$(get_active_tasks)
    echo -e "${GREEN}✅ Application is running - Status: $status, Active tasks: $tasks${NC}"
else
    echo -e "${RED}❌ Application is not running or not responding${NC}"
    exit 1
fi

echo -e "\n${YELLOW}Step 2: Triggering graceful shutdown${NC}"
echo "💡 You can trigger shutdown using one of these methods:"
echo "   1. Send SIGTERM: kill -TERM <PID>"
echo "   2. Use Actuator endpoint: curl -X POST $ACTUATOR_URL/shutdown"
echo "   3. Press Ctrl+C in the terminal running the application"

echo -e "\n${YELLOW}Triggering shutdown via Actuator endpoint...${NC}"
shutdown_response=$(curl -s -X POST "$ACTUATOR_URL/shutdown" 2>/dev/null)
echo "Shutdown response: $shutdown_response"

echo -e "\n${YELLOW}Step 3: Monitoring shutdown process${NC}"
max_wait=35
waited=0

while [ $waited -lt $max_wait ]; do
    status=$(get_system_status)
    tasks=$(get_active_tasks)

    if [ "$status" = "SHUTTING_DOWN" ]; then
        echo -e "${YELLOW}⏳ Application is shutting down... Active tasks: $tasks (${waited}s)${NC}"
    elif [ "$status" = "SHUTDOWN_COMPLETED" ]; then
        echo -e "${GREEN}✅ Graceful shutdown completed successfully (${waited}s)${NC}"
        break
    elif [ "$status" = "UNKNOWN" ] || ! check_app_status "$DOCIX_URL/api/system/status" "200"; then
        echo -e "${GREEN}✅ Application has stopped responding - shutdown completed (${waited}s)${NC}"
        break
    else
        echo -e "${YELLOW}📊 Status: $status, Active tasks: $tasks (${waited}s)${NC}"
    fi

    sleep 2
    waited=$((waited + 2))
done

if [ $waited -ge $max_wait ]; then
    echo -e "${RED}⚠️ Shutdown monitoring timeout after ${max_wait}s${NC}"
fi

echo -e "\n${YELLOW}Step 4: Final verification${NC}"
if check_app_status "$DOCIX_URL/api/system/status" "200"; then
    echo -e "${RED}❌ Application is still running${NC}"
else
    echo -e "${GREEN}✅ Application has stopped successfully${NC}"
fi

echo -e "\n${GREEN}🎉 Graceful shutdown test completed${NC}"
echo "Check the application logs to see detailed shutdown sequence."
