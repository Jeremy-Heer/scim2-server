package com.scim2.server.scim2_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.GroupResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JsonFileService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String DATA_DIR = "data";
    private final String USERS_FILE = DATA_DIR + "/users.json";
    private final String GROUPS_FILE = DATA_DIR + "/groups.json";
    
    private final Map<String, UserResource> users = new ConcurrentHashMap<>();
    private final Map<String, GroupResource> groups = new ConcurrentHashMap<>();
    
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
    
    public UserResource saveUser(UserResource user) {
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
    
    public GroupResource saveGroup(GroupResource group) {
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
    
    public boolean deleteGroup(String id) {
        boolean removed = groups.remove(id) != null;
        if (removed) {
            saveGroups();
        }
        return removed;
    }
    
    // Search operations
    public List<UserResource> searchUsers(String filter, Integer startIndex, Integer count) {
        List<UserResource> allUsers = new ArrayList<>(users.values());
        
        // Apply simple filtering (for demonstration - in real implementation you'd parse SCIM filter)
        if (filter != null && !filter.isEmpty()) {
            allUsers = allUsers.stream()
                .filter(user -> matchesFilter(user, filter))
                .toList();
        }
        
        // Apply pagination
        int start = (startIndex != null && startIndex > 0) ? startIndex - 1 : 0;
        int end = (count != null && count > 0) ? Math.min(start + count, allUsers.size()) : allUsers.size();
        
        if (start >= allUsers.size()) {
            return new ArrayList<>();
        }
        
        return allUsers.subList(start, end);
    }
    
    public List<GroupResource> searchGroups(String filter, Integer startIndex, Integer count) {
        List<GroupResource> allGroups = new ArrayList<>(groups.values());
        
        // Apply simple filtering
        if (filter != null && !filter.isEmpty()) {
            allGroups = allGroups.stream()
                .filter(group -> matchesGroupFilter(group, filter))
                .toList();
        }
        
        // Apply pagination
        int start = (startIndex != null && startIndex > 0) ? startIndex - 1 : 0;
        int end = (count != null && count > 0) ? Math.min(start + count, allGroups.size()) : allGroups.size();
        
        if (start >= allGroups.size()) {
            return new ArrayList<>();
        }
        
        return allGroups.subList(start, end);
    }
    
    private boolean matchesFilter(UserResource user, String filter) {
        // Simple contains-based filtering - in real implementation parse SCIM filter syntax
        String lowerFilter = filter.toLowerCase();
        return (user.getUserName() != null && user.getUserName().toLowerCase().contains(lowerFilter)) ||
               (user.getName() != null && user.getName().getFamilyName() != null && 
                user.getName().getFamilyName().toLowerCase().contains(lowerFilter)) ||
               (user.getName() != null && user.getName().getGivenName() != null && 
                user.getName().getGivenName().toLowerCase().contains(lowerFilter));
    }
    
    private boolean matchesGroupFilter(GroupResource group, String filter) {
        String lowerFilter = filter.toLowerCase();
        return group.getDisplayName() != null && group.getDisplayName().toLowerCase().contains(lowerFilter);
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