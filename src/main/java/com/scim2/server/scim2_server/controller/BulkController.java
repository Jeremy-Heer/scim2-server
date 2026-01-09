package com.scim2.server.scim2_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scim2.server.scim2_server.exception.InvalidRequestException;
import com.scim2.server.scim2_server.model.BulkOperation;
import com.scim2.server.scim2_server.model.BulkRequest;
import com.scim2.server.scim2_server.model.BulkResponse;
import com.scim2.server.scim2_server.repository.ScimRepository;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.UserResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/scim/v2/Bulk")
@Tag(name = "Bulk", description = "SCIM2 Bulk Operations")
@SecurityRequirement(name = "bearerAuth")
public class BulkController {
    
    private final ScimRepository scimRepository;
    private final ObjectMapper objectMapper;
    
    public BulkController(ScimRepository scimRepository, ObjectMapper objectMapper) {
        this.scimRepository = scimRepository;
        this.objectMapper = objectMapper;
    }
    
    @Operation(summary = "Bulk Operations", description = "Process bulk create, update, delete operations")
    @ApiResponse(responseCode = "200", description = "Bulk operations processed")
    @ApiResponse(responseCode = "400", description = "Invalid bulk request", content = @Content)
    @PostMapping(consumes = "application/scim+json", produces = "application/scim+json")
    public ResponseEntity<BulkResponse> processBulk(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Bulk request with operations to perform", required = true,
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "schemas": ["urn:ietf:params:scim:api:messages:2.0:BulkRequest"],
                              "failOnErrors": 1,
                              "Operations": [
                                {
                                  "method": "POST",
                                  "path": "/Users",
                                  "bulkId": "qwerty",
                                  "data": {
                                    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
                                    "userName": "alice.smith",
                                    "name": {
                                      "givenName": "Alice",
                                      "familyName": "Smith"
                                    },
                                    "emails": [
                                      {
                                        "value": "alice.smith@example.com",
                                        "type": "work",
                                        "primary": true
                                      }
                                    ],
                                    "active": true
                                  }
                                },
                                {
                                  "method": "POST",
                                  "path": "/Groups",
                                  "bulkId": "ytrewq",
                                  "data": {
                                    "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"],
                                    "displayName": "Test Group"
                                  }
                                }
                              ]
                            }
                            """)))
            @RequestBody BulkRequest bulkRequest, HttpServletRequest request) {
        
        if (bulkRequest.getOperations() == null || bulkRequest.getOperations().isEmpty()) {
            throw new InvalidRequestException("No operations provided in bulk request");
        }
        
        BulkResponse bulkResponse = new BulkResponse();
        List<BulkOperation> responseOperations = new ArrayList<>();
        
        int errorCount = 0;
        Integer failOnErrors = bulkRequest.getFailOnErrors();
        
        for (BulkOperation operation : bulkRequest.getOperations()) {
            BulkOperation responseOp = new BulkOperation();
            responseOp.setBulkId(operation.getBulkId());
            
            try {
                processOperation(operation, responseOp, request);
            } catch (Exception e) {
                errorCount++;
                responseOp.setStatus("400");
                responseOp.setResponse("{\"detail\":\"" + e.getMessage() + "\"}");
                
                if (failOnErrors != null && errorCount >= failOnErrors) {
                    responseOperations.add(responseOp);
                    break;
                }
            }
            
            responseOperations.add(responseOp);
        }
        
        bulkResponse.setOperations(responseOperations);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(bulkResponse);
    }
    
    private void processOperation(BulkOperation operation, BulkOperation responseOp, HttpServletRequest request) throws Exception {
        String method = operation.getMethod();
        String path = operation.getPath();
        Object data = operation.getData();
        
        if (method == null || path == null) {
            throw new InvalidRequestException("Method and path are required for bulk operations");
        }
        
        switch (method.toUpperCase()) {
            case "POST":
                handlePost(path, data, responseOp, request);
                break;
            case "PUT":
                handlePut(path, data, responseOp, request);
                break;
            case "PATCH":
                handlePatch(path, data, responseOp, request);
                break;
            case "DELETE":
                handleDelete(path, responseOp);
                break;
            default:
                throw new InvalidRequestException("Unsupported method: " + method);
        }
    }
    
    private void handlePost(String path, Object data, BulkOperation responseOp, HttpServletRequest request) throws Exception {
        if (path.equals("/Users")) {
            UserResource user = objectMapper.convertValue(data, UserResource.class);
            UserResource savedUser = scimRepository.saveUser(user);
            responseOp.setStatus("201");
            responseOp.setLocation("/scim/v2/Users/" + savedUser.getId());
            responseOp.setResponse(savedUser);
        } else if (path.equals("/Groups")) {
            GroupResource group = objectMapper.convertValue(data, GroupResource.class);
            GroupResource savedGroup = scimRepository.saveGroup(group);
            responseOp.setStatus("201");
            responseOp.setLocation("/scim/v2/Groups/" + savedGroup.getId());
            responseOp.setResponse(savedGroup);
        } else {
            throw new InvalidRequestException("Unsupported resource type for POST: " + path);
        }
    }
    
    private void handlePut(String path, Object data, BulkOperation responseOp, HttpServletRequest request) throws Exception {
        Pattern userPattern = Pattern.compile("/Users/(.+)");
        Pattern groupPattern = Pattern.compile("/Groups/(.+)");
        
        Matcher userMatcher = userPattern.matcher(path);
        Matcher groupMatcher = groupPattern.matcher(path);
        
        if (userMatcher.matches()) {
            String userId = userMatcher.group(1);
            UserResource user = objectMapper.convertValue(data, UserResource.class);
            UserResource updatedUser = scimRepository.updateUser(userId, user);
            responseOp.setStatus("200");
            responseOp.setLocation("/scim/v2/Users/" + userId);
            responseOp.setResponse(updatedUser);
        } else if (groupMatcher.matches()) {
            String groupId = groupMatcher.group(1);
            GroupResource group = objectMapper.convertValue(data, GroupResource.class);
            GroupResource updatedGroup = scimRepository.updateGroup(groupId, group);
            responseOp.setStatus("200");
            responseOp.setLocation("/scim/v2/Groups/" + groupId);
            responseOp.setResponse(updatedGroup);
        } else {
            throw new InvalidRequestException("Invalid path for PUT operation: " + path);
        }
    }
    
    private void handlePatch(String path, Object data, BulkOperation responseOp, HttpServletRequest request) throws Exception {
        // For simplicity, treating PATCH like PUT in this implementation
        // In a real implementation, you'd need to parse PATCH operations properly
        handlePut(path, data, responseOp, request);
    }
    
    private void handleDelete(String path, BulkOperation responseOp) throws Exception {
        Pattern userPattern = Pattern.compile("/Users/(.+)");
        Pattern groupPattern = Pattern.compile("/Groups/(.+)");
        
        Matcher userMatcher = userPattern.matcher(path);
        Matcher groupMatcher = groupPattern.matcher(path);
        
        if (userMatcher.matches()) {
            String userId = userMatcher.group(1);
            if (scimRepository.deleteUser(userId)) {
                responseOp.setStatus("204");
            } else {
                responseOp.setStatus("404");
                responseOp.setResponse("{\"detail\":\"User not found\"}");
            }
        } else if (groupMatcher.matches()) {
            String groupId = groupMatcher.group(1);
            if (scimRepository.deleteGroup(groupId)) {
                responseOp.setStatus("204");
            } else {
                responseOp.setStatus("404");
                responseOp.setResponse("{\"detail\":\"Group not found\"}");
            }
        } else {
            throw new InvalidRequestException("Invalid path for DELETE operation: " + path);
        }
    }
}