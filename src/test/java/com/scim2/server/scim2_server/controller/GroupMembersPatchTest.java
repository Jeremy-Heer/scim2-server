package com.scim2.server.scim2_server.controller;

import com.scim2.server.scim2_server.repository.ScimRepository;
import com.scim2.server.scim2_server.security.ScimAuthenticationFilter;
import com.scim2.server.scim2_server.security.SecurityConfig;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.types.Member;
import com.unboundid.scim2.common.messages.PatchRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GroupsController.class)
@Import({ScimAuthenticationFilter.class, SecurityConfig.class})
public class GroupMembersPatchTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ScimRepository scimRepository;
    
    private UserResource testUser1;
    private UserResource testUser2;
    private GroupResource testGroup;
    
    @BeforeEach
    void setUp() {
        // Create test users
        testUser1 = new UserResource();
        testUser1.setId("user-1");
        testUser1.setUserName("user1");
        testUser1.setDisplayName("User One");
        
        testUser2 = new UserResource();
        testUser2.setId("user-2");
        testUser2.setUserName("user2");
        testUser2.setDisplayName("User Two");
        
        // Create test group
        testGroup = new GroupResource();
        testGroup.setId("group-1");
        testGroup.setDisplayName("Test Group");
        testGroup.setMembers(Collections.emptyList());
    }
    
    @Test
    @WithMockUser
    void testPatchGroup_AddSingleMember_Success() throws Exception {
        String groupId = testGroup.getId();
        
        // Create expected group with the added member
        GroupResource updatedGroup = new GroupResource();
        updatedGroup.setId(groupId);
        updatedGroup.setDisplayName("Test Group");
        
        Member member = new Member();
        member.setValue("user-1");
        member.setDisplay("User One");
        updatedGroup.setMembers(Arrays.asList(member));
        
        when(scimRepository.getGroupById(groupId)).thenReturn(testGroup);
        when(scimRepository.patchGroup(eq(groupId), any(PatchRequest.class))).thenReturn(updatedGroup);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "add",
                        "path": "members",
                        "value": [
                            {
                                "value": "user-1",
                                "display": "User One"
                            }
                        ]
                    }
                ]
            }
            """;
        
        mockMvc.perform(patch("/scim/v2/Groups/" + groupId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId))
                .andExpect(jsonPath("$.displayName").value("Test Group"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members[0].value").value("user-1"))
                .andExpect(jsonPath("$.members[0].display").value("User One"));
        
        verify(scimRepository).getGroupById(groupId);
        verify(scimRepository).patchGroup(eq(groupId), any(PatchRequest.class));
    }
    
    @Test
    @WithMockUser
    void testPatchGroup_AddMultipleMembers_Success() throws Exception {
        String groupId = testGroup.getId();
        
        // Create expected group with the added members
        GroupResource updatedGroup = new GroupResource();
        updatedGroup.setId(groupId);
        updatedGroup.setDisplayName("Test Group");
        
        Member member1 = new Member();
        member1.setValue("user-1");
        member1.setDisplay("User One");
        
        Member member2 = new Member();
        member2.setValue("user-2");
        member2.setDisplay("User Two");
        
        updatedGroup.setMembers(Arrays.asList(member1, member2));
        
        when(scimRepository.getGroupById(groupId)).thenReturn(testGroup);
        when(scimRepository.patchGroup(eq(groupId), any(PatchRequest.class))).thenReturn(updatedGroup);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "add",
                        "path": "members",
                        "value": [
                            {
                                "value": "user-1",
                                "display": "User One"
                            },
                            {
                                "value": "user-2",
                                "display": "User Two"
                            }
                        ]
                    }
                ]
            }
            """;
        
        mockMvc.perform(patch("/scim/v2/Groups/" + groupId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members", hasSize(2)))
                .andExpect(jsonPath("$.members[0].value").value("user-1"))
                .andExpect(jsonPath("$.members[1].value").value("user-2"));
        
        verify(scimRepository).getGroupById(groupId);
        verify(scimRepository).patchGroup(eq(groupId), any(PatchRequest.class));
    }
    
    @Test
    @WithMockUser
    void testPatchGroup_AddMembersToExistingGroup_Success() throws Exception {
        String groupId = testGroup.getId();
        
        // Group already has one member
        Member existingMember = new Member();
        existingMember.setValue("existing-user");
        existingMember.setDisplay("Existing User");
        testGroup.setMembers(Arrays.asList(existingMember));
        
        // Create expected group with both existing and new members
        GroupResource updatedGroup = new GroupResource();
        updatedGroup.setId(groupId);
        updatedGroup.setDisplayName("Test Group");
        
        Member newMember = new Member();
        newMember.setValue("user-1");
        newMember.setDisplay("User One");
        
        updatedGroup.setMembers(Arrays.asList(existingMember, newMember));
        
        when(scimRepository.getGroupById(groupId)).thenReturn(testGroup);
        when(scimRepository.patchGroup(eq(groupId), any(PatchRequest.class))).thenReturn(updatedGroup);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "add",
                        "path": "members",
                        "value": [
                            {
                                "value": "user-1",
                                "display": "User One"
                            }
                        ]
                    }
                ]
            }
            """;
        
        mockMvc.perform(patch("/scim/v2/Groups/" + groupId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members", hasSize(2)));
        
        verify(scimRepository).getGroupById(groupId);
        verify(scimRepository).patchGroup(eq(groupId), any(PatchRequest.class));
    }
    
    @Test
    @WithMockUser
    void testPatchGroup_RemoveMemberByFilter_Success() throws Exception {
        String groupId = testGroup.getId();
        String userIdToRemove = "95e96ef6-f4aa-48cd-b33f-697112d390dc";
        
        // Group initially has two members
        Member member1 = new Member();
        member1.setValue(userIdToRemove);
        member1.setDisplay("User To Remove");
        
        Member member2 = new Member();
        member2.setValue("user-2");
        member2.setDisplay("User Two");
        
        testGroup.setMembers(Arrays.asList(member1, member2));
        
        // Create expected group with only one member after removal
        GroupResource updatedGroup = new GroupResource();
        updatedGroup.setId(groupId);
        updatedGroup.setDisplayName("Test Group");
        updatedGroup.setMembers(Arrays.asList(member2));
        
        when(scimRepository.getGroupById(groupId)).thenReturn(testGroup);
        when(scimRepository.patchGroup(eq(groupId), any(PatchRequest.class))).thenReturn(updatedGroup);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "remove",
                        "path": "members[value eq \\"%s\\"]"
                    }
                ]
            }
            """.formatted(userIdToRemove);
        
        mockMvc.perform(patch("/scim/v2/Groups/" + groupId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId))
                .andExpect(jsonPath("$.displayName").value("Test Group"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members", hasSize(1)))
                .andExpect(jsonPath("$.members[0].value").value("user-2"));
        
        verify(scimRepository).getGroupById(groupId);
        verify(scimRepository).patchGroup(eq(groupId), any(PatchRequest.class));
    }
    
    @Test
    @WithMockUser
    void testPatchGroup_RemoveAllMembers_Success() throws Exception {
        String groupId = testGroup.getId();
        
        // Group initially has members
        Member member1 = new Member();
        member1.setValue("user-1");
        member1.setDisplay("User One");
        
        Member member2 = new Member();
        member2.setValue("user-2");
        member2.setDisplay("User Two");
        
        testGroup.setMembers(Arrays.asList(member1, member2));
        
        // Create expected group with no members after removal
        GroupResource updatedGroup = new GroupResource();
        updatedGroup.setId(groupId);
        updatedGroup.setDisplayName("Test Group");
        updatedGroup.setMembers(Collections.emptyList());
        
        when(scimRepository.getGroupById(groupId)).thenReturn(testGroup);
        when(scimRepository.patchGroup(eq(groupId), any(PatchRequest.class))).thenReturn(updatedGroup);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "remove",
                        "path": "members"
                    }
                ]
            }
            """;
        
        mockMvc.perform(patch("/scim/v2/Groups/" + groupId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(groupId))
                .andExpect(jsonPath("$.displayName").value("Test Group"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members", hasSize(0)));
        
        verify(scimRepository).getGroupById(groupId);
        verify(scimRepository).patchGroup(eq(groupId), any(PatchRequest.class));
    }
    
    @Test
    @WithMockUser  
    void testPatchGroup_GroupNotFound() throws Exception {
        String groupId = "nonexistent-group-id";
        
        when(scimRepository.getGroupById(groupId)).thenReturn(null);
        
        String patchBody = """
            {
                "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
                "Operations": [
                    {
                        "op": "add",
                        "path": "members",
                        "value": [
                            {
                                "value": "user-1",
                                "display": "User One"
                            }
                        ]
                    }
                ]
            }
            """;
        
        mockMvc.perform(patch("/scim/v2/Groups/" + groupId)
                .header("Authorization", "Bearer scim-token-123")
                .contentType("application/scim+json")
                .content(patchBody))
                .andExpect(status().isNotFound());
        
        verify(scimRepository).getGroupById(groupId);
        verify(scimRepository, never()).patchGroup(any(), any());
    }
}