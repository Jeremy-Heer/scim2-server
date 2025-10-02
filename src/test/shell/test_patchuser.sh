#!/bin/bash
set -e

source "$(dirname "$0")/.env"

# Fetch USER_ID using USER_NAME from .env
FILTER="userName eq \"$USER_NAME\""
ENCODED_FILTER=$(printf '%s' "$FILTER" | jq -sRr @uri)

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for user ID fetch
REQUEST_DETAILS_ID="GET $SCIM2_SERVER_URL/Users?filter=$ENCODED_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

USER_ID=$(curl -s -X GET "$SCIM2_SERVER_URL/Users?filter=$ENCODED_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | jq -r '.Resources[0].id')

# Log user ID fetch
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (User ID fetch):\n$REQUEST_DETAILS_ID" >> "$LOG_FILE"
echo -e "USER_ID: $USER_ID" >> "$LOG_FILE"


# Prepare request details for user patch
REQUEST_DETAILS="PATCH $SCIM2_SERVER_URL/Users/$USER_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $(cat $(dirname "$0")/json/patchuser.json)"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X PATCH "$SCIM2_SERVER_URL/Users/$USER_ID" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d @"$(dirname "$0")/json/patchuser.json")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"


if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: User patch successful."
else
  echo "FAIL: User patch failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
