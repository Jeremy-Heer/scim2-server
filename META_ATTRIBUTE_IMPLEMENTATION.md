# SCIM Meta Attribute Implementation for LDAP Backend

## Overview

This document describes the implementation of SCIM meta attribute support for the LDAP backend. The meta attribute is a complex attribute that provides resource metadata including creation time, last modification time, resource type, version, and location.

## Implementation Approach

**LDAP Operational Attributes**: The implementation uses LDAP's standard operational attributes `createTimestamp` and `modifyTimestamp` for SCIM `meta.created` and `meta.lastModified` respectively. These attributes are automatically maintained by the LDAP server and require no additional schema or storage overhead.

## Meta Attribute Fields

| SCIM Field | LDAP Mapping | Type | Notes |
|------------|--------------|------|-------|
| `meta.resourceType` | `scimResourceType` | Custom attribute | Stored as "User" or "Group" |
| `meta.created` | `createTimestamp` | Operational attribute | Automatically maintained by LDAP |
| `meta.lastModified` | `modifyTimestamp` | Operational attribute | Automatically maintained by LDAP |
| `meta.version` | `scimVersion` | Custom attribute | ETag for optimistic concurrency |
| `meta.location` | Computed | N/A | Constructed from base URL and resource ID |

## Advantages of Using Operational Attributes

1. **Automatic Maintenance**: LDAP server automatically maintains these timestamps
2. **Guaranteed Accuracy**: Timestamps reflect actual LDAP entry creation/modification
3. **No Schema Overhead**: No need for custom timestamp attributes
4. **Always Present**: Operational attributes are always available on entries
5. **Atomic Updates**: Timestamps are updated atomically with entry modifications

## LDAP Schema Requirements

Only minimal custom attributes are needed:

```ldif
# Resource type
attributeTypes: ( 1.3.6.1.4.1.99999.1.40
  NAME 'scimResourceType'
  DESC 'SCIM Resource Type (User, Group, etc.)'
  EQUALITY caseIgnoreMatch
  SYNTAX 1.3.6.1.4.1.1466.115.121.1.15
  SINGLE-VALUE )

# Version (ETag)
attributeTypes: ( 1.3.6.1.4.1.99999.1.41
  NAME 'scimVersion'
  DESC 'SCIM Resource Version (ETag)'
  EQUALITY caseIgnoreMatch
  SYNTAX 1.3.6.1.4.1.1466.115.121.1.15
  SINGLE-VALUE )
```

## Code Implementation

### Reading Timestamps

The `ScimLdapAttributeMapper` reads timestamps from LDAP operational attributes:

```java
// Created timestamp from LDAP operational attribute
String createTimestamp = entry.getAttributeValue("createTimestamp");
if (createTimestamp != null) {
    Calendar created = parseLdapTimestamp(createTimestamp);
    if (created != null) {
        meta.setCreated(created);
    }
}

// Modified timestamp from LDAP operational attribute
String modifyTimestamp = entry.getAttributeValue("modifyTimestamp");
if (modifyTimestamp != null) {
    Calendar lastModified = parseLdapTimestamp(modifyTimestamp);
    if (lastModified != null) {
        meta.setLastModified(lastModified);
    }
}
```

### Timestamp Format Conversion

LDAP uses Generalized Time format (YYYYMMDDHHmmss.SSSZ), which is parsed using the `parseLdapTimestamp()` helper method that supports multiple formats:

```java
private Calendar parseLdapTimestamp(String ldapTimestamp) {
    // Supports formats: YYYYMMDDHHmmss.SSSZ, YYYYMMDDHHmmssZ, YYYYMMDDHHmmss'Z'
    // Returns Calendar in UTC timezone
}
```

## Example LDAP Entry

```ldif
dn: entryUUID=68e73a1d-6a76-4ddc-9dac-79c01800ed0f,ou=Users,dc=example,dc=com
objectClass: inetOrgPerson
objectClass: scimUser
uid: bjensen
cn: Barbara Jensen
sn: Jensen
givenName: Barbara
displayName: Barbara Jensen
mail: bjensen@example.com
scimResourceType: User
scimVersion: W/"1"
# Operational attributes (automatically maintained by LDAP):
createTimestamp: 20251220191045.123Z
modifyTimestamp: 20251220192030.456Z
entryUUID: 68e73a1d-6a76-4ddc-9dac-79c01800ed0f
```

## SCIM Response Example

```json
{
  "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
  "id": "68e73a1d-6a76-4ddc-9dac-79c01800ed0f",
  "userName": "bjensen",
  "name": {
    "familyName": "Jensen",
    "givenName": "Barbara",
    "formatted": "Barbara Jensen"
  },
  "displayName": "Barbara Jensen",
  "emails": [
    {
      "value": "bjensen@example.com",
      "type": "work",
      "primary": true
    }
  ],
  "meta": {
    "resourceType": "User",
    "created": "2025-12-20T19:10:45.123Z",
    "lastModified": "2025-12-20T19:20:30.456Z",
    "version": "W/\"1\"",
    "location": "https://example.com/scim/v2/Users/68e73a1d-6a76-4ddc-9dac-79c01800ed0f"
  }
}
```

## Installation and Testing

### 1. Schema Installation

Install the updated schema in PingDirectory:

```bash
# Copy schema file to PingDirectory
cp src/main/resources/schema/99-scim2-schema.ldif \
   /path/to/pingdirectory/config/schema/

# Restart PingDirectory to load new schema
/path/to/pingdirectory/bin/stop-server
/path/to/pingdirectory/bin/start-server
```

### 2. Request Operational Attributes

Ensure LDAP searches request operational attributes:

```java
SearchRequest searchRequest = new SearchRequest(
    baseDN,
    SearchScope.SUB,
    filter,
    "*",  // All user attributes
    "+"   // All operational attributes (including createTimestamp, modifyTimestamp)
);
```

The `LdapRepository` already includes operational attributes in all search requests.

### 3. Testing Checklist

- [ ] Create new user via SCIM API
- [ ] Verify `meta.created` matches LDAP `createTimestamp`
- [ ] Update user via SCIM API
- [ ] Verify `meta.lastModified` matches LDAP `modifyTimestamp`
- [ ] Verify both timestamps are in ISO 8601 format in SCIM responses
- [ ] Test with PingDirectory operational attributes
- [ ] Verify timestamps persist across server restarts

### 4. Verification Commands

```bash
# Create user
curl -X POST http://localhost:9080/scim/v2/Users \
  -H "Authorization: Bearer scim-token-123" \
  -H "Content-Type: application/scim+json" \
  -d @test-user.json

# Get user and verify meta
curl -X GET http://localhost:9080/scim/v2/Users/{id} \
  -H "Authorization: Bearer scim-token-123" \
  | jq '.meta'

# Check LDAP entry
ldapsearch -h localhost -p 2389 \
  -D "cn=Directory Manager" -w password \
  -b "ou=Users,dc=example,dc=com" \
  "(entryUUID={id})" \
  createTimestamp modifyTimestamp scimResourceType scimVersion
```

## Files Modified

1. `src/main/resources/schema/99-scim2-schema.ldif`
   - Removed custom `scimCreated` and `scimLastModified` attribute definitions
   - Added note about using LDAP operational attributes

2. `src/main/java/com/scim2/server/scim2_server/service/ldap/ScimLdapAttributeMapper.java`
   - Removed code that writes custom timestamp attributes
   - Simplified timestamp reading to use only operational attributes
   - Retained `parseLdapTimestamp()` helper method for format conversion

## Benefits

- **Simplified Implementation**: No custom timestamp storage needed
- **Better Accuracy**: LDAP server guarantees timestamp accuracy
- **Reduced Maintenance**: Operational attributes are always in sync
- **Standard Compliance**: Uses LDAP operational attributes as intended
- **No Migration Needed**: Works with existing LDAP entries
