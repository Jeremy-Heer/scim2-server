#!/bin/bash
set -e
source "$(dirname "$0")/.env"

USER_NAME="$USER_NAME"
GROUP_NAME="$GROUP_NAME"

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for user ID fetch
REQUEST_DETAILS_USER_ID="GET $SCIM2_SERVER_URL/Users?filter=$(printf 'userName eq \"%s\"' "$USER_NAME" | jq -sRr @uri)\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

USER_ID=$(curl -s --no-keepalive -X GET "$SCIM2_SERVER_URL/Users?filter=$(printf 'userName eq \"%s\"' "$USER_NAME" | jq -sRr @uri)" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | grep -o '"id":"[^\"]*"' | head -1 | cut -d':' -f2 | tr -d '"')

# Log user ID fetch
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (User ID fetch):\n$REQUEST_DETAILS_USER_ID" >> "$LOG_FILE"
echo -e "USER_ID: $USER_ID" >> "$LOG_FILE"

# Prepare request details for group ID fetch
REQUEST_DETAILS_GROUP_ID="GET $SCIM2_SERVER_URL/Groups?filter=$(printf 'displayName eq \"%s\"' "$GROUP_NAME" | jq -sRr @uri)\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

GROUP_ID=$(curl -s --no-keepalive -X GET "$SCIM2_SERVER_URL/Groups?filter=$(printf 'displayName eq \"%s\"' "$GROUP_NAME" | jq -sRr @uri)" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" | grep -o '"id":"[^\"]*"' | head -1 | cut -d':' -f2 | tr -d '"')

# Log group ID fetch
echo -e "REQUEST (Group ID fetch):\n$REQUEST_DETAILS_GROUP_ID" >> "$LOG_FILE"
echo -e "GROUP_ID: $GROUP_ID" >> "$LOG_FILE"

# Update patch JSON with correct user ID
PATCH_FILE="$(dirname "$0")/json/patch_removeuserfromgroup.json"
TMP_PATCH_FILE="$(dirname "$0")/json/patch_removeuserfromgroup_tmp.json"
sed "s/\\\${USER_ID}/$USER_ID/g" "$PATCH_FILE" > "$TMP_PATCH_FILE"

# Prepare request details for patch
REQUEST_DETAILS_PATCH="PATCH $SCIM2_SERVER_URL/Groups/$GROUP_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $(cat "$TMP_PATCH_FILE")"

# Perform request and log
RESPONSE=$(curl -s --no-keepalive -o response.json -w "%{http_code}" \
  -X PATCH "$SCIM2_SERVER_URL/Groups/$GROUP_ID" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d @"$TMP_PATCH_FILE")

rm -f "$TMP_PATCH_FILE"

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS_PATCH" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"

if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: User removed from group."
else
  echo "FAIL: Remove user from group failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
