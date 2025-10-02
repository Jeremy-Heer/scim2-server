
#!/bin/bash
set -e
source "$(dirname "$0")/.env"




# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# Prepare request details for group ID fetch
REQUEST_DETAILS_ID="GET $SCIM2_SERVER_URL/Groups?filter=$FILTER\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

FILTER_RESPONSE=$(curl -s -w "%{http_code}" -o filter_response.json \
  -X GET "$SCIM2_SERVER_URL/Groups?filter=$FILTER" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json")

# Log group ID fetch
echo -e "==== $(date) ====" >> "$LOG_FILE"
echo -e "REQUEST (Group ID fetch):\n$REQUEST_DETAILS_ID" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $FILTER_RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat filter_response.json)" >> "$LOG_FILE"

if [ "$FILTER_RESPONSE" -ne 200 ]; then
  echo "FAIL: Filter GET for group failed. HTTP $FILTER_RESPONSE"
  if [ -s filter_response.json ]; then
    cat filter_response.json
  fi
  rm -f filter_response.json
  exit 1
fi

GROUP_ID=$(grep -o '"id":"[^"]*"' filter_response.json | head -1 | cut -d':' -f2 | tr -d '"')

rm -f filter_response.json
if [ -z "$GROUP_ID" ]; then
  echo "FAIL: No group ID found in filter response."
  exit 2
fi


# Prepare request details for group fetch
REQUEST_DETAILS="GET $SCIM2_SERVER_URL/Groups/$GROUP_ID\nAuthorization: Bearer $SCIM2_BEARER_TOKEN\nContent-Type: application/scim+json"

# Perform request and log
RESPONSE=$(curl -s -o response.json -w "%{http_code}" \
  -X GET "$SCIM2_SERVER_URL/Groups/$GROUP_ID" \
  -H "Authorization: Bearer $SCIM2_BEARER_TOKEN" \
  -H "Content-Type: application/scim+json")

# Log request and response
echo -e "REQUEST:\n$REQUEST_DETAILS" >> "$LOG_FILE"
echo -e "RESPONSE CODE: $RESPONSE" >> "$LOG_FILE"
echo -e "RESPONSE BODY:\n$(cat response.json)" >> "$LOG_FILE"


if [ "$RESPONSE" -eq 200 ]; then
  echo "PASS: Get group successful."
else
  echo "FAIL: Get group failed. HTTP $RESPONSE"
  cat response.json
fi
rm -f response.json
