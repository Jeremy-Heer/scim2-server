#!/bin/bash
set -e
source "$(dirname "$0")/.env"

USER_NAME="$USER_NAME"
GROUP_NAME="$GROUP_NAME"

# Properly encode double quotes in filter values using jq
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


# Filter for groups containing a member with value equal to USER_ID

# Prepare request details for group membership check
GROUP_FILTER="members.value eq \"$USER_ID\""
ENCODED_GROUP_FILTER=$(printf '%s' "$GROUP_FILTER" | jq -sRr @uri)
REQUEST_DETAILS_GROUP="GET $SCIM2_SERVER_URL/Groups?filter=$ENCODED_GROUP_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X GET "$SCIM2_SERVER_URL/Groups?filter=$ENCODED_GROUP_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS_GROUP" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"

if [ "$RESPONSE" -eq 200 ] && grep -q "$USER_ID" response.json; then
  echo "PASS: User is a member of a group (GET filter)."
else
  echo "FAIL: User not found in any group (GET filter). HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
