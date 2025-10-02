#!/bin/bash

source "$(dirname "$0")/.env"

# Build and encode the filter string
FILTER="userName eq \"$USER_NAME\""
ENCODED_FILTER=$(printf '%s' "$FILTER" | jq -sRr @uri)


# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for user filter
REQUEST_DETAILS="GET $SCIM2_SERVER_URL/Users?filter=$ENCODED_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X GET "$SCIM2_SERVER_URL/Users?filter=$ENCODED_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json")

# Log request and response
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"

if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: User filter successful."
else
  echo "FAIL: User filter failed. HTTP $RESPONSE"
fi
rm -f response.json
rm -f response.json
