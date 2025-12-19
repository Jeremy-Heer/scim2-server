# SCIM2 LDAP Index Configuration for PingDirectory

## Overview

This document specifies the required indexes for optimal SCIM2 LDAP backend performance. These indexes support efficient SCIM filtering, searching, pagination, and reference lookups.

**IMPORTANT:** The SCIM `id` property is mapped directly to the LDAP `entryUUID` operational attribute. PingDirectory automatically indexes `entryUUID`, so no separate scimId index is needed.

## Installation

Execute these commands using `dsconfig` CLI or PingDirectory Admin Console.

## Required Indexes

### 1. Username Index (CRITICAL)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name uid \
  --set index-type:equality \
  --set index-type:substring \
  --set index-entry-limit:4000
```

**Purpose:** Filter by SCIM `userName`  
**Usage:** `userName eq "john.doe"`, `userName sw "john"`  
**Priority:** CRITICAL - Most common filter attribute

---

### 2. Common Name Index (HIGH)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name cn \
  --set index-type:equality \
  --set index-type:substring \
  --set index-entry-limit:4000
```

**Purpose:** Filter by SCIM `displayName` (mapped to cn)  
**Usage:** `displayName co "Smith"`, group name filtering  
**Priority:** HIGH - Common in user and group searches

---

### 4. Surname Index (HIGH)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name sn \
  --set index-type:equality \
  --set index-type:substring \
  --set index-entry-limit:4000
```

**Purpose:** Filter by SCIM `name.familyName`  
**Usage:** `name.familyName eq "Smith"`, `name.familyName sw "Sm"`  
**Priority:** HIGH - Common filter in user searches

---

### 5. Given Name Index (MEDIUM)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name givenName \
  --set index-type:equality \
  --set index-type:substring \
  --set index-entry-limit:4000
```

**Purpose:** Filter by SCIM `name.givenName`  
**Usage:** `name.givenName eq "John"`  
**Priority:** MEDIUM - Moderately common filter

---

### 6. Email Address Index (HIGH)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name mail \
  --set index-type:equality \
  --set index-type:substring \
  --set index-entry-limit:4000
```

**Purpose:** Filter by primary email address  
**Usage:** `emails.value eq "user@example.com"`, `emails.value co "@example.com"`  
**Priority:** HIGH - Very common filter attribute

---

### 7. SCIM Emails JSON Index (MEDIUM)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name scimEmails \
  --set index-type:equality \
  --set index-entry-limit:4000
```

**Purpose:** Filter complex email queries using JSON object filters  
**Usage:** `emails[type eq "work" and primary eq true].value`  
**Priority:** MEDIUM - Used for complex email filtering

---

### 8. Active Status Index (MEDIUM)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name scimActive \
  --set index-type:equality \
  --set index-entry-limit:4000
```

**Purpose:** Filter by SCIM `active` status  
**Usage:** `active eq true`, `active eq false`  
**Priority:** MEDIUM - Common filter for active accounts

---

### 9. External ID Index (LOW)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name scimExternalId \
  --set index-type:equality \
  --set index-entry-limit:4000
```

**Purpose:** Filter by SCIM `externalId`  
**Usage:** `externalId eq "external-system-123"`  
**Priority:** LOW - Only if external system integration is used

---

### 10. Group Member Index (CRITICAL)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name member \
  --set index-type:equality \
  --set index-entry-limit:4000
```

**Purpose:** Efficient group membership queries  
**Usage:** Find groups containing specific user DN  
**Priority:** CRITICAL - Required for group operations and bulk queries

---

### 11. Virtual MemberOf Index (CRITICAL)

**Note:** PingDirectory automatically indexes `memberOf` as a virtual attribute when using the member attribute. No manual configuration needed, but verify it's enabled:

```bash
dsconfig get-virtual-attribute-prop \
  --name "Virtual memberOf" \
  --property enabled
```

**Purpose:** Reverse group membership lookup (user → groups)  
**Usage:** SCIM `groups` field on User resources  
**Priority:** CRITICAL - Required for user group membership

---

### 12. Employee Number Index (MEDIUM)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name employeeNumber \
  --set index-type:equality \
  --set index-entry-limit:4000
```

**Purpose:** Filter by Enterprise User `employeeNumber`  
**Usage:** `urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber eq "E12345"`  
**Priority:** MEDIUM - If using Enterprise User extension

---

### 13. Manager Index (LOW)

```bash
dsconfig create-local-db-index \
  --backend-name userRoot \
  --index-name manager \
  --set index-type:equality \
  --set index-entry-limit:4000
```

**Purpose:** Filter by manager DN  
**Usage:** Find direct reports of a manager  
**Priority:** LOW - Less common filter

---

### 14. Object Class Index (AUTOMATIC)

**Note:** PingDirectory automatically maintains `objectClass` index. Verify it exists:

```bash
dsconfig get-local-db-index-prop \
  --backend-name userRoot \
  --index-name objectClass
```

**Purpose:** Filter by resource type (User vs Group)  
**Priority:** AUTOMATIC - Pre-configured in PingDirectory

---

### 15. Entry UUID Index (AUTOMATIC)

**Note:** PingDirectory automatically indexes `entryUUID`. This is critical for DN resolution when using Name-with-entryUUID control:

```bash
dsconfig get-local-db-index-prop \
  --backend-name userRoot \
  --index-name entryUUID
```

**Purpose:** Resolve DN from entryUUID, reference lookups  
**Priority:** AUTOMATIC - Pre-configured in PingDirectory

---

## Index Verification

After creating indexes, verify they're active and building:

```bash
# List all indexes
dsconfig list-local-db-indexes --backend-name userRoot

# Check index status
ldapsearch -b "cn=userRoot Backend,cn=monitor" \
  "objectClass=ds-local-db-index-monitor-entry" \
  cn ds-index-entry-count ds-index-state

# Verify index for specific attribute
ldapsearch -b "cn=userRoot Backend,cn=monitor" \
  "(&(objectClass=ds-local-db-index-monitor-entry)(cn=uid))" \
  cn ds-index-entry-count ds-index-state

# Verify entryUUID index (automatic)
ldapsearch -b "cn=userRoot Backend,cn=monitor" \
  "(&(objectClass=ds-local-db-index-monitor-entry)(cn=entryUUID))" \
  cn ds-index-entry-count ds-index-state
```

## Index Maintenance

### Rebuild Index (If Needed)

```bash
rebuild-index --baseDN dc=example,dc=com --index uid
rebuild-index --baseDN dc=example,dc=com --index member
rebuild-index --baseDN dc=example,dc=com --index mail
```

**Note:** entryUUID is automatically indexed and maintained by PingDirectory.

### Monitor Index Performance

```bash
# Check index entry limits
ldapsearch -b "cn=userRoot Backend,cn=monitor" \
  "(objectClass=ds-local-db-index-monitor-entry)" \
  cn ds-index-entry-limit-exceeded-count

# View unindexed search statistics
ldapsearch -b "cn=monitor" \
  "(objectClass=ds-unindexed-search-monitor-entry)" \
  ds-unindexed-search-filter
```

## Performance Tuning Recommendations

1. **Index Entry Limits:**
   - Default: 4000 entries per index key
   - Increase for attributes with many duplicate values (e.g., `scimActive`)
   - Monitor `ds-index-entry-limit-exceeded-count`

2. **Substring Indexes:**
   - Only add to frequently filtered text attributes (uid, cn, sn, mail)
   - Avoid on attributes rarely used in substring searches (increases storage)

3. **JSON Indexes:**
   - PingDirectory indexes JSON attributes specially for object filtering
   - Equality index sufficient for JSON object filter matching rules
   - No substring index needed for JSON types

4. **Composite Filters:**
   - Optimize multi-attribute filters: `(&(uid=john*)(scimActive=TRUE))`
   - Ensure all filter attributes are indexed
   - Most specific filter term should be indexed first

5. **Index Cache Size:**
   - Adjust `db-cache-percent` if index cache misses are high
   - Monitor: `ds-db-cache-hit-ratio` (target: >95%)

## Priority Summary

| Priority | Indexes |
|----------|---------|
| CRITICAL | uid, member, entryUUID (auto, used for SCIM id), memberOf (virtual) |
| HIGH | cn, sn, mail |
| MEDIUM | givenName, scimEmails, scimActive, employeeNumber |
| LOW | scimExternalId, manager |
| AUTOMATIC | objectClass, entryUUID |

**Note:** entryUUID is automatically indexed by PingDirectory and serves as the SCIM `id` property. No separate scimId index is needed.

## Bulk Operations Optimization

For large datasets and bulk operations:

1. **Enable Import/Export Index Generation:**
   ```bash
   import-ldif --backendID userRoot \
     --ldifFile users.ldif \
     --generateIndexes
   ```

2. **Pre-load Index Cache:**
   ```bash
   dsconfig set-backend-prop \
     --backend-name userRoot \
     --set prime-internal-nodes-cache:true
   ```

3. **Monitor During Bulk Import:**
   ```bash
   # Watch for unindexed searches during bulk operations
   tail -f logs/access | grep "etime=" | grep "notes=.*UNINDEXED"
   ```

## Additional JSON Index Configuration

For complex SCIM filters on JSON attributes, configure JSON matching rules:

```bash
# Enable JSON object filter matching for scimEmails
dsconfig set-matching-rule-prop \
  --rule-name caseIgnoreJsonQueryMatch \
  --set enabled:true

# Verify JSON matching rule configuration
ldapsearch -b "cn=schema" \
  "(&(objectClass=*)(matchingRules=caseIgnoreJsonQueryMatch))" \
  matchingRules
```

## Troubleshooting

### Unindexed Search Detected

If you see "UNINDEXED" in access logs:

1. Identify the filter: Check access log for filter expression
2. Determine missing index: Analyze filter attributes
3. Create appropriate index
4. Wait for index build completion
5. Re-test query performance

### Index Not Being Used

Possible causes:

1. Index still building (check `ds-index-state`)
2. Filter attribute name mismatch
3. Index entry limit exceeded
4. Query optimizer chose full scan (rare, for small datasets)

Check explain plan:

```bash
ldapsearch --getEffectiveRightsControl \
  "(&(uid=test*)(scimActive=TRUE))" \
  debugsearchindex
```

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-18  
**Compatible With:** PingDirectory 9.x, 10.x
