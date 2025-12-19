package com.scim2.server.scim2_server.service.ldap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scim2.server.scim2_server.exception.InvalidRequestException;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim2.common.types.*;
import com.unboundid.scim2.common.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps SCIM2 User and Group resources to/from LDAP entries.
 * 
 * Supports:
 * - Standard inetOrgPerson attributes (uid, cn, sn, mail, etc.)
 * - Custom scim* attributes for non-mappable SCIM fields
 * - JSON-encoded multi-valued complex attributes (scimEmails, scimPhoneNumbers, etc.)
 * - Virtual memberOf attribute from PingDirectory
 * - entryUUID-based DNs for SCIM ID mapping
 * 
 * JSON Attribute Encoding:
 * - emails[] → scimEmails (JSON array)
 * - phoneNumbers[] → scimPhoneNumbers (JSON array)
 * - addresses[] → scimAddresses (JSON array)
 * - ims[] → scimIms (JSON array)
 * - photos[] → scimPhotos (JSON array)
 * - roles[] → scimRoles (JSON array)
 * - entitlements[] → scimEntitlements (JSON array)
 * - x509Certificates[] → scimX509Certificates (JSON array)
 */
@Component
public class ScimLdapAttributeMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(ScimLdapAttributeMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // ========== User Resource Mapping ==========
    
    /**
     * Convert SCIM UserResource to LDAP Entry attributes.
     * Note: This returns the attributes Map, not a complete Entry.
     * The DN must be determined separately using LdapConnectionService.
     * 
     * @param user SCIM UserResource
     * @return Map of LDAP attribute names to Attribute objects
     */
    public Map<String, Attribute> userToLdapAttributes(UserResource user) {
        Map<String, Attribute> attributes = new HashMap<>();
        
        // Object classes
        attributes.put("objectClass", new Attribute("objectClass", 
            "top", "person", "organizationalPerson", "inetOrgPerson", "scimUser"));
        
        // SCIM ID is mapped to entryUUID (operational attribute, not set directly)
        // Note: entryUUID is automatically set by LDAP server
        
        // External ID
        if (user.getExternalId() != null) {
            attributes.put("scimExternalId", new Attribute("scimExternalId", user.getExternalId()));
        }
        
        // Username (required)
        if (user.getUserName() != null) {
            attributes.put("uid", new Attribute("uid", user.getUserName()));
        }
        
        // Display Name → cn (required for person)
        if (user.getDisplayName() != null) {
            attributes.put("cn", new Attribute("cn", user.getDisplayName()));
            attributes.put("displayName", new Attribute("displayName", user.getDisplayName()));
        } else if (user.getName() != null && user.getName().getFormatted() != null) {
            // Fallback to formatted name
            attributes.put("cn", new Attribute("cn", user.getName().getFormatted()));
        } else if (user.getUserName() != null) {
            // Fallback to username
            attributes.put("cn", new Attribute("cn", user.getUserName()));
        }
        
        // Name attributes
        if (user.getName() != null) {
            Name name = user.getName();
            
            if (name.getFamilyName() != null) {
                attributes.put("sn", new Attribute("sn", name.getFamilyName()));
            }
            
            if (name.getGivenName() != null) {
                attributes.put("givenName", new Attribute("givenName", name.getGivenName()));
            }
            
            if (name.getMiddleName() != null) {
                attributes.put("scimMiddleName", new Attribute("scimMiddleName", name.getMiddleName()));
            }
            
            if (name.getHonorificPrefix() != null) {
                attributes.put("personalTitle", new Attribute("personalTitle", name.getHonorificPrefix()));
            }
            
            if (name.getHonorificSuffix() != null) {
                attributes.put("scimHonorificSuffix", new Attribute("scimHonorificSuffix", name.getHonorificSuffix()));
            }
        }
        
        // Ensure sn is present (required for person)
        if (!attributes.containsKey("sn")) {
            if (user.getUserName() != null) {
                attributes.put("sn", new Attribute("sn", user.getUserName()));
            } else {
                attributes.put("sn", new Attribute("sn", "Unknown"));
            }
        }
        
        // Nickname
        if (user.getNickName() != null) {
            attributes.put("scimNickName", new Attribute("scimNickName", user.getNickName()));
        }
        
        // Profile URL
        if (user.getProfileUrl() != null) {
            attributes.put("scimProfileUrl", new Attribute("scimProfileUrl", user.getProfileUrl().toString()));
        }
        
        // Title (job title)
        if (user.getTitle() != null) {
            attributes.put("title", new Attribute("title", user.getTitle()));
        }
        
        // User Type
        if (user.getUserType() != null) {
            attributes.put("employeeType", new Attribute("employeeType", user.getUserType()));
        }
        
        // Preferred Language
        if (user.getPreferredLanguage() != null) {
            attributes.put("preferredLanguage", new Attribute("preferredLanguage", user.getPreferredLanguage()));
        }
        
        // Locale
        if (user.getLocale() != null) {
            attributes.put("scimLocale", new Attribute("scimLocale", user.getLocale()));
        }
        
        // Timezone
        if (user.getTimezone() != null) {
            attributes.put("scimTimezone", new Attribute("scimTimezone", user.getTimezone()));
        }
        
        // Active status
        if (user.getActive() != null) {
            attributes.put("scimActive", new Attribute("scimActive", user.getActive().toString()));
        }
        
        // Password (if present in create/update)
        if (user.getPassword() != null) {
            attributes.put("userPassword", new Attribute("userPassword", user.getPassword()));
        }
        
        // Emails (JSON encoded) - Store primary in mail, all in scimEmails
        if (user.getEmails() != null && !user.getEmails().isEmpty()) {
            // Find primary email for mail attribute
            Email primaryEmail = user.getEmails().stream()
                .filter(Email::getPrimary)
                .findFirst()
                .orElse(user.getEmails().get(0));
            
            if (primaryEmail.getValue() != null) {
                attributes.put("mail", new Attribute("mail", primaryEmail.getValue()));
            }
            
            // Store each email as a separate JSON object (multi-valued attribute)
            try {
                String[] emailJsons = new String[user.getEmails().size()];
                for (int i = 0; i < user.getEmails().size(); i++) {
                    emailJsons[i] = objectMapper.writeValueAsString(user.getEmails().get(i));
                }
                attributes.put("scimEmails", new Attribute("scimEmails", emailJsons));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize emails to JSON", e);
            }
        }
        
        // Phone Numbers (JSON encoded) - Map by type to standard attributes
        if (user.getPhoneNumbers() != null && !user.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : user.getPhoneNumbers()) {
                String type = phone.getType();
                String value = phone.getValue();
                
                if (value != null && type != null) {
                    switch (type.toLowerCase()) {
                        case "work":
                            if (!attributes.containsKey("telephoneNumber")) {
                                attributes.put("telephoneNumber", new Attribute("telephoneNumber", value));
                            }
                            break;
                        case "mobile":
                            if (!attributes.containsKey("mobile")) {
                                attributes.put("mobile", new Attribute("mobile", value));
                            }
                            break;
                        case "home":
                            if (!attributes.containsKey("homePhone")) {
                                attributes.put("homePhone", new Attribute("homePhone", value));
                            }
                            break;
                        case "fax":
                            if (!attributes.containsKey("facsimileTelephoneNumber")) {
                                attributes.put("facsimileTelephoneNumber", 
                                    new Attribute("facsimileTelephoneNumber", value));
                            }
                            break;
                        case "pager":
                            if (!attributes.containsKey("pager")) {
                                attributes.put("pager", new Attribute("pager", value));
                            }
                            break;
                    }
                }
            }
            
            // Store each phone number as a separate JSON object (multi-valued attribute)
            try {
                String[] phoneJsons = new String[user.getPhoneNumbers().size()];
                for (int i = 0; i < user.getPhoneNumbers().size(); i++) {
                    phoneJsons[i] = objectMapper.writeValueAsString(user.getPhoneNumbers().get(i));
                }
                attributes.put("scimPhoneNumbers", new Attribute("scimPhoneNumbers", phoneJsons));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize phone numbers to JSON", e);
            }
        }
        
        // Addresses (JSON encoded)
        if (user.getAddresses() != null && !user.getAddresses().isEmpty()) {
            // Map work address to standard LDAP attributes
            Address workAddress = user.getAddresses().stream()
                .filter(a -> "work".equalsIgnoreCase(a.getType()))
                .findFirst()
                .orElse(user.getAddresses().get(0));
            
            if (workAddress != null) {
                if (workAddress.getStreetAddress() != null) {
                    attributes.put("street", new Attribute("street", workAddress.getStreetAddress()));
                }
                if (workAddress.getLocality() != null) {
                    attributes.put("l", new Attribute("l", workAddress.getLocality()));
                }
                if (workAddress.getRegion() != null) {
                    attributes.put("st", new Attribute("st", workAddress.getRegion()));
                }
                if (workAddress.getPostalCode() != null) {
                    attributes.put("postalCode", new Attribute("postalCode", workAddress.getPostalCode()));
                }
                if (workAddress.getCountry() != null) {
                    attributes.put("c", new Attribute("c", workAddress.getCountry()));
                }
            }
            
            // Store each address as a separate JSON object (multi-valued attribute)
            try {
                String[] addressJsons = new String[user.getAddresses().size()];
                for (int i = 0; i < user.getAddresses().size(); i++) {
                    addressJsons[i] = objectMapper.writeValueAsString(user.getAddresses().get(i));
                }
                attributes.put("scimAddresses", new Attribute("scimAddresses", addressJsons));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize addresses to JSON", e);
            }
        }
        
        // IMs, Photos, Roles, Entitlements, X509Certificates (all JSON encoded)
        encodeComplexMultiValuedAttribute(user.getIms(), "scimIms", attributes);
        encodeComplexMultiValuedAttribute(user.getPhotos(), "scimPhotos", attributes);
        encodeComplexMultiValuedAttribute(user.getRoles(), "scimRoles", attributes);
        encodeComplexMultiValuedAttribute(user.getEntitlements(), "scimEntitlements", attributes);
        encodeComplexMultiValuedAttribute(user.getX509Certificates(), "scimX509Certificates", attributes);
        
        // Enterprise User Extension        // TODO: EnterpriseUser support - uncomment when SCIM SDK supports it
        /*        if (user.getEnterpriseUser() != null) {
            EnterpriseUser enterprise = user.getEnterpriseUser();
            
            if (enterprise.getEmployeeNumber() != null) {
                attributes.put("employeeNumber", new Attribute("employeeNumber", enterprise.getEmployeeNumber()));
            }
            
            if (enterprise.getCostCenter() != null) {
                attributes.put("scimCostCenter", new Attribute("scimCostCenter", enterprise.getCostCenter()));
            }
            
            if (enterprise.getOrganization() != null) {
                attributes.put("o", new Attribute("o", enterprise.getOrganization()));
            }
            
            if (enterprise.getDivision() != null) {
                attributes.put("scimDivision", new Attribute("scimDivision", enterprise.getDivision()));
            }
            
            if (enterprise.getDepartment() != null) {
                attributes.put("departmentNumber", new Attribute("departmentNumber", enterprise.getDepartment()));
            }
            
            // Manager is stored as DN reference - will be set separately
            // because we need to resolve the manager's UUID to DN
        }
        */
        
        // Metadata fields (resource type, version)
        attributes.put("scimResourceType", new Attribute("scimResourceType", "User"));
        
        return attributes;
    }
    
    /**
     * Convert LDAP Entry to SCIM UserResource.
     * 
     * @param entry LDAP Entry
     * @param ldapConnectionService Connection service for DN resolution
     * @return SCIM UserResource
     */
    public UserResource ldapEntryToUser(Entry entry, LdapConnectionService ldapConnectionService) {
        UserResource user = new UserResource();
        
        // SCIM ID directly from entryUUID attribute (operational)
        String entryUuid = entry.getAttributeValue("entryUUID");
        if (entryUuid != null) {
            user.setId(entryUuid);
        } else {
            // Fallback: try to extract UUID from DN if using entryUUID naming
            String uuid = ldapConnectionService.extractUuidFromDn(entry.getDN());
            if (uuid != null) {
                user.setId(uuid);
            } else {
                logger.warn("Unable to determine SCIM ID for entry: {}", entry.getDN());
            }
        }
        
        // External ID
        user.setExternalId(entry.getAttributeValue("scimExternalId"));
        
        // Username
        user.setUserName(entry.getAttributeValue("uid"));
        
        // Display Name
        user.setDisplayName(entry.getAttributeValue("displayName"));
        
        // Name
        Name name = new Name();
        name.setFamilyName(entry.getAttributeValue("sn"));
        name.setGivenName(entry.getAttributeValue("givenName"));
        name.setMiddleName(entry.getAttributeValue("scimMiddleName"));
        name.setHonorificPrefix(entry.getAttributeValue("personalTitle"));
        name.setHonorificSuffix(entry.getAttributeValue("scimHonorificSuffix"));
        
        // Build formatted name
        StringBuilder formatted = new StringBuilder();
        if (name.getHonorificPrefix() != null) formatted.append(name.getHonorificPrefix()).append(" ");
        if (name.getGivenName() != null) formatted.append(name.getGivenName()).append(" ");
        if (name.getMiddleName() != null) formatted.append(name.getMiddleName()).append(" ");
        if (name.getFamilyName() != null) formatted.append(name.getFamilyName());
        if (name.getHonorificSuffix() != null) formatted.append(" ").append(name.getHonorificSuffix());
        name.setFormatted(formatted.toString().trim());
        
        user.setName(name);
        
        // Other simple attributes
        user.setNickName(entry.getAttributeValue("scimNickName"));
        String profileUrl = entry.getAttributeValue("scimProfileUrl");
        if (profileUrl != null) {
            try {
                user.setProfileUrl(new java.net.URI(profileUrl));
            } catch (java.net.URISyntaxException e) {
                logger.warn("Invalid profile URL: {}", profileUrl);
            }
        }
        user.setTitle(entry.getAttributeValue("title"));
        user.setUserType(entry.getAttributeValue("employeeType"));
        user.setPreferredLanguage(entry.getAttributeValue("preferredLanguage"));
        user.setLocale(entry.getAttributeValue("scimLocale"));
        user.setTimezone(entry.getAttributeValue("scimTimezone"));
        
        // Active status
        String activeStr = entry.getAttributeValue("scimActive");
        if (activeStr != null) {
            user.setActive(Boolean.parseBoolean(activeStr));
        }
        
        // Emails from JSON (multi-valued attribute)
        String[] emailJsons = entry.getAttributeValues("scimEmails");
        if (emailJsons != null && emailJsons.length > 0) {
            try {
                List<Email> emails = new ArrayList<>();
                for (String emailJson : emailJsons) {
                    Email email = objectMapper.readValue(emailJson, Email.class);
                    emails.add(email);
                }
                user.setEmails(emails);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize emails from JSON", e);
            }
        } else {
            // Fallback: Create from mail attribute
            String mail = entry.getAttributeValue("mail");
            if (mail != null) {
                Email email = new Email();
                email.setValue(mail);
                email.setType("work");
                email.setPrimary(true);
                user.setEmails(Collections.singletonList(email));
            }
        }
        
        // Phone Numbers from JSON (multi-valued attribute)
        String[] phoneJsons = entry.getAttributeValues("scimPhoneNumbers");
        if (phoneJsons != null && phoneJsons.length > 0) {
            try {
                List<PhoneNumber> phones = new ArrayList<>();
                for (String phoneJson : phoneJsons) {
                    PhoneNumber phone = objectMapper.readValue(phoneJson, PhoneNumber.class);
                    phones.add(phone);
                }
                user.setPhoneNumbers(phones);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize phone numbers from JSON", e);
            }
        }
        
        // Addresses from JSON (multi-valued attribute)
        String[] addressJsons = entry.getAttributeValues("scimAddresses");
        if (addressJsons != null && addressJsons.length > 0) {
            try {
                List<Address> addresses = new ArrayList<>();
                for (String addressJson : addressJsons) {
                    Address address = objectMapper.readValue(addressJson, Address.class);
                    addresses.add(address);
                }
                user.setAddresses(addresses);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize addresses from JSON", e);
            }
        }
        
        // Other complex multi-valued attributes from JSON
        decodeComplexMultiValuedAttribute(entry, "scimIms", InstantMessagingAddress.class, user::setIms);
        decodeComplexMultiValuedAttribute(entry, "scimPhotos", Photo.class, user::setPhotos);
        decodeComplexMultiValuedAttribute(entry, "scimRoles", Role.class, user::setRoles);
        decodeComplexMultiValuedAttribute(entry, "scimEntitlements", Entitlement.class, user::setEntitlements);
        decodeComplexMultiValuedAttribute(entry, "scimX509Certificates", X509Certificate.class, user::setX509Certificates);
        
        // Enterprise User Extension        // TODO: EnterpriseUser support - uncomment when SCIM SDK supports it
        /*        EnterpriseUser enterprise = new EnterpriseUser();
        enterprise.setEmployeeNumber(entry.getAttributeValue("employeeNumber"));
        enterprise.setCostCenter(entry.getAttributeValue("scimCostCenter"));
        enterprise.setOrganization(entry.getAttributeValue("o"));
        enterprise.setDivision(entry.getAttributeValue("scimDivision"));
        enterprise.setDepartment(entry.getAttributeValue("departmentNumber"));
        
        // Manager from DN - TODO: resolve to UUID
        String managerDn = entry.getAttributeValue("manager");
        if (managerDn != null) {
            Manager manager = new Manager();
            // Extract UUID from manager DN if using entryUUID naming
            String managerUuid = ldapConnectionService.extractUuidFromDn(managerDn);
            if (managerUuid != null) {
                manager.setValue(managerUuid);
            }
            enterprise.setManager(manager);
        }
        
        user.setEnterpriseUser(enterprise);
        */
        
        // Groups from virtual memberOf attribute (PingDirectory)
        String[] memberOfDns = entry.getAttributeValues("memberOf");
        if (memberOfDns != null && memberOfDns.length > 0) {
            List<Group> groups = new ArrayList<>();
            for (String groupDn : memberOfDns) {
                Group group = new Group();
                // Extract UUID from group DN
                String groupUuid = ldapConnectionService.extractUuidFromDn(groupDn);
                if (groupUuid != null) {
                    group.setValue(groupUuid);
                }
                groups.add(group);
            }
            user.setGroups(groups);
        }
        
        // Metadata
        Meta meta = new Meta();
        meta.setResourceType("User");
        
        // Created/Modified timestamps from LDAP operational attributes
        String createTimestamp = entry.getAttributeValue("createTimestamp");
        String modifyTimestamp = entry.getAttributeValue("modifyTimestamp");
        // TODO: Parse LDAP timestamps and convert to Calendar
        
        user.setMeta(meta);
        
        return user;
    }
    
    // ========== Group Resource Mapping ==========
    
    /**
     * Convert SCIM GroupResource to LDAP Entry attributes.
     * 
     * @param group SCIM GroupResource
     * @return Map of LDAP attribute names to Attribute objects
     */
    public Map<String, Attribute> groupToLdapAttributes(GroupResource group) {
        Map<String, Attribute> attributes = new HashMap<>();
        
        // Object classes
        attributes.put("objectClass", new Attribute("objectClass",
            "top", "groupOfNames", "scimGroup"));
        
        // SCIM ID is mapped to entryUUID (operational attribute, not set directly)
        // Note: entryUUID is automatically set by LDAP server
        
        // External ID
        if (group.getExternalId() != null) {
            attributes.put("scimExternalId", new Attribute("scimExternalId", group.getExternalId()));
        }
        
        // Display Name → cn (required for groupOfNames)
        if (group.getDisplayName() != null) {
            attributes.put("cn", new Attribute("cn", group.getDisplayName()));
        }
        
        // Members - stored as DNs in member attribute
        // Note: This will be handled separately because we need to resolve UUIDs to DNs
        
        // Metadata
        attributes.put("scimResourceType", new Attribute("scimResourceType", "Group"));
        
        return attributes;
    }
    
    /**
     * Convert LDAP Entry to SCIM GroupResource.
     * 
     * @param entry LDAP Entry
     * @param ldapConnectionService Connection service for DN resolution
     * @return SCIM GroupResource
     */
    public GroupResource ldapEntryToGroup(Entry entry, LdapConnectionService ldapConnectionService) {
        GroupResource group = new GroupResource();
        
        // SCIM ID directly from entryUUID attribute (operational)
        String entryUuid = entry.getAttributeValue("entryUUID");
        if (entryUuid != null) {
            group.setId(entryUuid);
        } else {
            // Fallback: try to extract UUID from DN if using entryUUID naming
            String uuid = ldapConnectionService.extractUuidFromDn(entry.getDN());
            if (uuid != null) {
                group.setId(uuid);
            } else {
                logger.warn("Unable to determine SCIM ID for group entry: {}", entry.getDN());
            }
        }
        
        // External ID
        group.setExternalId(entry.getAttributeValue("scimExternalId"));
        
        // Display Name
        group.setDisplayName(entry.getAttributeValue("cn"));
        
        // Members from member attribute
        String[] memberDns = entry.getAttributeValues("member");
        if (memberDns != null && memberDns.length > 0) {
            List<Member> members = new ArrayList<>();
            for (String memberDn : memberDns) {
                Member member = new Member();
                // Extract UUID from member DN
                String memberUuid = ldapConnectionService.extractUuidFromDn(memberDn);
                if (memberUuid != null) {
                    member.setValue(memberUuid);
                    member.setType("User");
                    members.add(member);
                }
            }
            group.setMembers(members);
        }
        
        // Metadata
        Meta meta = new Meta();
        meta.setResourceType("Group");
        group.setMeta(meta);
        
        return group;
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Encode a complex multi-valued attribute to JSON and add to attributes map.
     */
    private <T> void encodeComplexMultiValuedAttribute(List<T> values, String attributeName,
                                                       Map<String, Attribute> attributes) {
        if (values != null && !values.isEmpty()) {
            try {
                String[] jsonValues = new String[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    jsonValues[i] = objectMapper.writeValueAsString(values.get(i));
                }
                attributes.put(attributeName, new Attribute(attributeName, jsonValues));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize {} to JSON", attributeName, e);
            }
        }
    }
    
    /**
     * Decode a JSON-encoded complex multi-valued attribute from LDAP entry.
     */
    private <T> void decodeComplexMultiValuedAttribute(Entry entry, String attributeName,
                                                       Class<T> elementClass,
                                                       java.util.function.Consumer<List<T>> setter) {
        String[] jsonValues = entry.getAttributeValues(attributeName);
        if (jsonValues != null && jsonValues.length > 0) {
            try {
                List<T> values = new ArrayList<>();
                for (String json : jsonValues) {
                    T value = objectMapper.readValue(json, elementClass);
                    values.add(value);
                }
                setter.accept(values);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize {} from JSON", attributeName, e);
            }
        }
    }
}
