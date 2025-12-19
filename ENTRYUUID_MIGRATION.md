# Migration to entryUUID for SCIM ID Mapping

## Overview

This document describes the migration from using a custom `scimId` attribute to directly mapping SCIM `id` to the LDAP `entryUUID` operational attribute.

## Rationale

**Before:**
- SCIM `id` was stored in a custom `scimId` attribute
- Required additional index maintenance
- Redundant data storage (UUID stored twice: entryUUID + scimId)

**After:**
- SCIM `id` maps directly to LDAP `entryUUID`
- Leverages PingDirectory's automatic `entryUUID` indexing
- Cleaner architecture with no data redundancy
- Proper entryUUID-based DN naming: `entryUUID=<uuid>,<parent-dn>`

## Changes Made

### 1. LdapConnectionService

**File:** `src/main/java/com/scim2/server/scim2_server/service/ldap/LdapConnectionService.java`

- **Changed `findEntryByUuid()`**: Now searches by `entryUUID` instead of `scimId`
  ```java
  Filter filter = Filter.createEqualityFilter("entryUUID", uuid);
  ```

- **Added `buildDnFromUuid()`**: New method to construct DN from UUID for member operations
  ```java
  public String buildDnFromUuid(String uuid, String baseDn) {
      if (ldapProperties.isUseEntryUuidDn()) {
          return "entryUUID=" + uuid + "," + baseDn;
      }
      // Fallback for non-entryUUID mode
  }
  ```

### 2. ScimLdapAttributeMapper

**File:** `src/main/java/com/scim2/server/scim2_server/service/ldap/ScimLdapAttributeMapper.java`

- **Removed scimId attribute mapping** from `userToLdapAttributes()` and `groupToLdapAttributes()`
  - SCIM ID is no longer written to LDAP (entryUUID is automatically set by server)

- **Updated `ldapEntryToUser()`**: Reads SCIM ID from `entryUUID` attribute
  ```java
  String entryUuid = entry.getAttributeValue("entryUUID");
  if (entryUuid != null) {
      user.setId(entryUuid);
  }
  ```

- **Updated `ldapEntryToGroup()`**: Same change for groups
  ```java
  String entryUuid = entry.getAttributeValue("entryUUID");
  if (entryUuid != null) {
      group.setId(entryUuid);
  }
  ```

### 3. LdapRepository

**File:** `src/main/java/com/scim2/server/scim2_server/repository/ldap/LdapRepository.java`

- **`saveUser()` changes:**
  - Removed pre-generation of SCIM ID (let server create entryUUID)
  - After creation, search by `uid` to retrieve entry with server-generated `entryUUID`
  - Request operational attributes (`"+"`) to get `entryUUID`

- **`saveGroup()` changes:**
  - Same pattern as users
  - Use `buildDnFromUuid()` to convert member UUIDs to DNs
  ```java
  String memberDn = connectionService.buildDnFromUuid(
      member.getValue(),
      connectionService.getLdapProperties().getUserBaseDn()
  );
  ```

- **`updateGroup()` changes:**
  - Use `buildDnFromUuid()` for member DN conversion instead of `findEntryByUuid()`

### 4. ScimFilterToLdapConverter

**File:** `src/main/java/com/scim2/server/scim2_server/service/ldap/ScimFilterToLdapConverter.java`

- **Added dependency injection**: Inject `LdapConnectionService` to enable DN conversion
  ```java
  private final LdapConnectionService connectionService;
  
  public ScimFilterToLdapConverter(LdapConnectionService connectionService) {
      this.connectionService = connectionService;
  }
  ```

- **Enhanced `convertEqualFilter()`**: Convert SCIM member.value queries
  ```java
  if ("member".equals(mapping.ldapAttribute) && value != null) {
      // Value is a SCIM ID (entryUUID), convert to DN
      String memberDn = connectionService.buildDnFromUuid(
          value,
          connectionService.getLdapProperties().getUserBaseDn()
      );
      if (memberDn != null) {
          mapping.ldapValue = memberDn;
      }
  }
  ```

### 5. LDAP Schema Updates

**File:** `src/main/resources/schema/99-scim2-schema.ldif`

- **Removed `scimId` attribute definition** (OID 1.3.6.1.4.1.99999.1.1)
- **Updated object classes**: Removed `scimId` from MAY attributes
  - `scimUser`: Now uses only `entryUUID` (operational)
  - `scimGroup`: Now uses only `entryUUID` (operational)
- **Updated documentation**: Added notes about entryUUID mapping
- **Updated examples**: Show `entryUUID` as operational attribute instead of `scimId`

### 6. Index Documentation Updates

**File:** `src/main/resources/schema/README-INDEXES.md`

- **Removed scimId index creation** instructions
- **Added note**: entryUUID is automatically indexed by PingDirectory
- **Updated priority table**: Removed scimId from CRITICAL indexes
- **Updated monitoring examples**: Use `uid` and `entryUUID` for verification
- **Updated rebuild commands**: Removed scimId rebuild instructions

## Migration Path

### For Existing Deployments

If you have existing LDAP data with `scimId` attributes:

1. **Backup your data**
   ```bash
   export-ldif --backendID userRoot --ldifFile backup.ldif
   ```

2. **Deploy updated schema** (removes scimId from object classes)
   ```bash
   cp 99-scim2-schema.ldif /opt/pingdirectory/config/schema/
   dsconfig set-schema-provider-prop --reload
   ```

3. **Optional: Clean up old scimId attributes**
   ```bash
   # Remove scimId from existing entries (if desired)
   ldapmodify <<EOF
   dn: uid=user1,ou=users,dc=example,dc=com
   changetype: modify
   delete: scimId
   EOF
   ```

4. **Remove scimId index** (if previously created)
   ```bash
   dsconfig delete-local-db-index \
     --backend-name userRoot \
     --index-name scimId
   ```

### For New Deployments

1. Deploy the updated schema (no scimId attribute)
2. Configure Name-with-entryUUID control:
   ```properties
   ldap.useEntryUuidDn=true
   ```
3. No scimId index needed - entryUUID is automatically indexed

## Testing

### Verify entryUUID is being used

1. **Create a user via SCIM**
   ```bash
   curl -X POST http://localhost:9080/scim/v2/Users \
     -H "Authorization: Bearer scim-token-123" \
     -H "Content-Type: application/scim+json" \
     -d '{
       "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
       "userName": "testuser"
     }'
   ```

2. **Check LDAP entry**
   ```bash
   ldapsearch -b "ou=users,dc=example,dc=com" "uid=testuser" \
     entryUUID scimId dn
   ```
   
   Expected:
   - `entryUUID` attribute present (operational)
   - `scimId` attribute absent
   - DN format: `entryUUID=<uuid>,ou=users,dc=example,dc=com`

3. **Test group membership filtering**
   ```bash
   # Get user's SCIM ID (entryUUID)
   USER_ID=$(curl -s http://localhost:9080/scim/v2/Users?filter=userName%20eq%20%22testuser%22 \
     -H "Authorization: Bearer scim-token-123" | jq -r '.Resources[0].id')
   
   # Query groups by member
   curl "http://localhost:9080/scim/v2/Groups?filter=members.value%20eq%20%22$USER_ID%22" \
     -H "Authorization: Bearer scim-token-123"
   ```

### Verify isMemberOf virtual attribute

```bash
ldapsearch -b "ou=users,dc=example,dc=com" "uid=testuser" isMemberOf
```

Should return DNs of groups the user is a member of.

## Benefits

1. **Simplified Architecture**: No custom attribute needed
2. **Better Performance**: Leverage PingDirectory's optimized entryUUID index
3. **Data Consistency**: Single source of truth (entryUUID)
4. **Standard Compliance**: Uses LDAP operational attributes as designed
5. **Easier DN Management**: entryUUID-based DNs are stable and UUID-based

## Compatibility Notes

- **Backward Compatible**: Code gracefully falls back if entryUUID is not available
- **Mixed Mode Support**: Can handle both uid-based and entryUUID-based DNs
- **Search Flexibility**: `findEntryByUuid()` works with entryUUID attribute

## References

- RFC 4530: LDAP entryUUID Operational Attribute
- PingDirectory Name-with-entryUUID control (OID 1.3.6.1.4.1.30221.2.5.44)
- SCIM 2.0 Core Schema (RFC 7643)
