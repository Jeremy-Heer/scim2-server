package com.scim2.server.scim2_server.service;

import com.unboundid.scim2.common.types.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for SdkFilterService that uses UnboundID SCIM2 SDK filtering
 */
public class SdkFilterServiceTest {
    
    private SdkFilterService filterService;
    private List<UserResource> testUsers;
    private List<GroupResource> testGroups;
    
    @BeforeEach
    void setUp() {
        filterService = new SdkFilterService();
        
        // Create test users
        testUsers = new ArrayList<>();
        
        UserResource user1 = new UserResource();
        user1.setId("1");
        user1.setUserName("bjensen");
        user1.setDisplayName("Barbara Jensen");
        user1.setActive(true);
        
        Name name1 = new Name();
        name1.setFamilyName("Jensen");
        name1.setGivenName("Barbara");
        user1.setName(name1);
        
        List<Email> emails1 = new ArrayList<>();
        Email workEmail1 = new Email();
        workEmail1.setValue("bjensen@example.com");
        workEmail1.setType("work");
        workEmail1.setPrimary(true);
        emails1.add(workEmail1);
        user1.setEmails(emails1);
        
        UserResource user2 = new UserResource();
        user2.setId("2");
        user2.setUserName("jsmith");
        user2.setDisplayName("John Smith");
        user2.setActive(false);
        
        Name name2 = new Name();
        name2.setFamilyName("Smith");
        name2.setGivenName("John");
        user2.setName(name2);
        
        List<Email> emails2 = new ArrayList<>();
        Email workEmail2 = new Email();
        workEmail2.setValue("jsmith@example.com");
        workEmail2.setType("work");
        workEmail2.setPrimary(true);
        emails2.add(workEmail2);
        user2.setEmails(emails2);
        
        testUsers.add(user1);
        testUsers.add(user2);
        
        // Create test groups
        testGroups = new ArrayList<>();
        
        GroupResource group1 = new GroupResource();
        group1.setId("1");
        group1.setDisplayName("Employees");
        
        GroupResource group2 = new GroupResource();
        group2.setId("2");
        group2.setDisplayName("Managers");
        
        testGroups.add(group1);
        testGroups.add(group2);
    }
    
    @Test
    void testFilterUsersWithNullOrEmptyFilter() {
        // Test null filter
        List<UserResource> result = filterService.filterUsers(testUsers, null);
        assertEquals(2, result.size());
        
        // Test empty filter
        result = filterService.filterUsers(testUsers, "");
        assertEquals(2, result.size());
        
        // Test whitespace filter
        result = filterService.filterUsers(testUsers, "   ");
        assertEquals(2, result.size());
    }
    
    @Test
    void testFilterUsersWithEqualityFilter() {
        List<UserResource> result = filterService.filterUsers(testUsers, "userName eq \"bjensen\"");
        assertEquals(1, result.size());
        assertEquals("bjensen", result.get(0).getUserName());
        
        result = filterService.filterUsers(testUsers, "userName eq \"nonexistent\"");
        assertEquals(0, result.size());
    }
    
    @Test
    void testFilterUsersWithPresentFilter() {
        List<UserResource> result = filterService.filterUsers(testUsers, "userName pr");
        assertEquals(2, result.size());
        
        result = filterService.filterUsers(testUsers, "title pr");
        assertEquals(0, result.size());
    }
    
    @Test
    void testFilterUsersWithContainsFilter() {
        List<UserResource> result = filterService.filterUsers(testUsers, "displayName co \"Barbara\"");
        assertEquals(1, result.size());
        assertEquals("Barbara Jensen", result.get(0).getDisplayName());
        
        result = filterService.filterUsers(testUsers, "displayName co \"Smith\"");
        assertEquals(1, result.size());
        assertEquals("John Smith", result.get(0).getDisplayName());
    }
    
    @Test
    void testFilterUsersWithLogicalFilters() {
        // AND filter
        List<UserResource> result = filterService.filterUsers(testUsers, "userName eq \"bjensen\" and active eq true");
        assertEquals(1, result.size());
        assertEquals("bjensen", result.get(0).getUserName());
        
        // OR filter
        result = filterService.filterUsers(testUsers, "userName eq \"bjensen\" or userName eq \"jsmith\"");
        assertEquals(2, result.size());
        
        // NOT filter
        result = filterService.filterUsers(testUsers, "not (userName eq \"bjensen\")");
        assertEquals(1, result.size());
        assertEquals("jsmith", result.get(0).getUserName());
    }
    
    @Test
    void testFilterUsersWithComplexAttributeFilter() {
        List<UserResource> result = filterService.filterUsers(testUsers, "name.familyName eq \"Jensen\"");
        assertEquals(1, result.size());
        assertEquals("bjensen", result.get(0).getUserName());
        
        result = filterService.filterUsers(testUsers, "name.familyName eq \"Smith\"");
        assertEquals(1, result.size());
        assertEquals("jsmith", result.get(0).getUserName());
    }
    
    @Test
    void testFilterUsersWithValuePathFilter() {
        List<UserResource> result = filterService.filterUsers(testUsers, "emails[type eq \"work\"]");
        assertEquals(2, result.size()); // Both users have work emails
        
        result = filterService.filterUsers(testUsers, "emails[type eq \"personal\"]");
        assertEquals(0, result.size()); // No users have personal emails
    }
    
    @Test
    void testFilterGroupsWithNullOrEmptyFilter() {
        // Test null filter
        List<GroupResource> result = filterService.filterGroups(testGroups, null);
        assertEquals(2, result.size());
        
        // Test empty filter
        result = filterService.filterGroups(testGroups, "");
        assertEquals(2, result.size());
    }
    
    @Test
    void testFilterGroupsWithEqualityFilter() {
        List<GroupResource> result = filterService.filterGroups(testGroups, "displayName eq \"Employees\"");
        assertEquals(1, result.size());
        assertEquals("Employees", result.get(0).getDisplayName());
        
        result = filterService.filterGroups(testGroups, "displayName eq \"Nonexistent\"");
        assertEquals(0, result.size());
    }
    
    @Test
    void testFilterGroupsWithContainsFilter() {
        List<GroupResource> result = filterService.filterGroups(testGroups, "displayName co \"Employee\"");
        assertEquals(1, result.size());
        assertEquals("Employees", result.get(0).getDisplayName());
        
        result = filterService.filterGroups(testGroups, "displayName co \"Manager\"");
        assertEquals(1, result.size());
        assertEquals("Managers", result.get(0).getDisplayName());
    }
    
    @Test
    void testFilterWithInvalidExpression() {
        // Test that invalid filter expressions are handled gracefully
        assertThrows(IllegalArgumentException.class, () -> {
            filterService.filterUsers(testUsers, "invalid filter syntax");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            filterService.filterGroups(testGroups, "invalid filter syntax");
        });
    }
    
    @Test
    void testFilterWithComplexExpression() {
        // Test a complex filter expression
        List<UserResource> result = filterService.filterUsers(testUsers, 
            "(userName eq \"bjensen\" or userName eq \"jsmith\") and name.familyName pr");
        assertEquals(2, result.size());
        
        result = filterService.filterUsers(testUsers, 
            "userName eq \"bjensen\" and active eq true and emails[type eq \"work\"]");
        assertEquals(1, result.size());
        assertEquals("bjensen", result.get(0).getUserName());
    }
}