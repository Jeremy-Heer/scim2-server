package com.scim2.server.scim2_server.controller;

import com.scim2.server.scim2_server.service.JsonFileService;
import com.scim2.server.scim2_server.security.ScimAuthenticationFilter;
import com.scim2.server.scim2_server.security.SecurityConfig;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.Name;
import com.unboundid.scim2.common.types.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
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
    private JsonFileService jsonFileService;
    
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
        when(jsonFileService.searchUsers(any(), any(), any()))
            .thenReturn(Arrays.asList(testUser));
        when(jsonFileService.getAllUsers())
            .thenReturn(Arrays.asList(testUser));
        
        mockMvc.perform(get("/scim/v2/Users")
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json"))
                .andExpect(status().isOk());
    }
}
