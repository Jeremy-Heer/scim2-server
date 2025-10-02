#!/bin/bash
set -e

source "$(dirname "$0")/.env"

# Fetch GROUP_ID using GROUP_NAME from .env
FILTER="displayName eq \"$GROUP_NAME\""
ENCODED_FILTER=$(printf '%s' "$FILTER" | jq -sRr @uri)

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for group ID fetch
REQUEST_DETAILS_ID="GET $SCIM2_SERVER_URL/Groups?filter=$ENCODED_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

GROUP_ID=$(curl -s -X GET "$SCIM2_SERVER_URL/Groups?filter=$ENCODED_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | jq -r '.Resources[0].id')

# Log group ID fetch
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (Group ID fetch):\n$REQUEST_DETAILS_ID" >> "$LOG_FILE"
echo -e "GROUP_ID: $GROUP_ID" >> "$LOG_FILE"


# Prepare request details for group patch
REQUEST_DETAILS="PATCH $SCIM2_SERVER_URL/Groups/$GROUP_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $(cat $(dirname "$0")/json/patchgroup.json)"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X PATCH "$SCIM2_SERVER_URL/Groups/$GROUP_ID" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d @"$(dirname "$0")/json/patchgroup.json")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"


if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: Group patch successful."
else
  echo "FAIL: Group patch failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
