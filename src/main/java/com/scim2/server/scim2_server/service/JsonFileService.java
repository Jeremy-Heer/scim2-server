package com.scim2.server.scim2_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scim2.server.scim2_server.model.ScimSearchResult;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.Member;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.messages.SearchRequest;
import com.unboundid.scim2.common.Path;
import com.unboundid.scim2.common.ScimResource;
import com.unboundid.scim2.common.GenericScimResource;
import com.unboundid.scim2.common.utils.JsonUtils;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.scim2.server.scim2_server.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JsonFileService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SdkFilterService sdkFilterService;
    private final ScimAttributeService scimAttributeService;
    private final ScimSortingService scimSortingService;
    private final String DATA_DIR = "data";
    private final String USERS_FILE = DATA_DIR + "/users.json";
    private final String GROUPS_FILE = DATA_DIR + "/groups.json";
    
    private final Map<String, UserResource> users = new ConcurrentHashMap<>();
    private final Map<String, GroupResource> groups = new ConcurrentHashMap<>();
    
    public JsonFileService(SdkFilterService sdkFilterService, ScimAttributeService scimAttributeService, 
                          ScimSortingService scimSortingService) {
        this.sdkFilterService = sdkFilterService;
        this.scimAttributeService = scimAttributeService;
        this.scimSortingService = scimSortingService;
    }
    
    @PostConstruct
    public void init() {
        createDataDirectoryIfNotExists();
        loadUsers();
        loadGroups();
    }
    
    private void createDataDirectoryIfNotExists() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    // User operations
    public Collection<UserResource> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public UserResource getUserById(String id) {
        return users.get(id);
    }
    
    public UserResource getUserById(String id, String attributes, String excludedAttributes) {
        UserResource user = users.get(id);
        if (user != null) {
            user = scimAttributeService.selectAttributes(user, attributes, excludedAttributes);
        }
        return user;
    }
    
    public int getTotalUsers(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return users.size();
        }
        
        List<UserResource> allUsers = new ArrayList<>(users.values());
        List<UserResource> filteredUsers = sdkFilterService.filterUsers(allUsers, filter);
        return filteredUsers.size();
    }    public UserResource saveUser(UserResource user) {
        // Always generate a new UUID for create operations - ignore any provided ID
        user.setId(UUID.randomUUID().toString());
        // Ensure meta exists and update server-controlled fields
        if (user.getMeta() != null) {
            user.getMeta().setVersion(generateVersion());
            user.getMeta().setLastModified(Calendar.getInstance());
        }
        users.put(user.getId(), user);
        saveUsers();
        return user;
    }
    
    public UserResource updateUser(String id, UserResource user) {
        user.setId(id);
        // Ensure meta exists and update server-controlled fields
        if (user.getMeta() != null) {
            user.getMeta().setVersion(generateVersion());
            user.getMeta().setLastModified(Calendar.getInstance());
        }
        users.put(id, user);
        saveUsers();
        return user;
    }
    
    public UserResource patchUser(String id, PatchRequest patchRequest) {
        UserResource existingUser = users.get(id);
        if (existingUser == null) {
            throw new InvalidRequestException("User with id '" + id + "' not found");
        }
        
        try {
            // Use SDK's built-in patch application with proper conversion
            GenericScimResource genericUser = existingUser.asGenericScimResource();
            patchRequest.apply(genericUser);
            UserResource modifiedUser = JsonUtils.nodeToValue(genericUser.getObjectNode(), UserResource.class);
            
            // Update server-controlled fields
            if (modifiedUser.getMeta() != null) {
                modifiedUser.getMeta().setVersion(generateVersion());
                modifiedUser.getMeta().setLastModified(Calendar.getInstance());
            }
            
            users.put(id, modifiedUser);
            saveUsers();
            return modifiedUser;
            
        } catch (ScimException | JsonProcessingException e) {
            throw new InvalidRequestException("Error applying patch operations: " + e.getMessage());
        }
    }
    
    public boolean deleteUser(String id) {
        boolean removed = users.remove(id) != null;
        if (removed) {
            saveUsers();
        }
        return removed;
    }
    
    // Group operations
    public Collection<GroupResource> getAllGroups() {
        return new ArrayList<>(groups.values());
    }

    public GroupResource getGroupById(String id) {
        return groups.get(id);
    }
    
    public GroupResource getGroupById(String id, String attributes, String excludedAttributes) {
        GroupResource group = groups.get(id);
        if (group != null) {
            group = scimAttributeService.selectAttributes(group, attributes, excludedAttributes);
        }
        return group;
    }
    
    public int getTotalGroups(String filter) {
        if (filter == null || filter.trim().isEmpty()) {
            return groups.size();
        }
        
        List<GroupResource> allGroups = new ArrayList<>(groups.values());
        List<GroupResource> filteredGroups = sdkFilterService.filterGroups(allGroups, filter);
        return filteredGroups.size();
    }    public GroupResource saveGroup(GroupResource group) {
        // Always generate a new UUID for create operations - ignore any provided ID
        group.setId(UUID.randomUUID().toString());
        // Ensure meta exists and update server-controlled fields
        if (group.getMeta() != null) {
            group.getMeta().setVersion(generateVersion());
            group.getMeta().setLastModified(Calendar.getInstance());
        }
        groups.put(group.getId(), group);
        saveGroups();
        return group;
    }
    
    public GroupResource updateGroup(String id, GroupResource group) {
        group.setId(id);
        // Ensure meta exists and update server-controlled fields
        if (group.getMeta() != null) {
            group.getMeta().setVersion(generateVersion());
            group.getMeta().setLastModified(Calendar.getInstance());
        }
        groups.put(id, group);
        saveGroups();
        return group;
    }
    
    public GroupResource patchGroup(String id, PatchRequest patchRequest) {
        GroupResource existingGroup = groups.get(id);
        if (existingGroup == null) {
            throw new InvalidRequestException("Group with id '" + id + "' not found");
        }
        
        try {
            // Use SDK's built-in patch application with proper conversion
            GenericScimResource genericGroup = existingGroup.asGenericScimResource();
            patchRequest.apply(genericGroup);
            GroupResource modifiedGroup = JsonUtils.nodeToValue(genericGroup.getObjectNode(), GroupResource.class);
            
            // Update server-controlled fields
            if (modifiedGroup.getMeta() != null) {
                modifiedGroup.getMeta().setVersion(generateVersion());
                modifiedGroup.getMeta().setLastModified(Calendar.getInstance());
            }
            
            groups.put(id, modifiedGroup);
            saveGroups();
            return modifiedGroup;
            
        } catch (ScimException | JsonProcessingException e) {
            throw new InvalidRequestException("Error applying patch operations: " + e.getMessage());
        }
    }
    
    public boolean deleteGroup(String id) {
        boolean removed = groups.remove(id) != null;
        if (removed) {
            saveGroups();
        }
        return removed;
    }
    
    // Search operations
    public ScimSearchResult<UserResource> searchUsers(String filter, String attributes, String excludedAttributes, 
                                         String sortBy, String sortOrder, Integer startIndex, Integer count) {
        List<UserResource> allUsers = new ArrayList<>(users.values());
        
        // Apply filtering
        if (filter != null && !filter.trim().isEmpty()) {
            allUsers = sdkFilterService.filterUsers(allUsers, filter);
        }
        
        // Store total before pagination
        int totalResults = allUsers.size();
        
        // Apply sorting
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            allUsers = scimSortingService.sortUsers(allUsers, sortBy, sortOrder);
        }
        
        // Apply pagination
        int start = (startIndex != null && startIndex > 0) ? startIndex - 1 : 0;
        int end = (count != null && count > 0) ? Math.min(start + count, allUsers.size()) : allUsers.size();
        
        if (start >= allUsers.size()) {
            return new ScimSearchResult<>(new ArrayList<>(), totalResults);
        }
        
        List<UserResource> paginatedUsers = allUsers.subList(start, end);
        
        // Apply attribute selection to each user
        List<UserResource> results = paginatedUsers.stream()
                .map(user -> scimAttributeService.selectAttributes(user, attributes, excludedAttributes))
                .toList();
        
        return new ScimSearchResult<>(results, totalResults);
    }

    public ScimSearchResult<GroupResource> searchGroups(String filter, String attributes, String excludedAttributes, 
                                           String sortBy, String sortOrder, Integer startIndex, Integer count) {
        List<GroupResource> allGroups = new ArrayList<>(groups.values());
        
        // Apply filtering
        if (filter != null && !filter.trim().isEmpty()) {
            allGroups = sdkFilterService.filterGroups(allGroups, filter);
        }
        
        // Store total before pagination
        int totalResults = allGroups.size();
        
        // Apply sorting
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            allGroups = scimSortingService.sortGroups(allGroups, sortBy, sortOrder);
        }
        
        // Apply pagination
        int start = (startIndex != null && startIndex > 0) ? startIndex - 1 : 0;
        int end = (count != null && count > 0) ? Math.min(start + count, allGroups.size()) : allGroups.size();
        
        if (start >= allGroups.size()) {
            return new ScimSearchResult<>(new ArrayList<>(), totalResults);
        }
        
        List<GroupResource> paginatedGroups = allGroups.subList(start, end);
        
        // Apply attribute selection to each group
        List<GroupResource> results = paginatedGroups.stream()
                .map(group -> scimAttributeService.selectAttributes(group, attributes, excludedAttributes))
                .toList();
        
        return new ScimSearchResult<>(results, totalResults);
    }
    
    // SearchRequest-based search operations
    public ScimSearchResult<UserResource> searchUsers(SearchRequest searchRequest) {
        String filter = searchRequest.getFilter();
        String attributes = searchRequest.getAttributes() != null ? String.join(",", searchRequest.getAttributes()) : null;
        String excludedAttributes = searchRequest.getExcludedAttributes() != null ? String.join(",", searchRequest.getExcludedAttributes()) : null;
        String sortBy = searchRequest.getSortBy();
        String sortOrder = searchRequest.getSortOrder() != null ? searchRequest.getSortOrder().getName() : null;
        Integer startIndex = searchRequest.getStartIndex();
        Integer count = searchRequest.getCount();
        
        return searchUsers(filter, attributes, excludedAttributes, sortBy, sortOrder, startIndex, count);
    }

    public ScimSearchResult<GroupResource> searchGroups(SearchRequest searchRequest) {
        String filter = searchRequest.getFilter();
        String attributes = searchRequest.getAttributes() != null ? String.join(",", searchRequest.getAttributes()) : null;
        String excludedAttributes = searchRequest.getExcludedAttributes() != null ? String.join(",", searchRequest.getExcludedAttributes()) : null;
        String sortBy = searchRequest.getSortBy();
        String sortOrder = searchRequest.getSortOrder() != null ? searchRequest.getSortOrder().getName() : null;
        Integer startIndex = searchRequest.getStartIndex();
        Integer count = searchRequest.getCount();
        
        return searchGroups(filter, attributes, excludedAttributes, sortBy, sortOrder, startIndex, count);
    }
    
    public int getTotalUsers(SearchRequest searchRequest) {
        return getTotalUsers(searchRequest.getFilter());
    }
    
    public int getTotalGroups(SearchRequest searchRequest) {
        return getTotalGroups(searchRequest.getFilter());
    }
    
    // PATCH operation helper methods
    private UserResource applyPatchOperationToUser(UserResource user, PatchOperation operation) {
        try {
            Path path = operation.getPath();
            String pathStr = path != null ? path.toString() : "";
            
            switch (operation.getOpType()) {
                case ADD:
                    return addUserAttribute(user, pathStr, operation.getValue(Object.class));
                case REPLACE:
                    return replaceUserAttribute(user, pathStr, operation.getValue(Object.class));
                case REMOVE:
                    return removeUserAttribute(user, pathStr);
                default:
                    throw new InvalidRequestException("Unsupported PATCH operation: " + operation.getOpType());
            }
        } catch (Exception e) {
            throw new InvalidRequestException("Error applying PATCH operation: " + e.getMessage());
        }
    }
    
    private GroupResource applyPatchOperationToGroup(GroupResource group, PatchOperation operation) {
        try {
            Path path = operation.getPath();
            String pathStr = path != null ? path.toString() : "";
            
            Object value;
            try {
                // Try to get as Object.class first - this works for single values
                value = operation.getValue(Object.class);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("multiple values")) {
                    // For multiple values, try to get as JsonNode and convert to List
                    try {
                        com.fasterxml.jackson.databind.JsonNode jsonNode = operation.getJsonNode();
                        if (jsonNode.isArray()) {
                            // Convert JsonNode array to List of Maps
                            List<Object> list = new ArrayList<>();
                            for (com.fasterxml.jackson.databind.JsonNode element : jsonNode) {
                                if (element.isObject()) {
                                    Map<String, Object> map = new HashMap<>();
                                    var fieldNames = element.fieldNames();
                                    while (fieldNames.hasNext()) {
                                        String fieldName = fieldNames.next();
                                        map.put(fieldName, element.get(fieldName).asText());
                                    }
                                    list.add(map);
                                } else {
                                    list.add(element.asText());
                                }
                            }
                            value = list;
                        } else {
                            value = jsonNode.asText();
                        }
                    } catch (Exception e2) {
                        throw new InvalidRequestException("Error parsing patch operation value: " + e.getMessage());
                    }
                } else {
                    throw new InvalidRequestException("Error retrieving patch operation value: " + e.getMessage());
                }
            }
            
            switch (operation.getOpType()) {
                case ADD:
                    return addGroupAttribute(group, pathStr, value);
                case REPLACE:
                    return replaceGroupAttribute(group, pathStr, value);
                case REMOVE:
                    return removeGroupAttribute(group, pathStr);
                default:
                    throw new InvalidRequestException("Unsupported PATCH operation: " + operation.getOpType());
            }
        } catch (Exception e) {
            throw new InvalidRequestException("Error applying PATCH operation: " + e.getMessage());
        }
    }
    
    private UserResource addUserAttribute(UserResource user, String path, Object value) {
        // For simplicity, we'll handle common attributes
        // In a real implementation, you'd want more comprehensive path parsing
        switch (path.toLowerCase()) {
            case "displayname":
                if (value != null) {
                    user.setDisplayName(value.toString());
                }
                break;
            case "active":
                if (value instanceof Boolean) {
                    user.setActive((Boolean) value);
                }
                break;
            case "emails":
                // This is a simplified implementation
                // In practice, you'd need to handle adding to multi-valued attributes properly
                throw new InvalidRequestException("Adding to emails not implemented in this simple example");
            default:
                throw new InvalidRequestException("Unsupported path for ADD operation: " + path);
        }
        return user;
    }
    
    private UserResource replaceUserAttribute(UserResource user, String path, Object value) {
        switch (path.toLowerCase()) {
            case "displayname":
                user.setDisplayName(value != null ? value.toString() : null);
                break;
            case "active":
                if (value instanceof Boolean) {
                    user.setActive((Boolean) value);
                }
                break;
            case "username":
                if (value != null) {
                    user.setUserName(value.toString());
                }
                break;
            default:
                throw new InvalidRequestException("Unsupported path for REPLACE operation: " + path);
        }
        return user;
    }
    
    private UserResource removeUserAttribute(UserResource user, String path) {
        switch (path.toLowerCase()) {
            case "displayname":
                user.setDisplayName(null);
                break;
            case "active":
                user.setActive(null);
                break;
            default:
                throw new InvalidRequestException("Unsupported path for REMOVE operation: " + path);
        }
        return user;
    }
    
    private GroupResource addGroupAttribute(GroupResource group, String path, Object value) {
        switch (path.toLowerCase()) {
            case "displayname":
                if (value != null) {
                    group.setDisplayName(value.toString());
                }
                break;
            case "members":
                if (value != null) {
                    // Handle adding members to the group
                    List<Member> currentMembers = group.getMembers();
                    if (currentMembers == null) {
                        currentMembers = new ArrayList<>();
                    }
                    
                    // Parse the value as member(s)
                    List<Member> newMembers = parseMembersFromValue(value);
                    
                    // Validate that the users exist
                    for (Member member : newMembers) {
                        if (member.getValue() != null) {
                            UserResource user = getUserById(member.getValue());
                            if (user == null) {
                                throw new InvalidRequestException("User with id '" + member.getValue() + "' does not exist");
                            }
                        }
                    }
                    
                    // Add new members (avoid duplicates)
                    for (Member newMember : newMembers) {
                        boolean exists = currentMembers.stream()
                            .anyMatch(existing -> existing.getValue() != null && 
                                                 existing.getValue().equals(newMember.getValue()));
                        if (!exists) {
                            currentMembers.add(newMember);
                        }
                    }
                    
                    group.setMembers(currentMembers);
                }
                break;
            default:
                throw new InvalidRequestException("Unsupported path for ADD operation: " + path);
        }
        return group;
    }
    
    private GroupResource replaceGroupAttribute(GroupResource group, String path, Object value) {
        switch (path.toLowerCase()) {
            case "displayname":
                group.setDisplayName(value != null ? value.toString() : null);
                break;
            case "members":
                if (value != null) {
                    // Replace all members
                    List<Member> newMembers = parseMembersFromValue(value);
                    
                    // Validate that the users exist
                    for (Member member : newMembers) {
                        if (member.getValue() != null) {
                            UserResource user = getUserById(member.getValue());
                            if (user == null) {
                                throw new InvalidRequestException("User with id '" + member.getValue() + "' does not exist");
                            }
                        }
                    }
                    
                    group.setMembers(newMembers);
                } else {
                    group.setMembers(Collections.emptyList());
                }
                break;
            default:
                throw new InvalidRequestException("Unsupported path for REPLACE operation: " + path);
        }
        return group;
    }
    
    private GroupResource removeGroupAttribute(GroupResource group, String path) {
        // Handle filter expressions for removing specific members
        if (path.startsWith("members[") && path.endsWith("]")) {
            return removeGroupMemberByFilter(group, path);
        }
        
        switch (path.toLowerCase()) {
            case "displayname":
                group.setDisplayName(null);
                break;
            case "members":
                // Remove all members
                group.setMembers(Collections.emptyList());
                break;
            default:
                throw new InvalidRequestException("Unsupported path for REMOVE operation: " + path);
        }
        return group;
    }
    
    private GroupResource removeGroupMemberByFilter(GroupResource group, String path) {
        // Extract the filter expression from the path
        // Expected format: members[value eq "uuid"]
        String filterExpression = path.substring(8, path.length() - 1); // Remove "members[" and "]"
        
        // Parse the filter expression - for now we'll support the basic "value eq \"uuid\"" format
        String userIdToRemove = parseValueEqualsFilter(filterExpression);
        
        if (userIdToRemove == null) {
            throw new InvalidRequestException("Invalid filter expression in path: " + path);
        }
        
        // Remove the member with the specified value
        List<Member> currentMembers = group.getMembers();
        if (currentMembers != null) {
            List<Member> filteredMembers = currentMembers.stream()
                .filter(member -> !userIdToRemove.equals(member.getValue()))
                .toList();
            group.setMembers(filteredMembers);
        }
        
        return group;
    }
    
    private String parseValueEqualsFilter(String filterExpression) {
        // Handle the format: value eq "uuid"
        // This is a simplified parser for the specific case we need
        filterExpression = filterExpression.trim();
        
        if (filterExpression.startsWith("value eq ")) {
            String valuepart = filterExpression.substring(9).trim(); // Remove "value eq "
            
            // Handle quoted values: "uuid" or 'uuid'
            if ((valuepart.startsWith("\"") && valuepart.endsWith("\"")) ||
                (valuepart.startsWith("'") && valuepart.endsWith("'"))) {
                return valuepart.substring(1, valuepart.length() - 1);
            }
            
            // Handle unquoted values
            return valuepart;
        }
        
        return null; // Unsupported filter format
    }
    
    private List<Member> parseMembersFromValue(Object value) {
        List<Member> members = new ArrayList<>();
        
        try {
            if (value instanceof List) {
                // Handle array of members
                @SuppressWarnings("unchecked")
                List<Object> memberList = (List<Object>) value;
                
                for (Object memberObj : memberList) {
                    Member member = parseSingleMember(memberObj);
                    if (member != null) {
                        members.add(member);
                    }
                }
            } else {
                // Handle single member
                Member member = parseSingleMember(value);
                if (member != null) {
                    members.add(member);
                }
            }
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid member format: " + e.getMessage());
        }
        
        return members;
    }
    
    private Member parseSingleMember(Object memberObj) {
        if (memberObj == null) {
            return null;
        }
        
        Member member = new Member();
        
        if (memberObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> memberMap = (Map<String, Object>) memberObj;
            
            if (memberMap.containsKey("value")) {
                member.setValue(memberMap.get("value").toString());
            }
            if (memberMap.containsKey("display")) {
                member.setDisplay(memberMap.get("display").toString());
            }
            if (memberMap.containsKey("$ref")) {
                try {
                    member.setRef(java.net.URI.create(memberMap.get("$ref").toString()));
                } catch (Exception e) {
                    throw new InvalidRequestException("Invalid $ref URI format: " + memberMap.get("$ref"));
                }
            }
        } else if (memberObj instanceof String) {
            // Simple case: just a user ID
            member.setValue(memberObj.toString());
        } else {
            throw new InvalidRequestException("Member must be an object with 'value' property or a string user ID");
        }
        
        return member;
    }
    
    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (file.exists()) {
            try {
                List<UserResource> userList = objectMapper.readValue(file, new TypeReference<List<UserResource>>() {});
                users.clear();
                for (UserResource user : userList) {
                    users.put(user.getId(), user);
                }
            } catch (IOException e) {
                System.err.println("Error loading users from file: " + e.getMessage());
            }
        }
    }
    
    private void loadGroups() {
        File file = new File(GROUPS_FILE);
        if (file.exists()) {
            try {
                List<GroupResource> groupList = objectMapper.readValue(file, new TypeReference<List<GroupResource>>() {});
                groups.clear();
                for (GroupResource group : groupList) {
                    groups.put(group.getId(), group);
                }
            } catch (IOException e) {
                System.err.println("Error loading groups from file: " + e.getMessage());
            }
        }
    }
    
    private void saveUsers() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(new File(USERS_FILE), new ArrayList<>(users.values()));
        } catch (IOException e) {
            System.err.println("Error saving users to file: " + e.getMessage());
        }
    }
    
    private void saveGroups() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(new File(GROUPS_FILE), new ArrayList<>(groups.values()));
        } catch (IOException e) {
            System.err.println("Error saving groups to file: " + e.getMessage());
        }
    }
    
    private String generateVersion() {
        return String.valueOf(System.currentTimeMillis());
    }
}