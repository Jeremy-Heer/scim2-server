package com.scim2.server.scim2_server.controller;

import com.scim2.server.scim2_server.exception.InvalidRequestException;
import com.scim2.server.scim2_server.service.JsonFileService;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.Member;
import com.unboundid.scim2.common.types.UserResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupsControllerValidationTest {

    @Mock
    private JsonFileService jsonFileService;

    @InjectMocks
    private GroupsController groupsController;

    private UserResource testUser;
    private GroupResource testGroup;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create a test user
        testUser = new UserResource();
        testUser.setId("test-user-id-123");
        testUser.setUserName("testuser");
        
        // Create a test group
        testGroup = new GroupResource();
        testGroup.setDisplayName("Test Group");
    }

    @Test
    void testValidMemberIsAccepted() {
        // Arrange
        when(jsonFileService.getUserById("test-user-id-123")).thenReturn(testUser);
        
        Member member = new Member();
        member.setValue("test-user-id-123");
        member.setDisplay("Test User");
        testGroup.setMembers(Arrays.asList(member));
        
        when(jsonFileService.saveGroup(any(GroupResource.class))).thenReturn(testGroup);
        
        // Mock the HttpServletRequest properly
        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/scim/v2/Groups"));
        
        // Act & Assert - should not throw any exception
        assertDoesNotThrow(() -> {
            groupsController.createGroup(testGroup, mockRequest);
        });
        
        verify(jsonFileService).getUserById("test-user-id-123");
    }

    @Test
    void testInvalidMemberIdIsRejected() {
        // Arrange
        when(jsonFileService.getUserById("nonexistent-user-id")).thenReturn(null);
        
        Member member = new Member();
        member.setValue("nonexistent-user-id");
        member.setDisplay("Nonexistent User");
        testGroup.setMembers(Arrays.asList(member));
        
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            groupsController.createGroup(testGroup, mock(jakarta.servlet.http.HttpServletRequest.class));
        });
        
        assertEquals("Member with id 'nonexistent-user-id' does not exist in the system", exception.getMessage());
        verify(jsonFileService).getUserById("nonexistent-user-id");
    }

    @Test
    void testEmptyMemberValueIsRejected() {
        // Arrange
        Member member = new Member();
        member.setValue(""); // Empty value
        member.setDisplay("Test User");
        testGroup.setMembers(Arrays.asList(member));
        
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            groupsController.createGroup(testGroup, mock(jakarta.servlet.http.HttpServletRequest.class));
        });
        
        assertEquals("Member 'value' field is required and must not be empty", exception.getMessage());
    }

    @Test
    void testNullMemberValueIsRejected() {
        // Arrange
        Member member = new Member();
        member.setValue(null); // Null value
        member.setDisplay("Test User");
        testGroup.setMembers(Arrays.asList(member));
        
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            groupsController.createGroup(testGroup, mock(jakarta.servlet.http.HttpServletRequest.class));
        });
        
        assertEquals("Member 'value' field is required and must not be empty", exception.getMessage());
    }

    @Test
    void testDuplicateMembersAreRejected() {
        // Arrange
        when(jsonFileService.getUserById("test-user-id-123")).thenReturn(testUser);
        
        Member member1 = new Member();
        member1.setValue("test-user-id-123");
        member1.setDisplay("Test User 1");
        
        Member member2 = new Member();
        member2.setValue("test-user-id-123"); // Same ID as member1
        member2.setDisplay("Test User 2");
        
        testGroup.setMembers(Arrays.asList(member1, member2));
        
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            groupsController.createGroup(testGroup, mock(jakarta.servlet.http.HttpServletRequest.class));
        });
        
        assertEquals("Duplicate member with id 'test-user-id-123' found in group. Each member can only be added once.", exception.getMessage());
    }

    @Test
    void testValidRefIsAccepted() {
        // Arrange
        when(jsonFileService.getUserById("test-user-id-123")).thenReturn(testUser);
        
        Member member = new Member();
        member.setValue("test-user-id-123");
        member.setDisplay("Test User");
        member.setRef(URI.create("https://example.com/scim/v2/Users/test-user-id-123"));
        testGroup.setMembers(Arrays.asList(member));
        
        when(jsonFileService.saveGroup(any(GroupResource.class))).thenReturn(testGroup);
        
        // Mock the HttpServletRequest properly
        jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/scim/v2/Groups"));
        
        // Act & Assert - should not throw any exception
        assertDoesNotThrow(() -> {
            groupsController.createGroup(testGroup, mockRequest);
        });
        
        verify(jsonFileService, times(2)).getUserById("test-user-id-123"); // Once for value, once for $ref
    }

    @Test
    void testMismatchedRefIsRejected() {
        // Arrange
        when(jsonFileService.getUserById("test-user-id-123")).thenReturn(testUser);
        when(jsonFileService.getUserById("different-user-id")).thenReturn(testUser);
        
        Member member = new Member();
        member.setValue("test-user-id-123");
        member.setDisplay("Test User");
        member.setRef(URI.create("https://example.com/scim/v2/Users/different-user-id")); // Different ID in $ref
        testGroup.setMembers(Arrays.asList(member));
        
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            groupsController.createGroup(testGroup, mock(jakarta.servlet.http.HttpServletRequest.class));
        });
        
        assertTrue(exception.getMessage().contains("Member $ref ID 'different-user-id' does not match member value 'test-user-id-123'"));
    }

    @Test
    void testInvalidRefFormatIsRejected() {
        // Arrange
        when(jsonFileService.getUserById("test-user-id-123")).thenReturn(testUser);
        
        Member member = new Member();
        member.setValue("test-user-id-123");
        member.setDisplay("Test User");
        member.setRef(URI.create("https://example.com/scim/v2/Groups/test-user-id-123")); // Wrong resource type
        testGroup.setMembers(Arrays.asList(member));
        
        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            groupsController.createGroup(testGroup, mock(jakarta.servlet.http.HttpServletRequest.class));
        });
        
        assertTrue(exception.getMessage().contains("Member $ref must point to a Users resource"));
    }
}