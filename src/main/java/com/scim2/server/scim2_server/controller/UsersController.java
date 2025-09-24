package com.scim2.server.scim2_server.controller;

import com.scim2.server.scim2_server.exception.ResourceNotFoundException;
import com.scim2.server.scim2_server.exception.InvalidRequestException;
import com.scim2.server.scim2_server.service.JsonFileService;
import com.scim2.server.scim2_server.model.ScimListResponse;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.Meta;
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
    
    private final JsonFileService jsonFileService;
    
    public UsersController(JsonFileService jsonFileService) {
        this.jsonFileService = jsonFileService;
    }
    
    @Operation(summary = "List Users", description = "Retrieve all users with optional filtering and pagination")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    @GetMapping(produces = "application/scim+json")
    public ResponseEntity<ScimListResponse<UserResource>> getUsers(
            @Parameter(description = "SCIM filter expression") @RequestParam(required = false) String filter,
            @Parameter(description = "1-based index of the first result") @RequestParam(required = false) Integer startIndex,
            @Parameter(description = "Number of results per page") @RequestParam(required = false) Integer count) {
        
        List<UserResource> users = jsonFileService.searchUsers(filter, startIndex, count);
        int totalResults = jsonFileService.getAllUsers().size();
        
        ScimListResponse<UserResource> response = new ScimListResponse<>(
            totalResults,
            users,
            startIndex != null ? startIndex : 1
        );
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(response);
    }
    
    @Operation(summary = "Get User", description = "Retrieve a specific user by ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    @GetMapping(value = "/{id}", produces = "application/scim+json")
    public ResponseEntity<UserResource> getUser(@PathVariable String id) {
        UserResource user = jsonFileService.getUserById(id);
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
        
        UserResource savedUser = jsonFileService.saveUser(user);
        
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
        if (jsonFileService.getUserById(id) == null) {
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
        
        UserResource updatedUser = jsonFileService.updateUser(id, user);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(updatedUser);
    }
    
    @Operation(summary = "Delete User", description = "Delete a user")
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (!jsonFileService.deleteUser(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        
        return ResponseEntity.noContent().build();
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