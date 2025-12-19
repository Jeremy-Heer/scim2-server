package com.scim2.server.scim2_server.controller;

import com.scim2.server.scim2_server.repository.ScimRepository;
import com.scim2.server.scim2_server.security.ScimAuthenticationFilter;
import com.scim2.server.scim2_server.security.SecurityConfig;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.Name;
import com.unboundid.scim2.common.types.Email;
import com.unboundid.scim2.common.messages.PatchRequest;
import com.unboundid.scim2.common.messages.PatchOperation;
import com.unboundid.scim2.common.Path;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsersController.class)
@Import({ScimAuthenticationFilter.class, SecurityConfig.class})
public class UsersControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ScimRepository scimRepository;
    
    private UserResource testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new UserResource();
        testUser.setId(UUID.randomUUID().toString());
        testUser.setUserName("testuser");
        
        Name name = new Name();
        name.setGivenName("Test");
        name.setFamilyName("User");
        name.setFormatted("Test User");
        testUser.setName(name);
        
        Email email = new Email();
        email.setValue("test@example.com");
        email.setType("work");
        email.setPrimary(true);
        testUser.setEmails(Arrays.asList(email));
        
        testUser.setActive(true);
    }
    
    @Test
    @WithMockUser
    void testGetUsers_Success() throws Exception {
        when(scimRepository.searchUsers(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Arrays.asList(testUser));
        when(scimRepository.getTotalUsers(anyString()))
            .thenReturn(1);
        
        mockMvc.perform(get("/scim/v2/Users")
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json"))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser
    void testPatchUser_Success() throws Exception {
        String userId = testUser.getId();
        UserResource patchedUser = new UserResource();
        patchedUser.setId(userId);
        patchedUser.setUserName("testuser");
        patchedUser.setDisplayName("Updated Display Name");
        patchedUser.setActive(false);
        
        when(scimRepository.getUserById(userId)).thenReturn(testUser);
        when(scimRepository.patchUser(eq(userId), any(PatchRequest.class))).thenReturn(patchedUser);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "replace",
                        "path": "displayName",
                        "value": "Updated Display Name"
                    },
                    {
                        "op": "replace",
                        "path": "active",
                        "value": false
                    }
                ]
            }
            """;
        
        mockMvc.perform(patch("/scim/v2/Users/" + userId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.displayName").value("Updated Display Name"))
                .andExpect(jsonPath("$.active").value(false));
        
        verify(scimRepository).getUserById(userId);
        verify(scimRepository).patchUser(eq(userId), any(PatchRequest.class));
    }
    
    @Test
    @WithMockUser  
    void testPatchUser_NotFound() throws Exception {
        String userId = "nonexistent-id";
        
        when(scimRepository.getUserById(userId)).thenReturn(null);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "replace",
                        "path": "displayName",
                        "value": "Updated Display Name"
                    }
                ]
            }
            """;
        
        mockMvc.perform(patch("/scim/v2/Users/" + userId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isNotFound());
        
        verify(scimRepository).getUserById(userId);
        verify(scimRepository, never()).patchUser(any(), any());
    }
}
