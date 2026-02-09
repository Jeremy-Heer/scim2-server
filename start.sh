#!/bin/bash

# SCIM2 Server Startup Script
# Usage: ./start.sh [path/to/.env]

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default env file
ENV_FILE="${1:-.env}"

echo -e "${GREEN}Starting SCIM2 Server...${NC}"

# Check if env file exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}Warning: Environment file '$ENV_FILE' not found.${NC}"
    echo -e "${YELLOW}Using default values from application.properties${NC}"
else
    echo -e "${GREEN}Loading environment variables from: $ENV_FILE${NC}"
    
    # Load and export environment variables
    # Skip empty lines and comments
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip empty lines and comments
        if [[ -z "$line" ]] || [[ "$line" =~ ^[[:space:]]*# ]]; then
            continue
        fi
        
        # Export the variable
        if [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)=(.*)$ ]]; then
            var_name="${BASH_REMATCH[1]}"
            var_value="${BASH_REMATCH[2]}"
            
            # Remove quotes if present
            var_value="${var_value%\"}"
            var_value="${var_value#\"}"
            var_value="${var_value%\'}"
            var_value="${var_value#\'}"
            
            export "$var_name=$var_value"
            echo -e "  ${GREEN}✓${NC} Exported: $var_name"
        fi
    done < "$ENV_FILE"
fi

# Find the JAR file
JAR_FILE=$(find target -name "scim2-server-*.jar" -not -name "*-original.jar" 2>/dev/null | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found in target/ directory${NC}"
    echo -e "${YELLOW}Please build the application first: mvn clean package${NC}"
    exit 1
fi

echo -e "${GREEN}Found JAR: $JAR_FILE${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Starting application...${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Start the application
java -jar "$JAR_FILE"
