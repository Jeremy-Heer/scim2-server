#!/bin/bash
set -e
source "$(dirname "$0")/.env"

USER_NAME="$USER_NAME"

# Properly encode double quotes in filter value using jq
USER_FILTER="userName eq \"$USER_NAME\""
ENCODED_USER_FILTER=$(printf '%s' "$USER_FILTER" | jq -sRr @uri)

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for user ID fetch
REQUEST_DETAILS_USER_ID="GET $SCIM2_SERVER_URL/Users?filter=$ENCODED_USER_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

USER_ID=$(curl -s -X GET "$SCIM2_SERVER_URL/Users?filter=$ENCODED_USER_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | jq -r '.Resources[0].id')

# Log user ID fetch
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (User ID fetch):\n$REQUEST_DETAILS_USER_ID" >> "$LOG_FILE"
echo -e "USER_ID: $USER_ID" >> "$LOG_FILE"


# Use a filter that finds groups containing a member with value equal to USER_ID

# Prepare request details for group membership check
GROUP_FILTER="members.value eq \\\"$USER_ID\\\""
POST_JSON='{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:SearchRequest"],
  "filter": "'${GROUP_FILTER}'",
  "startIndex": 1,
  "count": 10
}'
REQUEST_DETAILS_GROUP="POST $SCIM2_SERVER_URL/Groups/.search\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $POST_JSON"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X POST "$SCIM2_SERVER_URL/Groups/.search" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d "$POST_JSON")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS_GROUP" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"

if [ "$RESPONSE" -eq 200 ] && grep -q "$USER_ID" response.json; then
  echo "PASS: User is a member of a group (POST filter)."
else
  echo "FAIL: User not found in any group (POST filter). HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
