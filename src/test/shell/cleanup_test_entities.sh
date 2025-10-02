#!/bin/bash
set -e
source "$(dirname "$0")/.env"

# Remove all users matching USER_NAME
USER_FILTER="userName eq \"$USER_NAME\""
ENCODED_USER_FILTER=$(printf '%s' "$USER_FILTER" | jq -sRr @uri)

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for user filter
REQUEST_DETAILS_USER_FILTER="GET $SCIM2_SERVER_URL/Users?filter=$ENCODED_USER_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

USER_IDS=$(curl -s -X GET "$SCIM2_SERVER_URL/Users?filter=$ENCODED_USER_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | jq -r '.Resources[].id')

# Log user filter request
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (User Filter):\n$REQUEST_DETAILS_USER_FILTER" >> "$LOG_FILE"
echo -e "USER_IDS: $USER_IDS" >> "$LOG_FILE"


for USER_ID in $USER_IDS; do
  echo "Deleting user: $USER_ID"
  REQUEST_DETAILS_DELETE_USER="DELETE $SCIM2_SERVER_URL/Users/$USER_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"
  RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
    -X DELETE "$SCIM2_SERVER_URL/Users/$USER_ID" \
    -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
    -H "Content-Type: application/scim+json")
  echo -e "REQUEST:\n$REQUEST_DETAILS_DELETE_USER" >> "$LOG_FILE"
  echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
  echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"
  rm -f response.json
done

# Remove all groups matching GROUP_NAME
GROUP_FILTER="displayName eq \"$GROUP_NAME\""
ENCODED_GROUP_FILTER=$(printf '%s' "$GROUP_FILTER" | jq -sRr @uri)

# Prepare request details for group filter
REQUEST_DETAILS_GROUP_FILTER="GET $SCIM2_SERVER_URL/Groups?filter=$ENCODED_GROUP_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

GROUP_IDS=$(curl -s -X GET "$SCIM2_SERVER_URL/Groups?filter=$ENCODED_GROUP_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | jq -r '.Resources[].id')

# Log group filter request
echo -e "REQUEST (Group Filter):\n$REQUEST_DETAILS_GROUP_FILTER" >> "$LOG_FILE"
echo -e "GROUP_IDS: $GROUP_IDS" >> "$LOG_FILE"


for GROUP_ID in $GROUP_IDS; do
  echo "Deleting group: $GROUP_ID"
  REQUEST_DETAILS_DELETE_GROUP="DELETE $SCIM2_SERVER_URL/Groups/$GROUP_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"
  RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
    -X DELETE "$SCIM2_SERVER_URL/Groups/$GROUP_ID" \
    -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
    -H "Content-Type: application/scim+json")
  echo -e "REQUEST:\n$REQUEST_DETAILS_DELETE_GROUP" >> "$LOG_FILE"
  echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
  echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"
  rm -f response.json
done

echo "Cleanup complete."
