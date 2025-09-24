package com.scim2.server.scim2_server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.Name;
import com.unboundid.scim2.common.types.Email;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ScimServerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String authToken = "Bearer scim-token-123";
    
    @Test
    void testAuthenticationFailure() throws Exception {
        mockMvc.perform(get("/scim/v2/Users")
                .contentType("application/scim+json"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/scim/v2/Users")
                .header("Authorization", "Bearer wrong-token")
                .contentType("application/scim+json"))
                .andExpect(status().isUnauthorized());
    }
    
    private UserResource createTestUser(String username) {
        UserResource user = new UserResource();
        user.setUserName(username);
        
        Name name = new Name();
        name.setGivenName("Test");
        name.setFamilyName("User");
        name.setFormatted("Test User");
        user.setName(name);
        
        Email email = new Email();
        email.setValue(username + "@example.com");
        email.setType("work");
        email.setPrimary(true);
        user.setEmails(Arrays.asList(email));
        
        user.setActive(true);
        user.setDisplayName("Test User");
        
        return user;
    }
}