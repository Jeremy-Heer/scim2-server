#!/bin/bash
set -e

source "$(dirname "$0")/.env"

USER_NAME="$USER_NAME"

# Substitute USER_NAME in search JSON
SEARCH_FILE="$(dirname "$0")/json/searchuser.json"
TMP_SEARCH_FILE="$(dirname "$0")/json/searchuser_tmp.json"
cat "$SEARCH_FILE" | sed "s/\${USER_NAME}/$USER_NAME/g" > "$TMP_SEARCH_FILE"

# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details
REQUEST_DETAILS="POST $SCIM2_SERVER_URL/Users/.search\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $(cat "$TMP_SEARCH_FILE")"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X POST "$SCIM2_SERVER_URL/Users/.search" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d @"$TMP_SEARCH_FILE")

# Log request and response
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"

rm -f "$TMP_SEARCH_FILE"

if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: User search successful."
else
  echo "FAIL: User search failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
