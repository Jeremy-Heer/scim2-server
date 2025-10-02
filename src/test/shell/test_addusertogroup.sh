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

GROUP_FILTER="displayName eq \"$GROUP_NAME\""
ENCODED_GROUP_FILTER=$(printf '%s' "$GROUP_FILTER" | jq -sRr @uri)

# Prepare request details for group ID fetch
REQUEST_DETAILS_GROUP_ID="GET $SCIM2_SERVER_URL/Groups?filter=$ENCODED_GROUP_FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

GROUP_ID=$(curl -s -X GET "$SCIM2_SERVER_URL/Groups?filter=$ENCODED_GROUP_FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | jq -r '.Resources[0].id')

# Log group ID fetch
echo -e "REQUEST (Group ID fetch):\n$REQUEST_DETAILS_GROUP_ID" >> "$LOG_FILE"
echo -e "GROUP_ID: $GROUP_ID" >> "$LOG_FILE"

# Update patch JSON with correct user ID
PATCH_FILE="$(dirname "$0")/json/patch_addusertogroup.json"
TMP_PATCH_FILE="$(dirname "$0")/json/patch_addusertogroup_tmp.json"
cat "$PATCH_FILE" | sed "s/2819c223-7f76-453a-919d-413861904646/$USER_ID/g" > "$TMP_PATCH_FILE"


# Prepare request details for patch
REQUEST_DETAILS_PATCH="PATCH $SCIM2_SERVER_URL/Groups/$GROUP_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $(cat "$TMP_PATCH_FILE")"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X PATCH "$SCIM2_SERVER_URL/Groups/$GROUP_ID" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d @"$TMP_PATCH_FILE")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS_PATCH" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"

rm -f "$TMP_PATCH_FILE"

if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: User added to group."
else
  echo "FAIL: Add user to group failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
