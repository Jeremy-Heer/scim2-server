# SCIM2 LDAP Backend Implementation Status

## Completed Tasks ✓

### 1. LDAP Schema Design
- ✅ Created [/src/main/resources/schema/99-scim2-schema.ldif](src/main/resources/schema/99-scim2-schema.ldif)
  - 41 custom attributes using JSON type for complex multi-valued fields
  - 2 auxiliary object classes (scimUser, scimGroup)
  - PingDirectory JSON attribute syntax (OID 1.3.6.1.4.1.30221.2.3.4)

### 2. Index Documentation
- ✅ Created [/src/main/resources/schema/README-INDEXES.md](src/main/resources/schema/README-INDEXES.md)
  - 15 index definitions with priority levels
  - dsconfig commands for index creation

### 3. Repository Pattern Implementation
- ✅ Created [ScimRepository interface](src/main/java/com/scim2/server/scim2_server/repository/ScimRepository.java)
  - 22 method signatures for full SCIM CRUD operations
  - Support for filtering, pagination, sorting, attribute selection

### 4. JSON File Backend Refactoring
- ✅ Refactored [JsonFileService](src/main/java/com/scim2/server/scim2_server/service/JsonFileService.java) → [JsonFileRepository](src/main/java/com/scim2/server/scim2_server/repository/json/JsonFileRepository.java)
  - Implements ScimRepository interface
  - Uses @ConditionalOnProperty for backend selection
  - **Compiles with zero errors**

### 5. LDAP Infrastructure (Created but needs fixes)
- ⚠️ [LdapConnectionService](src/main/java/com/scim2/server/scim2_server/service/ldap/LdapConnectionService.java)
- ⚠️ [ScimLdapAttributeMapper](src/main/java/com/scim2/server/scim2_server/service/ldap/ScimLdapAttributeMapper.java)
- ⚠️ [ScimFilterToLdapConverter](src/main/java/com/scim2/server/scim2_server/service/ldap/ScimFilterToLdapConverter.java)
- ⚠️ [LdapRepository](src/main/java/com/scim2/server/scim2_server/repository/ldap/LdapRepository.java)

### 6. Controller Refactoring
- ✅ [UsersController](src/main/java/com/scim2/server/scim2_server/controller/UsersController.java) - Updated to use ScimRepository
- ✅ [GroupsController](src/main/java/com/scim2/server/scim2_server/controller/GroupsController.java) - Updated to use ScimRepository
- ✅ [BulkController](src/main/java/com/scim2/server/scim2_server/controller/BulkController.java) - Updated to use ScimRepository

### 7. Test File Refactoring
- ✅ [UsersControllerTest](src/test/java/com/scim2/server/scim2_server/controller/UsersControllerTest.java) - Updated to mock ScimRepository
- ✅ [GroupsControllerValidationTest](src/test/java/com/scim2/server/scim2_server/controller/GroupsControllerValidationTest.java) - Updated to mock ScimRepository
- ✅ [GroupMembersPatchTest](src/test/java/com/scim2/server/scim2_server/controller/GroupMembersPatchTest.java) - Updated to mock ScimRepository

### 8. Configuration
- ✅ [application.properties](src/main/resources/application.properties) - Added 15 LDAP configuration properties
- ✅ [LdapProperties](src/main/java/com/scim2/server/scim2_server/config/LdapProperties.java) - Configuration binding class
- ✅ [pom.xml](pom.xml) - Added unboundid-ldapsdk 7.0.2 dependency

## Known Compilation Errors to Fix 🔧

### LdapConnectionService.java
1. **Line 94** - `LDAPConnectionPool` constructor signature issue
   - Current: `new LDAPConnectionPool(connection, initialConnections, maxConnections, healthCheck)`
   - Fix: Check UnboundID LDAP SDK documentation for correct constructor parameters

### ScimLdapAttributeMapper.java
1. **Line 131** - `Attribute` constructor doesn't accept URI directly
   - Fix: Convert URI to String: `new Attribute("scimProfileUrl", user.getProfileUrl().toString())`
   
2. **Lines 280-281** - `EnterpriseUser` handling
   - Fix: Need to properly import and use UnboundID's Enterprise User extension class
   
3. **Line 365** - `setProfileUrl()` type mismatch
   - Fix: Parse String to URI: `user.setProfileUrl(new URI(entry.getAttributeValue("scimProfileUrl")))`
   
4. **Line 432** - `EnterpriseUser` not resolved
   - Fix: Import correct class from UnboundID SDK

### ScimFilterToLdapConverter.java
1. **Line 133** - `EqualFilter` constructor not visible
   - Fix: Need to use UnboundID LDAP SDK's Filter factory methods instead

### LdapRepository.java
1. **Multiple lines** - Wrong `SearchRequest` import
   - Current: Importing `com.unboundid.scim2.common.messages.SearchRequest` (SCIM)
   - Fix: Import `com.unboundid.ldap.sdk.SearchRequest` (LDAP)
   
2. **Lines 304, 675** - `asUserResource()` / `asGroupResource()` undefined
   - Fix: Need to properly cast or convert GenericScimResource
   
3. **Lines 372, 379, 739** - `addControl()` undefined
   - Fix: Verify SearchRequest API for adding controls
   
4. **Lines 383-384** - `setSizeLimit()` / `setTimeLimitSeconds()` undefined
   - Fix: Check proper SearchRequest builder/setter methods

## Next Steps

1. **Fix LDAP Component Compilation Errors**
   - Fix import statements (SearchRequest from LDAP SDK not SCIM SDK)
   - Fix LDAPConnectionPool constructor
   - Fix EnterpriseUser handling
   - Fix Filter creation methods
   - Fix GenericScimResource conversion

2. **Create Integration Tests**
   - Set up UnboundID In-Memory Directory Server
   - Test LdapRepository CRUD operations
   - Test filter conversion
   - Test attribute mapping
   - Test pagination and sorting

3. **Test Dual Backend Switching**
   - Verify JSON backend works with `scim.backend.type=json`
   - Verify LDAP backend works with `ldap.enabled=true`
   - Test configuration property precedence

4. **End-to-End Testing**
   - Deploy against real PingDirectory
   - Test Name-with-entryUUID control
   - Test JSON attribute storage
   - Test virtual memberOf
   - Performance testing

## Architecture Summary

### Backend Selection Logic
```java
// JSON Backend (default)
@Repository
@ConditionalOnProperty(
    name = "scim.backend.type",
    havingValue = "json",
    matchIfMissing = true
)
public class JsonFileRepository implements ScimRepository { ... }

// LDAP Backend (when enabled)
@Repository
@Primary
@ConditionalOnProperty(
    name = "ldap.enabled",
    havingValue = "true"
)
public class LdapRepository implements ScimRepository { ... }
```

### Key Design Decisions
1. **Repository Pattern**: Controllers depend on ScimRepository interface
2. **JSON Attributes**: Complex SCIM attributes stored as JSON in LDAP
3. **UUID Naming**: entryUUID naming control for DN generation
4. **Virtual Attributes**: PingDirectory memberOf for bidirectional groups
5. **Spring Conditional Beans**: Configuration-driven backend selection

## Configuration Reference

### JSON Backend (Default)
```properties
scim.backend.type=json
# Or simply omit this property - JSON is default
```

### LDAP Backend
```properties
ldap.enabled=true
ldap.url=ldap://localhost:1389
ldap.bind-dn=cn=Directory Manager
ldap.password=password
ldap.user-base-dn=ou=users,dc=example,dc=com
ldap.group-base-dn=ou=groups,dc=example,dc=com
ldap.pool.initial-connections=5
ldap.pool.max-connections=20
ldap.pool.max-connection-age-millis=3600000
ldap.search.size-limit=1000
ldap.search.time-limit-seconds=30
```
