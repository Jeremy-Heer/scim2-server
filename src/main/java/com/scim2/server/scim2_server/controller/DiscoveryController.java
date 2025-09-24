package com.scim2.server.scim2_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/scim/v2")
@Tag(name = "Discovery", description = "SCIM2 Discovery Endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DiscoveryController {
    
    @Operation(summary = "Service Provider Configuration", description = "Returns the service provider configuration")
    @ApiResponse(responseCode = "200", description = "Service provider configuration retrieved successfully")
    @GetMapping(value = "/ServiceProviderConfig", produces = "application/scim+json")
    public ResponseEntity<Map<String, Object>> getServiceProviderConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"));
        config.put("documentationUri", "https://tools.ietf.org/html/rfc7644");
        
        Map<String, Object> patch = new HashMap<>();
        patch.put("supported", true);
        config.put("patch", patch);
        
        Map<String, Object> bulk = new HashMap<>();
        bulk.put("supported", true);
        bulk.put("maxOperations", 1000);
        bulk.put("maxPayloadSize", 1048576);
        config.put("bulk", bulk);
        
        Map<String, Object> filter = new HashMap<>();
        filter.put("supported", true);
        filter.put("maxResults", 200);
        config.put("filter", filter);
        
        Map<String, Object> changePassword = new HashMap<>();
        changePassword.put("supported", false);
        config.put("changePassword", changePassword);
        
        Map<String, Object> sort = new HashMap<>();
        sort.put("supported", true);
        config.put("sort", sort);
        
        Map<String, Object> etag = new HashMap<>();
        etag.put("supported", false);
        config.put("etag", etag);
        
        List<Map<String, Object>> authenticationSchemes = new ArrayList<>();
        Map<String, Object> bearerToken = new HashMap<>();
        bearerToken.put("type", "httpbearer");
        bearerToken.put("name", "Bearer Token");
        bearerToken.put("description", "Authentication scheme using Bearer token");
        bearerToken.put("specUri", "https://tools.ietf.org/html/rfc6750");
        bearerToken.put("primary", true);
        authenticationSchemes.add(bearerToken);
        config.put("authenticationSchemes", authenticationSchemes);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(config);
    }
    
    @Operation(summary = "Resource Types", description = "Returns supported resource types")
    @ApiResponse(responseCode = "200", description = "Resource types retrieved successfully")
    @GetMapping(value = "/ResourceTypes", produces = "application/scim+json")
    public ResponseEntity<List<Map<String, Object>>> getResourceTypes() {
        List<Map<String, Object>> resourceTypes = new ArrayList<>();
        
        // User Resource Type
        Map<String, Object> userResourceType = new HashMap<>();
        userResourceType.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"));
        userResourceType.put("id", "User");
        userResourceType.put("name", "User");
        userResourceType.put("endpoint", "/Users");
        userResourceType.put("description", "User Account");
        userResourceType.put("schema", "urn:ietf:params:scim:schemas:core:2.0:User");
        userResourceType.put("schemaExtensions", List.of(
            Map.of("schema", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User", "required", false)
        ));
        resourceTypes.add(userResourceType);
        
        // Group Resource Type
        Map<String, Object> groupResourceType = new HashMap<>();
        groupResourceType.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:ResourceType"));
        groupResourceType.put("id", "Group");
        groupResourceType.put("name", "Group");
        groupResourceType.put("endpoint", "/Groups");
        groupResourceType.put("description", "Group");
        groupResourceType.put("schema", "urn:ietf:params:scim:schemas:core:2.0:Group");
        resourceTypes.add(groupResourceType);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(resourceTypes);
    }
    
    @Operation(summary = "Schemas", description = "Returns supported schemas")
    @ApiResponse(responseCode = "200", description = "Schemas retrieved successfully")
    @GetMapping(value = "/Schemas", produces = "application/scim+json")
    public ResponseEntity<List<Map<String, Object>>> getSchemas() {
        List<Map<String, Object>> schemas = new ArrayList<>();
        
        // User Schema
        Map<String, Object> userSchema = new HashMap<>();
        userSchema.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:Schema"));
        userSchema.put("id", "urn:ietf:params:scim:schemas:core:2.0:User");
        userSchema.put("name", "User");
        userSchema.put("description", "User Account");
        
        List<Map<String, Object>> userAttributes = new ArrayList<>();
        userAttributes.add(createAttribute("userName", "string", "Unique identifier", true, false, "readWrite", null));
        userAttributes.add(createAttribute("name", "complex", "Full name", false, false, "readWrite", 
            List.of(
                createAttribute("formatted", "string", "Formatted name", false, false, "readWrite", null),
                createAttribute("familyName", "string", "Family name", false, false, "readWrite", null),
                createAttribute("givenName", "string", "Given name", false, false, "readWrite", null)
            )
        ));
        userAttributes.add(createAttribute("displayName", "string", "Display name", false, false, "readWrite", null));
        userAttributes.add(createAttribute("emails", "complex", "Email addresses", false, true, "readWrite",
            List.of(
                createAttribute("value", "string", "Email address", false, false, "readWrite", null),
                createAttribute("type", "string", "Email type", false, false, "readWrite", null),
                createAttribute("primary", "boolean", "Primary email", false, false, "readWrite", null)
            )
        ));
        userAttributes.add(createAttribute("active", "boolean", "Active status", false, false, "readWrite", null));
        
        userSchema.put("attributes", userAttributes);
        schemas.add(userSchema);
        
        // Group Schema
        Map<String, Object> groupSchema = new HashMap<>();
        groupSchema.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:Schema"));
        groupSchema.put("id", "urn:ietf:params:scim:schemas:core:2.0:Group");
        groupSchema.put("name", "Group");
        groupSchema.put("description", "Group");
        
        List<Map<String, Object>> groupAttributes = new ArrayList<>();
        groupAttributes.add(createAttribute("displayName", "string", "Display name", true, false, "readWrite", null));
        groupAttributes.add(createAttribute("members", "complex", "Group members", false, true, "readWrite",
            List.of(
                createAttribute("value", "string", "Member ID", false, false, "immutable", null),
                createAttribute("$ref", "reference", "Member reference", false, false, "immutable", null),
                createAttribute("type", "string", "Member type", false, false, "immutable", null)
            )
        ));
        
        groupSchema.put("attributes", groupAttributes);
        schemas.add(groupSchema);
        
        // Enterprise User Extension Schema
        Map<String, Object> enterpriseUserSchema = new HashMap<>();
        enterpriseUserSchema.put("schemas", List.of("urn:ietf:params:scim:schemas:core:2.0:Schema"));
        enterpriseUserSchema.put("id", "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
        enterpriseUserSchema.put("name", "EnterpriseUser");
        enterpriseUserSchema.put("description", "Enterprise User Extension");
        
        List<Map<String, Object>> enterpriseAttributes = new ArrayList<>();
        enterpriseAttributes.add(createAttribute("employeeNumber", "string", "Employee number", false, false, "readWrite", null));
        enterpriseAttributes.add(createAttribute("costCenter", "string", "Cost center", false, false, "readWrite", null));
        enterpriseAttributes.add(createAttribute("organization", "string", "Organization", false, false, "readWrite", null));
        enterpriseAttributes.add(createAttribute("division", "string", "Division", false, false, "readWrite", null));
        enterpriseAttributes.add(createAttribute("department", "string", "Department", false, false, "readWrite", null));
        enterpriseAttributes.add(createAttribute("manager", "complex", "Manager", false, false, "readOnly",
            List.of(
                createAttribute("value", "string", "Manager ID", false, false, "readOnly", null),
                createAttribute("$ref", "reference", "Manager reference", false, false, "readOnly", null),
                createAttribute("displayName", "string", "Manager display name", false, false, "readOnly", null)
            )
        ));
        
        enterpriseUserSchema.put("attributes", enterpriseAttributes);
        schemas.add(enterpriseUserSchema);
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/scim+json"))
            .body(schemas);
    }
    
    private Map<String, Object> createAttribute(String name, String type, String description, 
                                                boolean required, boolean multiValued, String mutability, 
                                                List<Map<String, Object>> subAttributes) {
        Map<String, Object> attribute = new HashMap<>();
        attribute.put("name", name);
        attribute.put("type", type);
        attribute.put("description", description);
        attribute.put("required", required);
        attribute.put("multiValued", multiValued);
        attribute.put("mutability", mutability);
        attribute.put("returned", "default");
        attribute.put("uniqueness", "none");
        if (subAttributes != null) {
            attribute.put("subAttributes", subAttributes);
        }
        return attribute;
    }
}