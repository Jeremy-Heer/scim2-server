#!/bin/bash
set -e
source "$(dirname "$0")/.env"


USER_NAME="$USER_NAME"
ENCODED_USER_NAME=$(echo "$USER_NAME" | sed 's/\"/%22/g')
FILTER="userName%20eq%20%22$ENCODED_USER_NAME%22"

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for user ID fetch
REQUEST_DETAILS_ID="GET $SCIM2_SERVER_URL/Users?filter=$FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

USER_ID=$(curl -s -X GET "$SCIM2_SERVER_URL/Users?filter=$FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | grep -o '"id":"[^"]*"' | head -1 | cut -d':' -f2 | tr -d '"')

# Log user ID fetch
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (User ID fetch):\n$REQUEST_DETAILS_ID" >> "$LOG_FILE"
echo -e "USER_ID: $USER_ID" >> "$LOG_FILE"


# Prepare request details for user fetch
REQUEST_DETAILS="GET $SCIM2_SERVER_URL/Users/$USER_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X GET "$SCIM2_SERVER_URL/Users/$USER_ID" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"


if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: Get user successful."
else
  echo "FAIL: Get user failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
