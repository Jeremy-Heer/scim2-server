package com.scim2.server.scim2_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.GroupResource;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for handling SCIM attribute selection (attributes and excludedAttributes parameters)
 */
@Service
public class ScimAttributeService {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Apply attribute selection to a UserResource
     * @param user The user resource
     * @param attributes Comma-separated list of attributes to include (null = include all)
     * @param excludedAttributes Comma-separated list of attributes to exclude (null = exclude none)
     * @return User resource with selected attributes
     */
    public UserResource selectAttributes(UserResource user, String attributes, String excludedAttributes) {
        if (user == null) {
            return null;
        }
        
        // If no attribute selection specified, return as-is
        if ((attributes == null || attributes.trim().isEmpty()) && 
            (excludedAttributes == null || excludedAttributes.trim().isEmpty())) {
            return user;
        }
        
        try {
            // Convert to JSON for manipulation
            JsonNode userJson = objectMapper.valueToTree(user);
            
            if (attributes != null && !attributes.trim().isEmpty()) {
                // Include only specified attributes
                Set<String> includeSet = parseAttributeList(attributes);
                userJson = includeOnlyAttributes(userJson, includeSet);
            } else if (excludedAttributes != null && !excludedAttributes.trim().isEmpty()) {
                // Exclude specified attributes
                Set<String> excludeSet = parseAttributeList(excludedAttributes);
                userJson = excludeAttributes(userJson, excludeSet);
            }
            
            // Convert back to UserResource
            return objectMapper.treeToValue(userJson, UserResource.class);
            
        } catch (Exception e) {
            // If there's an error, return the original user
            return user;
        }
    }
    
    /**
     * Apply attribute selection to a GroupResource
     * @param group The group resource
     * @param attributes Comma-separated list of attributes to include (null = include all)
     * @param excludedAttributes Comma-separated list of attributes to exclude (null = exclude none)
     * @return Group resource with selected attributes
     */
    public GroupResource selectAttributes(GroupResource group, String attributes, String excludedAttributes) {
        if (group == null) {
            return null;
        }
        
        // If no attribute selection specified, return as-is
        if ((attributes == null || attributes.trim().isEmpty()) && 
            (excludedAttributes == null || excludedAttributes.trim().isEmpty())) {
            return group;
        }
        
        try {
            // Convert to JSON for manipulation
            JsonNode groupJson = objectMapper.valueToTree(group);
            
            if (attributes != null && !attributes.trim().isEmpty()) {
                // Include only specified attributes
                Set<String> includeSet = parseAttributeList(attributes);
                groupJson = includeOnlyAttributes(groupJson, includeSet);
            } else if (excludedAttributes != null && !excludedAttributes.trim().isEmpty()) {
                // Exclude specified attributes
                Set<String> excludeSet = parseAttributeList(excludedAttributes);
                groupJson = excludeAttributes(groupJson, excludeSet);
            }
            
            // Convert back to GroupResource
            return objectMapper.treeToValue(groupJson, GroupResource.class);
            
        } catch (Exception e) {
            // If there's an error, return the original group
            return group;
        }
    }
    
    /**
     * Parse a comma-separated attribute list
     */
    private Set<String> parseAttributeList(String attributeList) {
        if (attributeList == null || attributeList.trim().isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<String> attributes = new HashSet<>();
        for (String attr : attributeList.split(",")) {
            String trimmed = attr.trim();
            if (!trimmed.isEmpty()) {
                attributes.add(trimmed.toLowerCase());
            }
        }
        return attributes;
    }
    
    /**
     * Include only specified attributes in the JSON node
     */
    private JsonNode includeOnlyAttributes(JsonNode node, Set<String> includeAttributes) {
        if (!node.isObject()) {
            return node;
        }
        
        ObjectNode result = objectMapper.createObjectNode();
        
        // Always include core attributes
        Set<String> coreAttributes = Set.of("id", "schemas", "meta");
        
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey().toLowerCase();
            
            // Include if it's a core attribute or explicitly requested
            if (coreAttributes.contains(fieldName) || includeAttributes.contains(fieldName)) {
                result.set(field.getKey(), field.getValue());
            }
            
            // Handle sub-attributes (e.g., name.givenName)
            for (String includeAttr : includeAttributes) {
                if (includeAttr.startsWith(fieldName + ".")) {
                    result.set(field.getKey(), field.getValue());
                    break;
                }
            }
        }
        
        return result;
    }
    
    /**
     * Exclude specified attributes from the JSON node
     */
    private JsonNode excludeAttributes(JsonNode node, Set<String> excludeAttributes) {
        if (!node.isObject()) {
            return node;
        }
        
        ObjectNode result = (ObjectNode) node.deepCopy();
        
        // Remove excluded attributes
        for (String excludeAttr : excludeAttributes) {
            // Handle simple attributes
            Iterator<String> fieldNames = result.fieldNames();
            List<String> fieldsToRemove = new ArrayList<>();
            
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (fieldName.toLowerCase().equals(excludeAttr) ||
                    excludeAttr.startsWith(fieldName.toLowerCase() + ".")) {
                    fieldsToRemove.add(fieldName);
                }
            }
            
            for (String field : fieldsToRemove) {
                result.remove(field);
            }
        }
        
        return result;
    }
}