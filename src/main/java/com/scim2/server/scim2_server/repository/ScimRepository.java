package com.scim2.server.scim2_server.repository;

import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.messages.SearchRequest;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.UserResource;

import java.util.Collection;
import java.util.List;

/**
 * Repository interface for SCIM resource storage operations.
 * Provides abstraction layer for different backend implementations (JSON file, LDAP, etc.)
 * 
 * This interface defines the contract for SCIM2 RFC 7644 compliant operations on User and Group resources.
 */
public interface ScimRepository {
    
    // ========== Initialization ==========
    
    /**
     * Initialize the repository backend (e.g., load files, connect to LDAP).
     * Called automatically after bean construction.
     */
    void init();
    
    // ========== User CRUD Operations ==========
    
    /**
     * Retrieve all users without filtering.
     * 
     * @return Collection of all user resources
     */
    Collection<UserResource> getAllUsers();
    
    /**
     * Retrieve a user by SCIM ID.
     * 
     * @param id SCIM user ID (UUID)
     * @return UserResource or null if not found
     */
    UserResource getUserById(String id);
    
    /**
     * Retrieve a user by SCIM ID with attribute selection.
     * 
     * @param id SCIM user ID (UUID)
     * @param attributes Comma-separated list of attributes to include
     * @param excludedAttributes Comma-separated list of attributes to exclude
     * @return UserResource with selected attributes or null if not found
     */
    UserResource getUserById(String id, String attributes, String excludedAttributes);
    
    /**
     * Get total count of users matching the optional filter.
     * 
     * @param filter SCIM filter expression (e.g., "userName eq \"john\"") or null for all users
     * @return Total count of matching users
     */
    int getTotalUsers(String filter);
    
    /**
     * Get total count of users matching the search request filter.
     * 
     * @param searchRequest SCIM SearchRequest with filter
     * @return Total count of matching users
     */
    int getTotalUsers(SearchRequest searchRequest);
    
    /**
     * Create a new user (POST operation).
     * Generates a new UUID and sets server-controlled metadata.
     * 
     * @param user UserResource to create (ID will be auto-generated)
     * @return Created UserResource with generated ID and metadata
     */
    UserResource saveUser(UserResource user);
    
    /**
     * Update an existing user (PUT operation).
     * Replaces entire resource at the specified ID.
     * 
     * @param id SCIM user ID
     * @param user UserResource with updated values
     * @return Updated UserResource
     */
    UserResource updateUser(String id, UserResource user);
    
    /**
     * Partially update a user (PATCH operation).
     * Applies SCIM PATCH operations (add, remove, replace) to the resource.
     * 
     * @param id SCIM user ID
     * @param patchRequest SCIM PatchRequest with operations
     * @return Patched UserResource
     */
    UserResource patchUser(String id, PatchRequest patchRequest);
    
    /**
     * Delete a user (DELETE operation).
     * 
     * @param id SCIM user ID
     * @return true if deleted successfully, false if not found
     */
    boolean deleteUser(String id);
    
    /**
     * Search users with filtering, sorting, pagination, and attribute selection.
     * 
     * @param filter SCIM filter expression or null
     * @param attributes Comma-separated attributes to include
     * @param excludedAttributes Comma-separated attributes to exclude
     * @param sortBy Attribute name to sort by
     * @param sortOrder "ascending" or "descending"
     * @param startIndex 1-based start index for pagination
     * @param count Number of results per page
     * @return List of matching UserResources
     */
    List<UserResource> searchUsers(String filter, String attributes, String excludedAttributes,
                                   String sortBy, String sortOrder, int startIndex, int count);
    
    /**
     * Search users using SCIM SearchRequest (POST .search).
     * 
     * @param searchRequest SCIM SearchRequest with all search parameters
     * @return List of matching UserResources
     */
    List<UserResource> searchUsers(SearchRequest searchRequest);
    
    // ========== Group CRUD Operations ==========
    
    /**
     * Retrieve all groups without filtering.
     * 
     * @return Collection of all group resources
     */
    Collection<GroupResource> getAllGroups();
    
    /**
     * Retrieve a group by SCIM ID.
     * 
     * @param id SCIM group ID (UUID)
     * @return GroupResource or null if not found
     */
    GroupResource getGroupById(String id);
    
    /**
     * Retrieve a group by SCIM ID with attribute selection.
     * 
     * @param id SCIM group ID (UUID)
     * @param attributes Comma-separated list of attributes to include
     * @param excludedAttributes Comma-separated list of attributes to exclude
     * @return GroupResource with selected attributes or null if not found
     */
    GroupResource getGroupById(String id, String attributes, String excludedAttributes);
    
    /**
     * Get total count of groups matching the optional filter.
     * 
     * @param filter SCIM filter expression or null for all groups
     * @return Total count of matching groups
     */
    int getTotalGroups(String filter);
    
    /**
     * Get total count of groups matching the search request filter.
     * 
     * @param searchRequest SCIM SearchRequest with filter
     * @return Total count of matching groups
     */
    int getTotalGroups(SearchRequest searchRequest);
    
    /**
     * Create a new group (POST operation).
     * Generates a new UUID and sets server-controlled metadata.
     * 
     * @param group GroupResource to create (ID will be auto-generated)
     * @return Created GroupResource with generated ID and metadata
     */
    GroupResource saveGroup(GroupResource group);
    
    /**
     * Update an existing group (PUT operation).
     * Replaces entire resource at the specified ID.
     * 
     * @param id SCIM group ID
     * @param group GroupResource with updated values
     * @return Updated GroupResource
     */
    GroupResource updateGroup(String id, GroupResource group);
    
    /**
     * Partially update a group (PATCH operation).
     * Applies SCIM PATCH operations to the resource, including member add/remove.
     * 
     * @param id SCIM group ID
     * @param patchRequest SCIM PatchRequest with operations
     * @return Patched GroupResource
     */
    GroupResource patchGroup(String id, PatchRequest patchRequest);
    
    /**
     * Delete a group (DELETE operation).
     * 
     * @param id SCIM group ID
     * @return true if deleted successfully, false if not found
     */
    boolean deleteGroup(String id);
    
    /**
     * Search groups with filtering, sorting, pagination, and attribute selection.
     * 
     * @param filter SCIM filter expression or null
     * @param attributes Comma-separated attributes to include
     * @param excludedAttributes Comma-separated attributes to exclude
     * @param sortBy Attribute name to sort by
     * @param sortOrder "ascending" or "descending"
     * @param startIndex 1-based start index for pagination
     * @param count Number of results per page
     * @return List of matching GroupResources
     */
    List<GroupResource> searchGroups(String filter, String attributes, String excludedAttributes,
                                     String sortBy, String sortOrder, int startIndex, int count);
    
    /**
     * Search groups using SCIM SearchRequest (POST .search).
     * 
     * @param searchRequest SCIM SearchRequest with all search parameters
     * @return List of matching GroupResources
     */
    List<GroupResource> searchGroups(SearchRequest searchRequest);
}
