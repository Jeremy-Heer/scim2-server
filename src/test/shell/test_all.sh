#!/bin/bash
set -e
source "$(dirname "$0")/.env"



# Log file for requests and responses
LOG_FILE="$(dirname "$0")/test_shell_requests.log"

# 1. Create user and group
echo -e "==== $(date) ====\nSTART: test_adduser.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_adduser.sh"
echo -e "END: test_adduser.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."
echo -e "==== $(date) ====\nSTART: test_addgroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_addgroup.sh"
echo -e "END: test_addgroup.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 2. Get user and group by ID
echo -e "==== $(date) ====\nSTART: test_getuser.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_getuser.sh"
echo -e "END: test_getuser.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."
echo -e "==== $(date) ====\nSTART: test_getgroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_getgroup.sh"
echo -e "END: test_getgroup.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 3. Search user and group
echo -e "==== $(date) ====\nSTART: test_searchuser.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_searchuser.sh"
echo -e "END: test_searchuser.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."
echo -e "==== $(date) ====\nSTART: test_searchgroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_searchgroup.sh"
echo -e "END: test_searchgroup.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 4. Filter user and group
echo -e "==== $(date) ====\nSTART: test_filteruser.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_filteruser.sh"
echo -e "END: test_filteruser.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."
echo -e "==== $(date) ====\nSTART: test_filtergroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_filtergroup.sh"
echo -e "END: test_filtergroup.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 5. Patch user and group
echo -e "==== $(date) ====\nSTART: test_patchuser.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_patchuser.sh"
echo -e "END: test_patchuser.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."
echo -e "==== $(date) ====\nSTART: test_patchgroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_patchgroup.sh"
echo -e "END: test_patchgroup.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 6. Add user to group
echo -e "==== $(date) ====\nSTART: test_addusertogroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_addusertogroup.sh"
echo -e "END: test_addusertogroup.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 7. Check group membership (GET and POST)
echo -e "==== $(date) ====\nSTART: test_checkgroupmembership_get.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_checkgroupmembership_get.sh"
echo -e "END: test_checkgroupmembership_get.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."
echo -e "==== $(date) ====\nSTART: test_checkgroupmembership_post.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_checkgroupmembership_post.sh"
echo -e "END: test_checkgroupmembership_post.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 8. Remove user from group
echo -e "==== $(date) ====\nSTART: test_removeuserfromgroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_removeuserfromgroup.sh"
echo -e "END: test_removeuserfromgroup.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."


# 9. Delete user and group
echo -e "==== $(date) ====\nSTART: test_deleteuser.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_deleteuser.sh"
echo -e "END: test_deleteuser.sh" >> "$LOG_FILE"
read -p "Press Enter to continue..."
echo -e "==== $(date) ====\nSTART: test_deletegroup.sh" >> "$LOG_FILE"
"$(dirname "$0")/test_deletegroup.sh"
echo -e "END: test_deletegroup.sh" >> "$LOG_FILE"
read -p "Press Enter to finish."
