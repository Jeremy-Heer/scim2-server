package com.scim2.server.scim2_server.service.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.scim2.common.filters.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts SCIM filter expressions to LDAP filter expressions.
 * 
 * Supports:
 * - SCIM comparison operators: eq, ne, co, sw, ew, pr, gt, ge, lt, le
 * - SCIM logical operators: and, or, not
 * - Simple attribute paths: userName, name.familyName, etc.
 * - Complex attribute paths: emails.value, emails[type eq "work"].value
 * - JSON object filters for complex multi-valued attributes (PingDirectory)
 * 
 * JSON Object Filter Matching (UnboundID/PingDirectory):
 * For attributes stored as JSON (scimEmails, scimPhoneNumbers, etc.), uses
 * JSON Object Filter Matching Rule (jsonObjectFilterExtensibleMatch).
 * 
 * Example:
 * SCIM: emails[type eq "work" and primary eq true].value eq "user@example.com"
 * LDAP: (scimEmails:jsonObjectFilterExtensibleMatch:={"filterType":"and","andFilters":[{"filterType":"equals","field":"type","value":"work"},{"filterType":"equals","field":"primary","value":"true"}]})
 * 
 * For simple cases, maps to standard LDAP attributes where possible.
 */
@Component
public class ScimFilterToLdapConverter {
    
    private static final Logger logger = LoggerFactory.getLogger(ScimFilterToLdapConverter.class);
    private final LdapConnectionService connectionService;
    
    public ScimFilterToLdapConverter(LdapConnectionService connectionService) {
        this.connectionService = connectionService;
    }
    
    /**
     * Convert SCIM filter string to LDAP Filter.
     * 
     * @param scimFilter SCIM filter expression (e.g., "userName eq \"john\"")
     * @return LDAP Filter object or null if filter is empty
     * @throws IllegalArgumentException if filter is invalid
     */
    public Filter convertFilter(String scimFilter) {
        if (scimFilter == null || scimFilter.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Parse SCIM filter using UnboundID SCIM2 SDK
            com.unboundid.scim2.common.filters.Filter parsedFilter = 
                com.unboundid.scim2.common.filters.Filter.fromString(scimFilter);
            
            return convertScimFilterToLdap(parsedFilter);
        } catch (Exception e) {
            logger.error("Failed to convert SCIM filter to LDAP: {}", scimFilter, e);
            throw new IllegalArgumentException("Invalid SCIM filter: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert parsed SCIM Filter to LDAP Filter recursively.
     */
    private Filter convertScimFilterToLdap(com.unboundid.scim2.common.filters.Filter scimFilter) {
        if (scimFilter instanceof AndFilter) {
            return convertAndFilter((AndFilter) scimFilter);
        } else if (scimFilter instanceof OrFilter) {
            return convertOrFilter((OrFilter) scimFilter);
        } else if (scimFilter instanceof NotFilter) {
            return convertNotFilter((NotFilter) scimFilter);
        } else if (scimFilter instanceof EqualFilter) {
            return convertEqualFilter((EqualFilter) scimFilter);
        } else if (scimFilter instanceof NotEqualFilter) {
            return convertNotEqualFilter((NotEqualFilter) scimFilter);
        } else if (scimFilter instanceof ContainsFilter) {
            return convertContainsFilter((ContainsFilter) scimFilter);
        } else if (scimFilter instanceof StartsWithFilter) {
            return convertStartsWithFilter((StartsWithFilter) scimFilter);
        } else if (scimFilter instanceof EndsWithFilter) {
            return convertEndsWithFilter((EndsWithFilter) scimFilter);
        } else if (scimFilter instanceof PresentFilter) {
            return convertPresentFilter((PresentFilter) scimFilter);
        } else if (scimFilter instanceof GreaterThanFilter) {
            return convertGreaterThanFilter((GreaterThanFilter) scimFilter);
        } else if (scimFilter instanceof GreaterThanOrEqualFilter) {
            return convertGreaterThanOrEqualFilter((GreaterThanOrEqualFilter) scimFilter);
        } else if (scimFilter instanceof LessThanFilter) {
            return convertLessThanFilter((LessThanFilter) scimFilter);
        } else if (scimFilter instanceof LessThanOrEqualFilter) {
            return convertLessThanOrEqualFilter((LessThanOrEqualFilter) scimFilter);
        } else {
            logger.warn("Unsupported SCIM filter type: {}", scimFilter.getClass().getName());
            // Return a filter that matches nothing
            return Filter.createANDFilter();
        }
    }
    
    /**
     * Extract the raw value from a filter's comparison value without quotes.
     * The ComparisonValue.toString() includes quotes for string values,
     * but we need the raw value for LDAP filters.
     */
    private String extractValue(Object comparisonValue) {
        if (comparisonValue == null) {
            return null;
        }
        // Use reflection-like approach to get the JsonNode value
        try {
            // ComparisonValue has getValue() method that returns JsonNode
            java.lang.reflect.Method getValueMethod = comparisonValue.getClass().getMethod("getValue");
            Object jsonNode = getValueMethod.invoke(comparisonValue);
            
            // JsonNode has asText() method
            java.lang.reflect.Method asTextMethod = jsonNode.getClass().getMethod("asText");
            String textValue = (String) asTextMethod.invoke(jsonNode);
            
            return textValue;
        } catch (Exception e) {
            logger.warn("Failed to extract value, falling back to toString(): {}", e.getMessage());
            // Fallback to toString and try to strip quotes
            String strValue = comparisonValue.toString();
            // Remove surrounding quotes if present
            if (strValue.startsWith("\"") && strValue.endsWith("\"") && strValue.length() > 1) {
                return strValue.substring(1, strValue.length() - 1);
            }
            return strValue;
        }
    }
    
    // ========== Logical Operators ==========
    
    private Filter convertAndFilter(AndFilter scimFilter) {
        List<Filter> ldapFilters = new ArrayList<>();
        for (com.unboundid.scim2.common.filters.Filter subFilter : scimFilter.getCombinedFilters()) {
            ldapFilters.add(convertScimFilterToLdap(subFilter));
        }
        return Filter.createANDFilter(ldapFilters);
    }
    
    private Filter convertOrFilter(OrFilter scimFilter) {
        List<Filter> ldapFilters = new ArrayList<>();
        for (com.unboundid.scim2.common.filters.Filter subFilter : scimFilter.getCombinedFilters()) {
            ldapFilters.add(convertScimFilterToLdap(subFilter));
        }
        return Filter.createORFilter(ldapFilters);
    }
    
    private Filter convertNotFilter(NotFilter scimFilter) {
        Filter innerFilter = convertScimFilterToLdap(scimFilter.getInvertedFilter());
        return Filter.createNOTFilter(innerFilter);
    }
    
    // ========== Comparison Operators ==========
    
    private Filter convertEqualFilter(EqualFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        
        // Special handling for member.value: convert SCIM ID (UUID) to DN
        if ("member".equals(mapping.ldapAttribute) && value != null) {
            // Value is a SCIM ID (entryUUID), convert to DN
            String memberDn = connectionService.buildDnFromUuid(
                value,
                connectionService.getLdapProperties().getUserBaseDn()
            );
            if (memberDn != null) {
                mapping.ldapValue = memberDn;
                logger.debug("Converted member UUID {} to DN: {}", value, memberDn);
            } else {
                logger.warn("Could not resolve member UUID to DN: {}", value);
                // Keep original value as fallback
            }
        }
        
        return Filter.createEqualityFilter(mapping.ldapAttribute, mapping.ldapValue);
    }
    
    private Filter convertNotEqualFilter(NotEqualFilter scimFilter) {
        // Convert the not-equal filter by creating an equality filter and negating it
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        Filter equalFilter = Filter.createEqualityFilter(mapping.ldapAttribute, mapping.ldapValue);
        return Filter.createNOTFilter(equalFilter);
    }
    
    private Filter convertContainsFilter(ContainsFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        
        // For JSON attributes, use JSON contains matching if supported
        if (mapping.isJsonAttribute) {
            return createJsonContainsFilter(mapping.ldapAttribute, mapping.jsonField, value);
        }
        
        // Standard substring match: *value*
        return Filter.createSubstringFilter(mapping.ldapAttribute, null, 
            new String[]{mapping.ldapValue}, null);
    }
    
    private Filter convertStartsWithFilter(StartsWithFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        
        // Substring match: value*
        return Filter.createSubstringFilter(mapping.ldapAttribute, mapping.ldapValue, 
            null, null);
    }
    
    private Filter convertEndsWithFilter(EndsWithFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        
        // Substring match: *value
        return Filter.createSubstringFilter(mapping.ldapAttribute, null, 
            null, mapping.ldapValue);
    }
    
    private Filter convertPresentFilter(PresentFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, null);
        return Filter.createPresenceFilter(mapping.ldapAttribute);
    }
    
    private Filter convertGreaterThanFilter(GreaterThanFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        return Filter.createGreaterOrEqualFilter(mapping.ldapAttribute, mapping.ldapValue);
    }
    
    private Filter convertGreaterThanOrEqualFilter(GreaterThanOrEqualFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        return Filter.createGreaterOrEqualFilter(mapping.ldapAttribute, mapping.ldapValue);
    }
    
    private Filter convertLessThanFilter(LessThanFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        return Filter.createLessOrEqualFilter(mapping.ldapAttribute, mapping.ldapValue);
    }
    
    private Filter convertLessThanOrEqualFilter(LessThanOrEqualFilter scimFilter) {
        String attributePath = scimFilter.getAttributePath().toString();
        String value = extractValue(scimFilter.getComparisonValue());
        
        AttributeMapping mapping = mapScimAttributeToLdap(attributePath, value);
        return Filter.createLessOrEqualFilter(mapping.ldapAttribute, mapping.ldapValue);
    }
    
    // ========== Attribute Mapping ==========
    
    /**
     * Map SCIM attribute path to LDAP attribute name.
     * Handles simple paths (userName), complex paths (name.familyName),
     * and array element paths (emails.value, emails[type eq "work"].value).
     */
    private AttributeMapping mapScimAttributeToLdap(String scimAttributePath, String value) {
        AttributeMapping mapping = new AttributeMapping();
        mapping.scimAttribute = scimAttributePath;
        mapping.ldapValue = value;
        
        // Normalize to lowercase for comparison
        String lowerPath = scimAttributePath.toLowerCase();
        
        // Simple attribute mappings
        if (lowerPath.equals("username")) {
            mapping.ldapAttribute = "uid";
        } else if (lowerPath.equals("displayname")) {
            mapping.ldapAttribute = "cn";
        } else if (lowerPath.equals("name.familyname")) {
            mapping.ldapAttribute = "sn";
        } else if (lowerPath.equals("name.givenname")) {
            mapping.ldapAttribute = "givenName";
        } else if (lowerPath.equals("name.middlename")) {
            mapping.ldapAttribute = "scimMiddleName";
        } else if (lowerPath.equals("name.honorificprefix")) {
            mapping.ldapAttribute = "personalTitle";
        } else if (lowerPath.equals("name.honorificsuffix")) {
            mapping.ldapAttribute = "scimHonorificSuffix";
        } else if (lowerPath.equals("name.formatted")) {
            mapping.ldapAttribute = "cn";
        } else if (lowerPath.equals("nickname")) {
            mapping.ldapAttribute = "scimNickName";
        } else if (lowerPath.equals("profileurl")) {
            mapping.ldapAttribute = "scimProfileUrl";
        } else if (lowerPath.equals("title")) {
            mapping.ldapAttribute = "title";
        } else if (lowerPath.equals("usertype")) {
            mapping.ldapAttribute = "employeeType";
        } else if (lowerPath.equals("preferredlanguage")) {
            mapping.ldapAttribute = "preferredLanguage";
        } else if (lowerPath.equals("locale")) {
            mapping.ldapAttribute = "scimLocale";
        } else if (lowerPath.equals("timezone")) {
            mapping.ldapAttribute = "scimTimezone";
        } else if (lowerPath.equals("active")) {
            mapping.ldapAttribute = "scimActive";
            if (value != null) {
                // Convert boolean to string
                mapping.ldapValue = String.valueOf(value);
            }
        } else if (lowerPath.equals("externalid")) {
            mapping.ldapAttribute = "scimExternalId";
        }
        
        // Email attribute mappings
        else if (lowerPath.startsWith("emails")) {
            if (lowerPath.equals("emails.value") || lowerPath.equals("emails[].value")) {
                // Simple case: any email value
                mapping.ldapAttribute = "mail";
            } else if (lowerPath.contains("[type eq")) {
                // Complex case: emails[type eq "work"].value
                // Use JSON object filter for scimEmails
                mapping.ldapAttribute = "scimEmails";
                mapping.isJsonAttribute = true;
                mapping.jsonField = "value";
                // Extract type filter if present
                if (lowerPath.contains("work")) {
                    mapping.jsonTypeFilter = "work";
                }
            } else {
                // Fallback to primary email
                mapping.ldapAttribute = "mail";
            }
        }
        
        // Phone number mappings
        else if (lowerPath.startsWith("phonenumbers")) {
            if (lowerPath.contains("[type eq \"work\"]")) {
                mapping.ldapAttribute = "telephoneNumber";
            } else if (lowerPath.contains("[type eq \"mobile\"]")) {
                mapping.ldapAttribute = "mobile";
            } else if (lowerPath.contains("[type eq \"home\"]")) {
                mapping.ldapAttribute = "homePhone";
            } else {
                // Use JSON attribute for complex queries
                mapping.ldapAttribute = "scimPhoneNumbers";
                mapping.isJsonAttribute = true;
                mapping.jsonField = "value";
            }
        }
        
        // Address mappings
        else if (lowerPath.startsWith("addresses")) {
            if (lowerPath.contains("streetaddress")) {
                mapping.ldapAttribute = "street";
            } else if (lowerPath.contains("locality")) {
                mapping.ldapAttribute = "l";
            } else if (lowerPath.contains("region")) {
                mapping.ldapAttribute = "st";
            } else if (lowerPath.contains("postalcode")) {
                mapping.ldapAttribute = "postalCode";
            } else if (lowerPath.contains("country")) {
                mapping.ldapAttribute = "c";
            } else {
                mapping.ldapAttribute = "scimAddresses";
                mapping.isJsonAttribute = true;
            }
        }
        
        // Enterprise User Extension
        else if (lowerPath.contains("enterprise")) {
            if (lowerPath.contains("employeenumber")) {
                mapping.ldapAttribute = "employeeNumber";
            } else if (lowerPath.contains("costcenter")) {
                mapping.ldapAttribute = "scimCostCenter";
            } else if (lowerPath.contains("organization")) {
                mapping.ldapAttribute = "o";
            } else if (lowerPath.contains("division")) {
                mapping.ldapAttribute = "scimDivision";
            } else if (lowerPath.contains("department")) {
                mapping.ldapAttribute = "departmentNumber";
            } else if (lowerPath.contains("manager.value")) {
                mapping.ldapAttribute = "manager";
            }
        }
        
        // Group attributes
        else if (lowerPath.equals("members.value")) {
            mapping.ldapAttribute = "member";
        }
        
        // Default: use SCIM attribute name as-is
        else {
            mapping.ldapAttribute = scimAttributePath;
            logger.warn("No explicit mapping for SCIM attribute: {}, using as-is", scimAttributePath);
        }
        
        return mapping;
    }
    
    /**
     * Create a JSON object filter for PingDirectory JSON attributes.
     * This uses the extensible match filter with JSON object matching rule.
     * 
     * Format: (attribute:jsonObjectFilterExtensibleMatch:={"filterType":"equals","field":"fieldName","value":"fieldValue"})
     * 
     * @param ldapAttribute LDAP attribute name (e.g., "scimEmails")
     * @param jsonField JSON field name (e.g., "value")
     * @param value Field value
     * @return LDAP Filter using JSON object filter matching
     */
    private Filter createJsonContainsFilter(String ldapAttribute, String jsonField, String value) {
        // Build JSON filter object
        // {"filterType":"contains","field":"value","value":"search-string"}
        String jsonFilter = String.format(
            "{\"filterType\":\"contains\",\"field\":\"%s\",\"value\":\"%s\"}",
            jsonField, escapeJsonValue(value)
        );
        
        // Create extensible match filter
        // Format: (attribute:matchingRuleOID:=value)
        String filterString = String.format(
            "(%s:jsonObjectFilterExtensibleMatch:=%s)",
            ldapAttribute, jsonFilter
        );
        
        try {
            return Filter.create(filterString);
        } catch (Exception e) {
            logger.error("Failed to create JSON object filter: {}", filterString, e);
            // Fallback to simple equality filter
            return Filter.createEqualityFilter(ldapAttribute, value);
        }
    }
    
    /**
     * Create a JSON equals filter for exact matching.
     */
    private Filter createJsonEqualsFilter(String ldapAttribute, String jsonField, String value) {
        String jsonFilter = String.format(
            "{\"filterType\":\"equals\",\"field\":\"%s\",\"value\":\"%s\"}",
            jsonField, escapeJsonValue(value)
        );
        
        String filterString = String.format(
            "(%s:jsonObjectFilterExtensibleMatch:=%s)",
            ldapAttribute, jsonFilter
        );
        
        try {
            return Filter.create(filterString);
        } catch (Exception e) {
            logger.error("Failed to create JSON equals filter: {}", filterString, e);
            return Filter.createEqualityFilter(ldapAttribute, value);
        }
    }
    
    /**
     * Escape special characters in JSON string values.
     */
    private String escapeJsonValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
    
    /**
     * Helper class for attribute mapping results.
     */
    private static class AttributeMapping {
        String scimAttribute;
        String ldapAttribute;
        String ldapValue;
        boolean isJsonAttribute = false;
        String jsonField;
        String jsonTypeFilter;
    }
}
