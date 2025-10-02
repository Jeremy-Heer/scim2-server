#!/bin/bash
set -e

source "$(dirname "$0")/.env"
GROUP_NAME="$GROUP_NAME"

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for group ID fetch
REQUEST_DETAILS_ID="GET $SCIM2_SERVER_URL/Groups?filter=$(printf 'displayName eq \"%s\"' "$GROUP_NAME" | jq -sRr @uri)\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

GROUP_ID=$(curl -s -X GET "$SCIM2_SERVER_URL/Groups?filter=$(printf 'displayName eq \"%s\"' "$GROUP_NAME" | jq -sRr @uri)" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | grep -o '"id":"[^"]*"' | head -1 | cut -d':' -f2 | tr -d '"')

# Log group ID fetch
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (Group ID fetch):\n$REQUEST_DETAILS_ID" >> "$LOG_FILE"
echo -e "GROUP_ID: $GROUP_ID" >> "$LOG_FILE"

# Prepare request details for group delete
REQUEST_DETAILS="DELETE $SCIM2_SERVER_URL/Groups/$GROUP_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X DELETE "$SCIM2_SERVER_URL/Groups/$GROUP_ID" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"


if [ "$RESPONSE" -eq 204 ]; then
  echo "PASS: Group deleted successfully."
else
  echo "FAIL: Group deletion failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
