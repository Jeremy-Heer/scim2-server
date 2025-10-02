#!/bin/bash
set -e

source "$(dirname "$0")/.env"


# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details
REQUEST_DETAILS="POST $SCIM2_SERVER_URL/Users\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json\nBody: $(cat $(dirname "$0")/json/adduser.json)"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X POST "$SCIM2_SERVER_URL/Users" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json" \
  -d @"$(dirname "$0")/json/adduser.json")

# Log request and response
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"


if [ "$RESPONSE" -eq 201 ]; then
  echo "PASS: User created successfully."
else
  echo "FAIL: User creation failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
