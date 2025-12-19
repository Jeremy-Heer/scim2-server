package com.scim2.server.scim2_server.controller;

import com.scim2.server.scim2_server.exception.ResourceNotFoundException;
import com.scim2.server.scim2_server.exception.InvalidRequestException;
import com.scim2.server.scim2_server.repository.ScimRepository;
import com.scim2.server.scim2_server.model.ScimListResponse;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.Meta;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.messages.SearchRequest;
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

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Calendar;
import java.util.List;

@RestController
@RequestMapping("/scim/v2/Users")
@Tag(name = "Users", description = "SCIM2 User Resource Operations")
@SecurityRequirement(name = "bearerAuth")
public class UsersController {
    
    private final ScimRepository scimRepository;

    public UsersController(ScimRepository scimRepository) {
        this.scimRepository = scimRepository;
    }
    
    @Operation(summary = "List Users", description = "Retrieve all users with optional filtering, sorting, pagination, and attribute selection")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping(produces = "application/scim+json")
    public ResponseEntity<ScimListResponse<UserResource>> getUsers(
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
        
        List<UserResource> users = scimRepository.searchUsers(filter, attributes, excludedAttributes, 
                                                               sortBy, sortOrder, effectiveStartIndex, effectiveCount);
        int totalResults = scimRepository.getTotalUsers(filter);
        
        ScimListResponse<UserResource> response = new ScimListResponse<>(
            totalResults,
            users,
            effectiveStartIndex
        );
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(response);
    }
    
    @Operation(summary = "Get User", description = "Retrieve a specific user by ID with optional attribute selection")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    @GetMapping(value = "/{id}", produces = "application/scim+json")
    public ResponseEntity<UserResource> getUser(
            @PathVariable String id,
            @Parameter(description = "Comma-separated list of attribute names to return") @RequestParam(required = false) String attributes,
            @Parameter(description = "Comma-separated list of attribute names to exclude") @RequestParam(required = false) String excludedAttributes) {
        UserResource user = scimRepository.getUserById(id, attributes, excludedAttributes);
        if (user == null) {
            throw new ResourceNotFoundException("User", id);
        }
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(user);
    }
    
    @Operation(summary = "Create User", description = "Create a new user")
    @ApiResponse(responseCode = "201", description = "User created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    @PostMapping(consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<UserResource> createUser(@RequestBody UserResource user, HttpServletRequest request) {
        // Reject requests that include an ID - IDs are server-generated
        if (user.getId() != null && !user.getId().trim().isEmpty()) {
            throw new InvalidRequestException("ID must not be provided in create requests. IDs are server-generated.");
        }
        
        // Reject requests that include meta data - meta is server-generated
        if (user.getMeta() != null) {
            throw new InvalidRequestException("Meta data must not be provided in create requests. Meta information is server-generated.");
        }
        
        validateUser(user);
        
        // Initialize meta information
        Meta meta = new Meta();
        meta.setResourceType("User");
        meta.setCreated(Calendar.getInstance());
        meta.setLastModified(Calendar.getInstance());
        meta.setVersion("1");
        meta.setLocation(URI.create(request.getRequestURL().toString()));
        user.setMeta(meta);
        
        UserResource savedUser = scimRepository.saveUser(user);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(savedUser);
    }
    
    @Operation(summary = "Update User", description = "Replace a user resource")
    @ApiResponse(responseCode = "200", description = "User updated successfully")
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
    @PutMapping(value = "/{id}", consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<UserResource> updateUser(@PathVariable String id, @RequestBody UserResource user, HttpServletRequest request) {
        if (scimRepository.getUserById(id) == null) {
            throw new ResourceNotFoundException("User", id);
        }
        
        // Reject attempts to change the ID or provide a different ID
        if (user.getId() != null && !user.getId().equals(id)) {
            throw new InvalidRequestException("ID in request body must match the ID in the URL path. IDs are immutable.");
        }
        
        // Reject requests that include meta data - meta is server-generated
        if (user.getMeta() != null) {
            throw new InvalidRequestException("Meta data must not be provided in update requests. Meta information is server-generated.");
        }
        
        validateUser(user);
        
        // Initialize meta information (always create new since client meta is rejected)
        Meta meta = new Meta();
        meta.setResourceType("User");
        meta.setLastModified(Calendar.getInstance());
        meta.setLocation(URI.create(request.getRequestURL().toString()));
        user.setMeta(meta);
        
        UserResource updatedUser = scimRepository.updateUser(id, user);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(updatedUser);
    }
    
    @Operation(summary = "Delete User", description = "Delete a user")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (!scimRepository.deleteUser(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Patch User", description = "Partially modify a user using SCIM PATCH operations")
    @ApiResponse(responseCode = "200", description = "User patched successfully")
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid patch request", content = @Content)
    @PatchMapping(value = "/{id}", consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<UserResource> patchUser(@PathVariable String id, @RequestBody PatchRequest patchRequest, HttpServletRequest request) {
        UserResource existingUser = scimRepository.getUserById(id);
        if (existingUser == null) {
            throw new ResourceNotFoundException("User", id);
        }
        
        UserResource patchedUser = scimRepository.patchUser(id, patchRequest);
        
        // Update meta information
        Meta meta = patchedUser.getMeta();
        if (meta == null) {
            meta = new Meta();
            meta.setResourceType("User");
        }
        meta.setLastModified(Calendar.getInstance());
        meta.setLocation(URI.create(request.getRequestURL().toString()));
        patchedUser.setMeta(meta);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(patchedUser);
    }
    
    @Operation(summary = "Search Users using POST", description = "Search users using HTTP POST with a SearchRequest body, as per RFC 7644 section 3.4.3")
    @ApiResponse(responseCode = "200", description = "Search completed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid search request", content = @Content)
    @PostMapping(value = "/.search", consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<ScimListResponse<UserResource>> searchUsers(@RequestBody SearchRequest searchRequest) {
        List<UserResource> users = scimRepository.searchUsers(searchRequest);
        int totalResults = scimRepository.getTotalUsers(searchRequest);
        
        ScimListResponse<UserResource> response = new ScimListResponse<>(
            totalResults,
            users,
            searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() : 1
        );
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(response);
    }
    
    private void validateUser(UserResource user) {
        if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            throw new InvalidRequestException("userName is required");
        }
        
        // Add additional validation as needed
        if (user.getEmails() != null && !user.getEmails().isEmpty()) {
            for (var email : user.getEmails()) {
                if (email.getValue() == null || !email.getValue().contains("@")) {
                    throw new InvalidRequestException("Invalid email format");
                }
            }
        }
    }
}