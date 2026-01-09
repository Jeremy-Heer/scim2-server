package com.scim2.server.scim2_server.repository.ldap;

import com.scim2.server.scim2_server.repository.ScimRepository;
import com.scim2.server.scim2_server.service.ScimAttributeService;
import com.scim2.server.scim2_server.service.ScimSortingService;
import com.scim2.server.scim2_server.service.ldap.LdapConnectionService;
import com.scim2.server.scim2_server.service.ldap.ScimFilterToLdapConverter;
import com.scim2.server.scim2_server.service.ldap.ScimLdapAttributeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.types.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LDAP-based implementation of ScimRepository using UnboundID LDAP SDK.
 * 
 * Features:
 * - Full SCIM2 RFC 7644 CRUD operations
 * - SCIM filter to LDAP filter translation with JSON object filtering
 * - Server-side sorting and pagination
 * - entryUUID-based DN naming
 * - PingDirectory virtual memberOf support
 * - Bulk operation support
 * 
 * This implementation is active when ldap.enabled=true in application.properties.
 */
@Repository
@Primary
@ConditionalOnProperty(name = "ldap.enabled", havingValue = "true")
public class LdapRepository implements ScimRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(LdapRepository.class);
    
    // LDAP attributes to request for User entries (including operational attributes for meta)
    private static final String[] USER_ATTRIBUTES = {
        // Standard LDAP attributes
        "uid", "cn", "sn", "givenName", "displayName", "mail", "telephoneNumber",
        "personalTitle", "title", "employeeType", "preferredLanguage",
        // SCIM-specific attributes
        "scimExternalId", "scimActive", "scimMiddleName", "scimHonorificSuffix",
        "scimNickName", "scimLocale", "scimTimezone", "scimProfileUrl", "scimUserType",
        "scimEmails", "scimPhoneNumbers", "scimAddresses", "scimIms", "scimPhotos",
        "scimRoles", "scimEntitlements", "scimX509Certificates",
        "scimCostCenter", "scimOrganization", "scimDivision",
        "scimResourceType", "scimVersion",
        // Virtual attributes
        "memberOf",
        // Operational attributes (for meta)
        "entryUUID", "createTimestamp", "modifyTimestamp"
    };
    
    // LDAP attributes to request for Group entries (including operational attributes for meta)
    private static final String[] GROUP_ATTRIBUTES = {
        // Standard LDAP attributes
        "cn", "member",
        // SCIM-specific attributes
        "scimExternalId", "scimResourceType", "scimVersion",
        // Operational attributes (for meta)
        "entryUUID", "createTimestamp", "modifyTimestamp"
    };
    
    private final LdapConnectionService connectionService;
    private final ScimLdapAttributeMapper attributeMapper;
    private final ScimFilterToLdapConverter filterConverter;
    private final ScimAttributeService attributeService;
    private final ScimSortingService sortingService;
    
    public LdapRepository(LdapConnectionService connectionService,
                         ScimLdapAttributeMapper attributeMapper,
                         ScimFilterToLdapConverter filterConverter,
                         ScimAttributeService attributeService,
                         ScimSortingService sortingService) {
        this.connectionService = connectionService;
        this.attributeMapper = attributeMapper;
        this.filterConverter = filterConverter;
        this.attributeService = attributeService;
        this.sortingService = sortingService;
    }
    
    @PostConstruct
    @Override
    public void init() {
        logger.info("Initializing LDAP repository...");
        // Connection pool is initialized in LdapConnectionService
        logger.info("LDAP repository initialized successfully");
    }
    
    // ========== User Operations ==========
    
    @Override
    public Collection<UserResource> getAllUsers() {
        try {
            SearchRequest searchRequest = new SearchRequest(
                connectionService.getLdapProperties().getUserBaseDn(),
                SearchScope.SUB,
                Filter.createEqualityFilter("objectClass", "scimUser"),
                USER_ATTRIBUTES
            );
            
            SearchResult result = connectionService.search(searchRequest);
            
            return result.getSearchEntries().stream()
                .map(entry -> attributeMapper.ldapEntryToUser(entry, connectionService))
                .collect(Collectors.toList());
                
        } catch (LDAPException e) {
            logger.error("Failed to get all users", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public UserResource getUserById(String id) {
        return getUserById(id, null, null);
    }
    
    @Override
    public UserResource getUserById(String id, String attributes, String excludedAttributes) {
        try {
            // Search by scimId attribute
            SearchResultEntry entry = connectionService.findEntryByUuid(
                id, 
                connectionService.getLdapProperties().getUserBaseDn()
            );
            
            if (entry == null) {
                return null;
            }
            
            UserResource user = attributeMapper.ldapEntryToUser(entry, connectionService);
            
            // Apply attribute selection
            if (attributes != null || excludedAttributes != null) {
                user = attributeService.selectAttributes(user, attributes, excludedAttributes);
            }
            
            return user;
            
        } catch (LDAPException e) {
            logger.error("Failed to get user by ID: {}", id, e);
            return null;
        }
    }
    
    @Override
    public int getTotalUsers(String filter) {
        try {
            Filter ldapFilter;
            if (filter != null && !filter.trim().isEmpty()) {
                // Convert SCIM filter to LDAP filter
                ldapFilter = filterConverter.convertFilter(filter);
                // Combine with objectClass filter
                ldapFilter = Filter.createANDFilter(
                    Filter.createEqualityFilter("objectClass", "scimUser"),
                    ldapFilter
                );
            } else {
                ldapFilter = Filter.createEqualityFilter("objectClass", "scimUser");
            }
            
            SearchRequest searchRequest = new SearchRequest(
                connectionService.getLdapProperties().getUserBaseDn(),
                SearchScope.SUB,
                ldapFilter,
                "1.1" // Don't return any attributes, just count
            );
            
            SearchResult result = connectionService.search(searchRequest);
            return result.getEntryCount();
            
        } catch (Exception e) {
            logger.error("Failed to get total users with filter: {}", filter, e);
            return 0;
        }
    }
    
    @Override
    public int getTotalUsers(com.unboundid.scim2.common.messages.SearchRequest searchRequest) {
        // Extract filter from SearchRequest
        String filter = null;
        if (searchRequest.getFilter() != null) {
            filter = searchRequest.getFilter().toString();
        }
        return getTotalUsers(filter);
    }
    
    @Override
    public UserResource saveUser(UserResource user) {
        try {
            // Don't pre-generate SCIM ID - let LDAP server create entryUUID
            // We'll retrieve it after successful creation
            
            // Set metadata
            Meta meta = new Meta();
            meta.setResourceType("User");
            meta.setCreated(Calendar.getInstance());
            meta.setLastModified(Calendar.getInstance());
            meta.setVersion(generateVersion());
            user.setMeta(meta);
            
            // Convert user to LDAP attributes
            Map<String, Attribute> attributes = attributeMapper.userToLdapAttributes(user);
            
            // Build DN (placeholder - will be replaced by entryUUID if enabled)
            String dn = connectionService.buildUserDn(user.getUserName());
            
            // Create add request
            AddRequest addRequest = new AddRequest(dn, attributes.values());
            
            // Add Name with entryUUID control if enabled
            if (connectionService.getLdapProperties().isUseEntryUuidDn()) {
                addRequest.addControl(connectionService.createNameWithEntryUuidControl());
            }
            
            // Execute add operation
            LDAPResult result = connectionService.add(addRequest);
            
            if (result.getResultCode() == ResultCode.SUCCESS) {
                logger.info("Created user: {} with DN: {}", user.getUserName(), dn);
                
                // Retrieve the entry to get the entryUUID (SCIM ID)
                SearchResultEntry entry;
                if (connectionService.getLdapProperties().isUseEntryUuidDn()) {
                    // Search by uid to find the newly created entry
                    Filter filter = Filter.createEqualityFilter("uid", user.getUserName());
                    SearchRequest searchRequest = new SearchRequest(
                        connectionService.getLdapProperties().getUserBaseDn(),
                        SearchScope.SUB,
                        filter,
                        USER_ATTRIBUTES
                    );
                    searchRequest.setSizeLimit(1);
                    SearchResult searchResult = connectionService.search(searchRequest);
                    
                    if (searchResult.getEntryCount() > 0) {
                        entry = searchResult.getSearchEntries().get(0);
                        logger.info("User created with entryUUID DN: {}", entry.getDN());
                        return attributeMapper.ldapEntryToUser(entry, connectionService);
                    } else {
                        logger.error("Failed to retrieve newly created user by uid: {}", user.getUserName());
                        throw new RuntimeException("Failed to retrieve newly created user");
                    }
                } else {
                    // Non-entryUUID mode: search by DN
                    entry = connectionService.getConnection().getEntry(dn, "*", "+");
                    if (entry != null) {
                        return attributeMapper.ldapEntryToUser(entry, connectionService);
                    }
                }
                
                // Fallback - return user with generated metadata
                logger.warn("Could not retrieve entry after creation, returning user without entryUUID");
                return user;
            } else {
                logger.error("Failed to create user: {}", result.getDiagnosticMessage());
                throw new RuntimeException("Failed to create user: " + result.getDiagnosticMessage());
            }
            
        } catch (LDAPException e) {
            logger.error("LDAP error creating user", e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public UserResource updateUser(String id, UserResource user) {
        try {
            // Find existing entry
            SearchResultEntry existingEntry = connectionService.findEntryByUuid(
                id,
                connectionService.getLdapProperties().getUserBaseDn()
            );
            
            if (existingEntry == null) {
                throw new RuntimeException("User not found: " + id);
            }
            
            // Preserve ID
            user.setId(id);
            
            // Update metadata
            Meta meta = user.getMeta() != null ? user.getMeta() : new Meta();
            meta.setLastModified(Calendar.getInstance());
            meta.setVersion(generateVersion());
            user.setMeta(meta);
            
            // Convert to LDAP attributes
            Map<String, Attribute> newAttributes = attributeMapper.userToLdapAttributes(user);
            
            // Build modify request - replace all modifiable attributes
            List<Modification> modifications = new ArrayList<>();
            
            for (Map.Entry<String, Attribute> entry : newAttributes.entrySet()) {
                String attrName = entry.getKey();
                
                // Skip non-modifiable attributes
                if (attrName.equals("objectClass") || attrName.equals("scimId") || 
                    attrName.equals("uid")) {
                    continue;
                }
                
                modifications.add(new Modification(
                    ModificationType.REPLACE,
                    attrName,
                    entry.getValue().getValues()
                ));
            }
            
            ModifyRequest modifyRequest = new ModifyRequest(existingEntry.getDN(), modifications);
            LDAPResult result = connectionService.modify(modifyRequest);
            
            if (result.getResultCode() == ResultCode.SUCCESS) {
                logger.info("Updated user: {}", id);
                return getUserById(id);
            } else {
                throw new RuntimeException("Failed to update user: " + result.getDiagnosticMessage());
            }
            
        } catch (LDAPException e) {
            logger.error("LDAP error updating user: {}", id, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public UserResource patchUser(String id, PatchRequest patchRequest) {
        try {
            // Get existing user
            UserResource user = getUserById(id);
            if (user == null) {
                throw new RuntimeException("User not found: " + id);
            }
            
            // Apply SCIM patch operations using UnboundID SDK
            com.unboundid.scim2.common.GenericScimResource genericUser = 
                user.asGenericScimResource();
            
            patchRequest.apply(genericUser);
            
            // Convert back to UserResource - the genericUser IS already a GenericScimResource
            // We need to construct a new UserResource from the patched generic resource
            UserResource patchedUser = new UserResource();
            patchedUser.setId(genericUser.getId());
            patchedUser.setExternalId(genericUser.getExternalId());
            patchedUser.setMeta(genericUser.getMeta());
            // Copy all attributes from the patched generic resource
            try {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(genericUser);
                patchedUser = mapper.readValue(json, UserResource.class);
            } catch (Exception e) {
                logger.error("Failed to convert patched resource", e);
                throw new RuntimeException("Failed to convert patched resource", e);
            }
            
            // Update in LDAP using updateUser
            return updateUser(id, patchedUser);
            
        } catch (Exception e) {
            logger.error("Failed to patch user: {}", id, e);
            throw new RuntimeException("Failed to patch user: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean deleteUser(String id) {
        try {
            SearchResultEntry entry = connectionService.findEntryByUuid(
                id,
                connectionService.getLdapProperties().getUserBaseDn()
            );
            
            if (entry == null) {
                return false;
            }
            
            DeleteRequest deleteRequest = new DeleteRequest(entry.getDN());
            LDAPResult result = connectionService.delete(deleteRequest);
            
            if (result.getResultCode() == ResultCode.SUCCESS) {
                logger.info("Deleted user: {}", id);
                return true;
            } else {
                logger.error("Failed to delete user {}: {}", id, result.getDiagnosticMessage());
                return false;
            }
            
        } catch (LDAPException e) {
            logger.error("LDAP error deleting user: {}", id, e);
            return false;
        }
    }
    
    @Override
    public List<UserResource> searchUsers(String filter, String attributes, String excludedAttributes,
                                         String sortBy, String sortOrder, int startIndex, int count) {
        try {
            // Build LDAP filter
            Filter ldapFilter;
            if (filter != null && !filter.trim().isEmpty()) {
                ldapFilter = filterConverter.convertFilter(filter);
                ldapFilter = Filter.createANDFilter(
                    Filter.createEqualityFilter("objectClass", "scimUser"),
                    ldapFilter
                );
            } else {
                ldapFilter = Filter.createEqualityFilter("objectClass", "scimUser");
            }
            
            // Build search request
            SearchRequest searchRequest = new SearchRequest(
                connectionService.getLdapProperties().getUserBaseDn(),
                SearchScope.SUB,
                ldapFilter,
                USER_ATTRIBUTES
            );
            
            // Add server-side sort control if sortBy is specified
            if (sortBy != null && !sortBy.isEmpty()) {
                boolean ascending = !"descending".equalsIgnoreCase(sortOrder);
                SortKey sortKey = new SortKey(mapScimAttributeToLdap(sortBy), ascending);
                searchRequest.addControl(new ServerSideSortRequestControl(sortKey));
            }
            
            // Add pagination control
            if (count > 0) {
                // LDAP uses 0-based, SCIM uses 1-based indexing
                int ldapStartIndex = Math.max(0, startIndex - 1);
                searchRequest.addControl(new SimplePagedResultsControl(count));
                // Note: For subsequent pages, we'd need to track the cookie
            }
            
            searchRequest.setSizeLimit(count > 0 ? count : connectionService.getLdapProperties().getSearch().getSizeLimit());
            searchRequest.setTimeLimitSeconds(connectionService.getLdapProperties().getSearch().getTimeLimitSeconds());
            
            SearchResult result = connectionService.search(searchRequest);
            
            List<UserResource> users = result.getSearchEntries().stream()
                .map(entry -> attributeMapper.ldapEntryToUser(entry, connectionService))
                .collect(Collectors.toList());
            
            // Apply attribute selection
            if (attributes != null || excludedAttributes != null) {
                users = users.stream()
                    .map(user -> attributeService.selectAttributes(user, attributes, excludedAttributes))
                    .collect(Collectors.toList());
            }
            
            // Apply pagination (skip to startIndex)
            if (startIndex > 1) {
                int skipCount = startIndex - 1;
                users = users.stream().skip(skipCount).collect(Collectors.toList());
            }
            
            // Limit to count
            if (count > 0 && users.size() > count) {
                users = users.subList(0, count);
            }
            
            return users;
            
        } catch (Exception e) {
            logger.error("Failed to search users", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<UserResource> searchUsers(com.unboundid.scim2.common.messages.SearchRequest searchRequest) {
        // Parse SearchRequest and extract parameters
        String filter = null;
        if (searchRequest.getFilter() != null) {
            filter = searchRequest.getFilter().toString();
        }
        
        // Extract attributes - convert List to comma-separated String
        String attributes = null;
        if (searchRequest.getAttributes() != null && !searchRequest.getAttributes().isEmpty()) {
            attributes = String.join(",", searchRequest.getAttributes());
        }
        
        String excludedAttributes = null;
        if (searchRequest.getExcludedAttributes() != null && !searchRequest.getExcludedAttributes().isEmpty()) {
            excludedAttributes = String.join(",", searchRequest.getExcludedAttributes());
        }
        
        String sortBy = searchRequest.getSortBy();
        String sortOrder = searchRequest.getSortOrder() != null ? searchRequest.getSortOrder().getName() : null;
        int startIndex = searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() : 1;
        int count = searchRequest.getCount() != null ? searchRequest.getCount() : 100;
        
        return searchUsers(filter, attributes, excludedAttributes, sortBy, sortOrder, startIndex, count);
    }
    
    // ========== Group Operations ==========
    
    @Override
    public Collection<GroupResource> getAllGroups() {
        try {
            SearchRequest searchRequest = new SearchRequest(
                connectionService.getLdapProperties().getGroupBaseDn(),
                SearchScope.SUB,
                Filter.createEqualityFilter("objectClass", "scimGroup"),
                "*"
            );
            
            SearchResult result = connectionService.search(searchRequest);
            
            return result.getSearchEntries().stream()
                .map(entry -> attributeMapper.ldapEntryToGroup(entry, connectionService))
                .collect(Collectors.toList());
                
        } catch (LDAPException e) {
            logger.error("Failed to get all groups", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public GroupResource getGroupById(String id) {
        return getGroupById(id, null, null);
    }
    
    @Override
    public GroupResource getGroupById(String id, String attributes, String excludedAttributes) {
        try {
            SearchResultEntry entry = connectionService.findEntryByUuid(
                id,
                connectionService.getLdapProperties().getGroupBaseDn()
            );
            
            if (entry == null) {
                return null;
            }
            
            GroupResource group = attributeMapper.ldapEntryToGroup(entry, connectionService);
            
            if (attributes != null || excludedAttributes != null) {
                group = attributeService.selectAttributes(group, attributes, excludedAttributes);
            }
            
            return group;
            
        } catch (LDAPException e) {
            logger.error("Failed to get group by ID: {}", id, e);
            return null;
        }
    }
    
    @Override
    public int getTotalGroups(String filter) {
        try {
            Filter ldapFilter;
            if (filter != null && !filter.trim().isEmpty()) {
                ldapFilter = filterConverter.convertFilter(filter);
                ldapFilter = Filter.createANDFilter(
                    Filter.createEqualityFilter("objectClass", "scimGroup"),
                    ldapFilter
                );
            } else {
                ldapFilter = Filter.createEqualityFilter("objectClass", "scimGroup");
            }
            
            SearchRequest searchRequest = new SearchRequest(
                connectionService.getLdapProperties().getGroupBaseDn(),
                SearchScope.SUB,
                ldapFilter,
                "1.1"
            );
            
            SearchResult result = connectionService.search(searchRequest);
            return result.getEntryCount();
            
        } catch (Exception e) {
            logger.error("Failed to get total groups with filter: {}", filter, e);
            return 0;
        }
    }
    
    @Override
    public int getTotalGroups(com.unboundid.scim2.common.messages.SearchRequest searchRequest) {
        // TODO: Implement proper SearchRequest parsing
        return getTotalGroups((String) null);
    }
    
    @Override
    public GroupResource saveGroup(GroupResource group) {
        try {
            // Don't pre-generate SCIM ID - let LDAP server create entryUUID
            // We'll retrieve it after successful creation
            
            Meta meta = new Meta();
            meta.setResourceType("Group");
            meta.setCreated(Calendar.getInstance());
            meta.setLastModified(Calendar.getInstance());
            meta.setVersion(generateVersion());
            group.setMeta(meta);
            
            Map<String, Attribute> attributes = attributeMapper.groupToLdapAttributes(group);
            
            String dn = connectionService.buildGroupDn(group.getDisplayName());
            
            // Add member DNs - convert SCIM IDs (UUIDs) to LDAP DNs
            List<String> memberDns = new ArrayList<>();
            if (group.getMembers() != null) {
                for (Member member : group.getMembers()) {
                    // Build DN from UUID
                    String memberDn = connectionService.buildDnFromUuid(
                        member.getValue(),
                        connectionService.getLdapProperties().getUserBaseDn()
                    );
                    if (memberDn != null) {
                        memberDns.add(memberDn);
                    } else {
                        logger.warn("Could not resolve member UUID to DN: {}", member.getValue());
                    }
                }
            }
            
            // Add member attribute only if there are members
            if (!memberDns.isEmpty()) {
                attributes.put("member", new Attribute("member", memberDns));
            }
            
            AddRequest addRequest = new AddRequest(dn, attributes.values());
            
            if (connectionService.getLdapProperties().isUseEntryUuidDn()) {
                addRequest.addControl(connectionService.createNameWithEntryUuidControl());
            }
            
            LDAPResult result = connectionService.add(addRequest);
            
            if (result.getResultCode() == ResultCode.SUCCESS) {
                logger.info("Created group: {}", group.getDisplayName());
                
                // Retrieve the entry to get the entryUUID (SCIM ID)
                SearchResultEntry entry;
                if (connectionService.getLdapProperties().isUseEntryUuidDn()) {
                    // Search by cn to find the newly created entry
                    Filter filter = Filter.createEqualityFilter("cn", group.getDisplayName());
                    SearchRequest searchRequest = new SearchRequest(
                        connectionService.getLdapProperties().getGroupBaseDn(),
                        SearchScope.SUB,
                        filter,
                        GROUP_ATTRIBUTES // Request all group and operational attributes (including entryUUID)
                    );
                    searchRequest.setSizeLimit(1);
                    SearchResult searchResult = connectionService.search(searchRequest);
                    
                    if (searchResult.getEntryCount() > 0) {
                        entry = searchResult.getSearchEntries().get(0);
                        return attributeMapper.ldapEntryToGroup(entry, connectionService);
                    } else {
                        logger.error("Failed to retrieve newly created group by cn: {}", group.getDisplayName());
                        throw new RuntimeException("Failed to retrieve newly created group");
                    }
                } else {
                    // Non-entryUUID mode: search by DN
                    entry = connectionService.getConnection().getEntry(dn, GROUP_ATTRIBUTES);
                    if (entry != null) {
                        return attributeMapper.ldapEntryToGroup(entry, connectionService);
                    }
                }
                
                // Fallback - return group with generated metadata
                logger.warn("Could not retrieve entry after creation, returning group without entryUUID");
                return group;
            } else {
                throw new RuntimeException("Failed to create group: " + result.getDiagnosticMessage());
            }
            
        } catch (LDAPException e) {
            logger.error("LDAP error creating group", e);
            throw new RuntimeException("Failed to create group: " + e.getMessage(), e);
        }
    }
    
    @Override
    public GroupResource updateGroup(String id, GroupResource group) {
        try {
            SearchResultEntry existingEntry = connectionService.findEntryByUuid(
                id,
                connectionService.getLdapProperties().getGroupBaseDn()
            );
            
            if (existingEntry == null) {
                throw new RuntimeException("Group not found: " + id);
            }
            
            group.setId(id);
            
            Meta meta = group.getMeta() != null ? group.getMeta() : new Meta();
            meta.setLastModified(Calendar.getInstance());
            meta.setVersion(generateVersion());
            group.setMeta(meta);
            
            List<Modification> modifications = new ArrayList<>();
            
            // Update displayName (cn)
            if (group.getDisplayName() != null) {
                modifications.add(new Modification(
                    ModificationType.REPLACE,
                    "cn",
                    group.getDisplayName()
                ));
            }
            
            // Update members - convert SCIM IDs (UUIDs) to LDAP DNs
            if (group.getMembers() != null) {
                List<String> memberDns = new ArrayList<>();
                for (Member member : group.getMembers()) {
                    // Build DN from UUID
                    String memberDn = connectionService.buildDnFromUuid(
                        member.getValue(),
                        connectionService.getLdapProperties().getUserBaseDn()
                    );
                    if (memberDn != null) {
                        memberDns.add(memberDn);
                    } else {
                        logger.warn("Could not resolve member UUID to DN: {}", member.getValue());
                    }
                }
                
                modifications.add(new Modification(
                    ModificationType.REPLACE,
                    "member",
                    memberDns.toArray(new String[0])
                ));
            }
            
            ModifyRequest modifyRequest = new ModifyRequest(existingEntry.getDN(), modifications);
            LDAPResult result = connectionService.modify(modifyRequest);
            
            if (result.getResultCode() == ResultCode.SUCCESS) {
                logger.info("Updated group: {}", id);
                return getGroupById(id);
            } else {
                throw new RuntimeException("Failed to update group: " + result.getDiagnosticMessage());
            }
            
        } catch (LDAPException e) {
            logger.error("LDAP error updating group: {}", id, e);
            throw new RuntimeException("Failed to update group: " + e.getMessage(), e);
        }
    }
    
    @Override
    public GroupResource patchGroup(String id, PatchRequest patchRequest) {
        try {
            SearchResultEntry existingEntry = connectionService.findEntryByUuid(
                id,
                connectionService.getLdapProperties().getGroupBaseDn()
            );
            
            if (existingEntry == null) {
                throw new RuntimeException("Group not found: " + id);
            }
            
            List<Modification> modifications = new ArrayList<>();
            
            // Process each PATCH operation
            for (PatchOperation operation : patchRequest.getOperations()) {
                com.unboundid.scim2.common.Path path = operation.getPath();
                String pathStr = path != null ? path.toString() : "";
                com.unboundid.scim2.common.messages.PatchOpType opType = operation.getOpType();
                
                // Handle member operations
                if ("members".equalsIgnoreCase(pathStr) || pathStr.toLowerCase().startsWith("members")) {
                    if (opType == com.unboundid.scim2.common.messages.PatchOpType.ADD) {
                        // Add operation - use ADD modification type
                        List<String> memberDnsToAdd = new ArrayList<>();
                        
                        try {
                            JsonNode valueNode = operation.getJsonNode();
                            
                            if (valueNode.isArray()) {
                                for (JsonNode memberNode : valueNode) {
                                    if (memberNode.has("value")) {
                                        String memberUuid = memberNode.get("value").asText();
                                        String memberDn = connectionService.buildDnFromUuid(
                                            memberUuid,
                                            connectionService.getLdapProperties().getUserBaseDn()
                                        );
                                        if (memberDn != null) {
                                            memberDnsToAdd.add(memberDn);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Failed to parse member add operation", e);
                        }
                        
                        if (!memberDnsToAdd.isEmpty()) {
                            modifications.add(new Modification(
                                ModificationType.ADD,
                                "member",
                                memberDnsToAdd.toArray(new String[0])
                            ));
                            logger.info("Adding {} members to group {}", memberDnsToAdd.size(), id);
                        }
                        
                    } else if (opType == com.unboundid.scim2.common.messages.PatchOpType.REMOVE) {
                        // Remove operation - use DELETE modification type
                        List<String> memberDnsToRemove = new ArrayList<>();
                        
                        try {
                            JsonNode valueNode = operation.getJsonNode();
                            
                            if (valueNode.isArray()) {
                                for (JsonNode memberNode : valueNode) {
                                    if (memberNode.has("value")) {
                                        String memberUuid = memberNode.get("value").asText();
                                        String memberDn = connectionService.buildDnFromUuid(
                                            memberUuid,
                                            connectionService.getLdapProperties().getUserBaseDn()
                                        );
                                        if (memberDn != null) {
                                            memberDnsToRemove.add(memberDn);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Failed to parse member remove operation", e);
                        }
                        
                        if (!memberDnsToRemove.isEmpty()) {
                            modifications.add(new Modification(
                                ModificationType.DELETE,
                                "member",
                                memberDnsToRemove.toArray(new String[0])
                            ));
                            logger.info("Removing {} members from group {}", memberDnsToRemove.size(), id);
                        }
                        
                    } else if (opType == com.unboundid.scim2.common.messages.PatchOpType.REPLACE) {
                        // Replace operation - use REPLACE modification type
                        List<String> memberDns = new ArrayList<>();
                        
                        try {
                            JsonNode valueNode = operation.getJsonNode();
                            
                            if (valueNode.isArray()) {
                                for (JsonNode memberNode : valueNode) {
                                    if (memberNode.has("value")) {
                                        String memberUuid = memberNode.get("value").asText();
                                        String memberDn = connectionService.buildDnFromUuid(
                                            memberUuid,
                                            connectionService.getLdapProperties().getUserBaseDn()
                                        );
                                        if (memberDn != null) {
                                            memberDns.add(memberDn);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Failed to parse member replace operation", e);
                        }
                        
                        if (!memberDns.isEmpty()) {
                            modifications.add(new Modification(
                                ModificationType.REPLACE,
                                "member",
                                memberDns.toArray(new String[0])
                            ));
                            logger.info("Replacing members in group {} with {} members", id, memberDns.size());
                        }
                    }
                } else if ("displayName".equalsIgnoreCase(pathStr)) {
                    // Handle displayName (cn) modifications
                    if (opType == com.unboundid.scim2.common.messages.PatchOpType.REPLACE || 
                        opType == com.unboundid.scim2.common.messages.PatchOpType.ADD) {
                        try {
                            String newDisplayName = operation.getJsonNode().asText();
                            modifications.add(new Modification(
                                ModificationType.REPLACE,
                                "cn",
                                newDisplayName
                            ));
                            logger.info("Updating displayName for group {}", id);
                        } catch (Exception e) {
                            logger.error("Failed to parse displayName operation", e);
                        }
                    }
                }
            }
            
            // Apply modifications if any
            if (!modifications.isEmpty()) {
                ModifyRequest modifyRequest = new ModifyRequest(existingEntry.getDN(), modifications);
                LDAPResult result = connectionService.modify(modifyRequest);
                
                if (result.getResultCode() != ResultCode.SUCCESS) {
                    throw new RuntimeException("Failed to patch group: " + result.getDiagnosticMessage());
                }
                logger.info("Successfully patched group {} with {} modifications", id, modifications.size());
            }
            
            // Return updated group
            GroupResource updatedGroup = getGroupById(id);
            Meta meta = updatedGroup.getMeta() != null ? updatedGroup.getMeta() : new Meta();
            meta.setLastModified(Calendar.getInstance());
            meta.setVersion(generateVersion());
            updatedGroup.setMeta(meta);
            
            return updatedGroup;
            
        } catch (LDAPException e) {
            logger.error("LDAP error patching group: {}", id, e);
            throw new RuntimeException("Failed to patch group: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Failed to patch group: {}", id, e);
            throw new RuntimeException("Failed to patch group: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean deleteGroup(String id) {
        try {
            SearchResultEntry entry = connectionService.findEntryByUuid(
                id,
                connectionService.getLdapProperties().getGroupBaseDn()
            );
            
            if (entry == null) {
                return false;
            }
            
            DeleteRequest deleteRequest = new DeleteRequest(entry.getDN());
            LDAPResult result = connectionService.delete(deleteRequest);
            
            if (result.getResultCode() == ResultCode.SUCCESS) {
                logger.info("Deleted group: {}", id);
                return true;
            } else {
                logger.error("Failed to delete group {}: {}", id, result.getDiagnosticMessage());
                return false;
            }
            
        } catch (LDAPException e) {
            logger.error("LDAP error deleting group: {}", id, e);
            return false;
        }
    }
    
    @Override
    public List<GroupResource> searchGroups(String filter, String attributes, String excludedAttributes,
                                           String sortBy, String sortOrder, int startIndex, int count) {
        try {
            Filter ldapFilter;
            if (filter != null && !filter.trim().isEmpty()) {
                ldapFilter = filterConverter.convertFilter(filter);
                ldapFilter = Filter.createANDFilter(
                    Filter.createEqualityFilter("objectClass", "scimGroup"),
                    ldapFilter
                );
            } else {
                ldapFilter = Filter.createEqualityFilter("objectClass", "scimGroup");
            }
            
            SearchRequest searchRequest = new SearchRequest(
                connectionService.getLdapProperties().getGroupBaseDn(),
                SearchScope.SUB,
                ldapFilter,
                GROUP_ATTRIBUTES
            );
            
            if (sortBy != null && !sortBy.isEmpty()) {
                boolean ascending = !"descending".equalsIgnoreCase(sortOrder);
                SortKey sortKey = new SortKey(mapScimAttributeToLdap(sortBy), ascending);
                searchRequest.addControl(new ServerSideSortRequestControl(sortKey));
            }
            
            if (count > 0) {
                searchRequest.addControl(new SimplePagedResultsControl(count));
            }
            
            searchRequest.setSizeLimit(count > 0 ? count : connectionService.getLdapProperties().getSearch().getSizeLimit());
            searchRequest.setTimeLimitSeconds(connectionService.getLdapProperties().getSearch().getTimeLimitSeconds());
            
            SearchResult result = connectionService.search(searchRequest);
            
            List<GroupResource> groups = result.getSearchEntries().stream()
                .map(entry -> attributeMapper.ldapEntryToGroup(entry, connectionService))
                .collect(Collectors.toList());
            
            if (attributes != null || excludedAttributes != null) {
                groups = groups.stream()
                    .map(grp -> attributeService.selectAttributes(grp, attributes, excludedAttributes))
                    .collect(Collectors.toList());
            }
            
            if (startIndex > 1) {
                int skipCount = startIndex - 1;
                groups = groups.stream().skip(skipCount).collect(Collectors.toList());
            }
            
            if (count > 0 && groups.size() > count) {
                groups = groups.subList(0, count);
            }
            
            return groups;
            
        } catch (Exception e) {
            logger.error("Failed to search groups", e);
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<GroupResource> searchGroups(com.unboundid.scim2.common.messages.SearchRequest searchRequest) {
        // TODO: Implement proper SearchRequest parsing
        return searchGroups(null, null, null, null, null, 1, 100);
    }
    
    // ========== Helper Methods ==========
    
    private String generateVersion() {
        return "W/\"" + System.currentTimeMillis() + "\"";
    }
    
    /**
     * Map SCIM attribute name to LDAP attribute name for sorting.
     */
    private String mapScimAttributeToLdap(String scimAttribute) {
        if (scimAttribute == null) {
            return "cn";
        }
        
        switch (scimAttribute.toLowerCase()) {
            case "username":
                return "uid";
            case "displayname":
                return "cn";
            case "name.familyname":
                return "sn";
            case "name.givenname":
                return "givenName";
            case "emails.value":
                return "mail";
            default:
                return scimAttribute;
        }
    }
}
