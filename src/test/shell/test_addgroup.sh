#!/bin/bash
set -e

source "$(dirname "$0")/.env"


GROUP_NAME="$GROUP_NAME"
ADDGROUP_FILE="$(dirname "$0")/json/addgroup.json"
TMP_ADDGROUP_FILE="$(dirname "$0")/json/addgroup_tmp.json"
cat "$ADDGROUP_FILE" | sed "s/\${GROUP_NAME}/$GROUP_NAME/g" > "$TMP_ADDGROUP_FILE"


# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details
REQUEST_DETAILS="POST $SCIM2_SERVER_URL/Groups\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $(cat "$TMP_ADDGROUP_FILE")"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X POST "$SCIM2_SERVER_URL/Groups" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d @"$TMP_ADDGROUP_FILE")

# Log request and response
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"

rm -f "$TMP_ADDGROUP_FILE"


if [ "$RESPONSE" -eq 201 ]; then
  echo "PASS: Group created successfully."
else
  echo "FAIL: Group creation failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
