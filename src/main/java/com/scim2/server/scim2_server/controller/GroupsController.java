package com.scim2.server.scim2_server.controller;

import com.scim2.server.scim2_server.exception.ResourceNotFoundException;
import com.scim2.server.scim2_server.exception.InvalidRequestException;
import com.scim2.server.scim2_server.repository.ScimRepository;
import com.scim2.server.scim2_server.model.ScimListResponse;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.messages.SearchRequest;
import com.unboundid.scim2.common.exceptions.ScimException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/scim/v2/Groups")
@Tag(name = "Groups", description = "SCIM2 Group Resource Operations")
@SecurityRequirement(name = "bearerAuth")
public class GroupsController {
    
    private final ScimRepository scimRepository;

    public GroupsController(ScimRepository scimRepository) {
        this.scimRepository = scimRepository;
    }
    
    @Operation(summary = "List Groups", description = "Retrieve all groups with optional filtering, sorting, pagination, and attribute selection")
    @ApiResponse(responseCode = "200", description = "Groups retrieved successfully")
    @GetMapping(produces = "application/scim+json")
    public ResponseEntity<ScimListResponse<GroupResource>> getGroups(
            @Parameter(description = "SCIM filter expression") @RequestParam(required = false) String filter,
            @Parameter(description = "Comma-separated list of attribute names to return") @RequestParam(required = false) String attributes,
            @Parameter(description = "Comma-separated list of attribute names to exclude") @RequestParam(required = false) String excludedAttributes,
            @Parameter(description = "Attribute name to sort by") @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort order: 'ascending' or 'descending'") @RequestParam(required = false) String sortOrder,
            @Parameter(description = "1-based index of the first result") @RequestParam(required = false) Integer startIndex,
            @Parameter(description = "Number of results per page") @RequestParam(required = false) Integer count) {
        
        // Apply SCIM default values for pagination parameters
        int effectiveStartIndex = (startIndex != null) ? startIndex : 1;
        int effectiveCount = (count != null) ? count : 100; // Default to 100 if not specified
        
        List<GroupResource> groups = scimRepository.searchGroups(filter, attributes, excludedAttributes, 
                                                                  sortBy, sortOrder, effectiveStartIndex, effectiveCount);
        int totalResults = scimRepository.getTotalGroups(filter);
        
        ScimListResponse<GroupResource> response = new ScimListResponse<>(
            totalResults,
            groups,
            effectiveStartIndex
        );
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(response);
    }
    
    @Operation(summary = "Get Group", description = "Retrieve a specific group by ID with optional attribute selection")
    @ApiResponse(responseCode = "200", description = "Group found")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    @GetMapping(value = "/{id}", produces = "application/scim+json")
    public ResponseEntity<GroupResource> getGroup(
            @PathVariable String id,
            @Parameter(description = "Comma-separated list of attribute names to return") @RequestParam(required = false) String attributes,
            @Parameter(description = "Comma-separated list of attribute names to exclude") @RequestParam(required = false) String excludedAttributes) {
        GroupResource group = scimRepository.getGroupById(id, attributes, excludedAttributes);
        if (group == null) {
            throw new ResourceNotFoundException("Group", id);
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(group);
    }
    
    @Operation(summary = "Create Group", description = "Create a new group")
    @ApiResponse(responseCode = "201", description = "Group created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    @PostMapping(consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<GroupResource> createGroup(@RequestBody GroupResource group, HttpServletRequest request) {
        // Reject requests that include an ID - IDs are server-generated
        if (group.getId() != null && !group.getId().trim().isEmpty()) {
            throw new InvalidRequestException("ID must not be provided in create requests. IDs are server-generated.");
        }
        
        // Reject requests that include meta data - meta is server-generated
        if (group.getMeta() != null) {
            throw new InvalidRequestException("Meta data must not be provided in create requests. Meta information is server-generated.");
        }
        
        validateGroup(group);
        
        // Initialize meta information
        Meta meta = new Meta();
        meta.setResourceType("Group");
        meta.setCreated(Calendar.getInstance());
        meta.setLastModified(Calendar.getInstance());
        meta.setVersion("1");
        meta.setLocation(URI.create(request.getRequestURL().toString()));
        group.setMeta(meta);
        
        GroupResource savedGroup = scimRepository.saveGroup(group);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(savedGroup);
    }
    
    @Operation(summary = "Update Group", description = "Replace a group resource")
    @ApiResponse(responseCode = "200", description = "Group updated successfully")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    @PutMapping(value = "/{id}", consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<GroupResource> updateGroup(@PathVariable String id, @RequestBody GroupResource group, HttpServletRequest request) {
        if (scimRepository.getGroupById(id) == null) {
            throw new ResourceNotFoundException("Group", id);
        }
        
        // Reject attempts to change the ID or provide a different ID
        if (group.getId() != null && !group.getId().equals(id)) {
            throw new InvalidRequestException("ID in request body must match the ID in the URL path. IDs are immutable.");
        }
        
        // Reject requests that include meta data - meta is server-generated
        if (group.getMeta() != null) {
            throw new InvalidRequestException("Meta data must not be provided in update requests. Meta information is server-generated.");
        }
        
        validateGroup(group);
        
        // Initialize meta information (always create new since client meta is rejected)
        Meta meta = new Meta();
        meta.setResourceType("Group");
        meta.setLastModified(Calendar.getInstance());
        meta.setLocation(URI.create(request.getRequestURL().toString()));
        group.setMeta(meta);
        
        GroupResource updatedGroup = scimRepository.updateGroup(id, group);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(updatedGroup);
    }
    
    @Operation(summary = "Delete Group", description = "Delete a group")
    @ApiResponse(responseCode = "204", description = "Group deleted successfully")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        if (!scimRepository.deleteGroup(id)) {
            throw new ResourceNotFoundException("Group", id);
        }
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Patch Group", description = "Partially modify a group using SCIM PATCH operations")
    @ApiResponse(responseCode = "200", description = "Group patched successfully")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid patch request", content = @Content)
    @PatchMapping(value = "/{id}", consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<GroupResource> patchGroup(@PathVariable String id, @RequestBody PatchRequest patchRequest, HttpServletRequest request) {
        GroupResource existingGroup = scimRepository.getGroupById(id);
        if (existingGroup == null) {
            throw new ResourceNotFoundException("Group", id);
        }
        
        GroupResource patchedGroup = scimRepository.patchGroup(id, patchRequest);
        
        // Update meta information
        Meta meta = patchedGroup.getMeta();
        if (meta == null) {
            meta = new Meta();
            meta.setResourceType("Group");
        }
        meta.setLastModified(Calendar.getInstance());
        meta.setLocation(URI.create(request.getRequestURL().toString()));
        patchedGroup.setMeta(meta);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(patchedGroup);
    }
    
    @PostMapping("/.search")
    public ResponseEntity<ScimListResponse<GroupResource>> searchGroups(@RequestBody SearchRequest searchRequest) throws ScimException, IOException {
        List<GroupResource> groups = scimRepository.searchGroups(searchRequest);
        int totalResults = scimRepository.getTotalGroups(searchRequest);
        
        ScimListResponse<GroupResource> response = new ScimListResponse<>();
        response.setSchemas(Collections.singletonList("urn:ietf:params:scim:api:messages:2.0:ListResponse"));
        response.setTotalResults(totalResults);
        response.setStartIndex(searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() : 1);
        response.setItemsPerPage(searchRequest.getCount() != null ? searchRequest.getCount() : totalResults);
        response.setResources(groups);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(response);
    }
    
    private void validateGroup(GroupResource group) {
        if (group.getDisplayName() == null || group.getDisplayName().trim().isEmpty()) {
            throw new InvalidRequestException("displayName is required");
        }
        
        // Validate group members according to RFC 7643
        if (group.getMembers() != null && !group.getMembers().isEmpty()) {
            validateGroupMembers(group.getMembers());
        }
    }
    
    private void validateGroupMembers(List<com.unboundid.scim2.common.types.Member> members) {
        // Keep track of member IDs to prevent duplicates
        Set<String> memberIds = new HashSet<>();
        
        for (com.unboundid.scim2.common.types.Member member : members) {
            // Validate that the member 'value' field exists and references a valid user
            if (member.getValue() == null || member.getValue().trim().isEmpty()) {
                throw new InvalidRequestException("Member 'value' field is required and must not be empty");
            }
            
            String memberId = member.getValue().trim();
            
            // Check for duplicate members in the same group
            if (memberIds.contains(memberId)) {
                throw new InvalidRequestException("Duplicate member with id '" + memberId + "' found in group. Each member can only be added once.");
            }
            memberIds.add(memberId);
            
            // Check if the user with this ID exists in the system
            if (scimRepository.getUserById(memberId) == null) {
                throw new InvalidRequestException("Member with id '" + memberId + "' does not exist in the system");
            }
            
            // Validate $ref field if provided
            if (member.getRef() != null && !member.getRef().toString().trim().isEmpty()) {
                validateMemberRef(memberId, member.getRef().toString());
            }
        }
    }
    
    private void validateMemberRef(String memberId, String ref) {
        // Parse the $ref URL to extract the resource ID and type
        try {
            java.net.URI refUri = java.net.URI.create(ref);
            String path = refUri.getPath();
            
            // Expected format: .../Users/{id} or .../v2/Users/{id}
            if (path == null || !path.contains("/Users/")) {
                throw new InvalidRequestException("Member $ref must point to a Users resource. Invalid format: " + ref);
            }
            
            // Extract the user ID from the path
            String[] pathSegments = path.split("/");
            String refUserId = null;
            for (int i = 0; i < pathSegments.length - 1; i++) {
                if ("Users".equals(pathSegments[i]) && i + 1 < pathSegments.length) {
                    refUserId = pathSegments[i + 1];
                    break;
                }
            }
            
            if (refUserId == null || refUserId.trim().isEmpty()) {
                throw new InvalidRequestException("Cannot extract user ID from $ref: " + ref);
            }
            
            // Verify that the ID in $ref matches the value field
            if (!refUserId.equals(memberId)) {
                throw new InvalidRequestException("Member $ref ID '" + refUserId + "' does not match member value '" + memberId + "'");
            }
            
            // Verify that the referenced user exists
            if (scimRepository.getUserById(refUserId) == null) {
                throw new InvalidRequestException("User referenced in $ref with id '" + refUserId + "' does not exist in the system");
            }
            
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid $ref URI format: " + ref + ". Error: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidRequestException("Error validating $ref: " + ref + ". Error: " + e.getMessage());
        }
    }
}