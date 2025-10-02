package com.scim2.server.scim2_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.types.GroupResource;
import com.unboundid.scim2.common.types.UserResource;
import com.unboundid.scim2.common.utils.FilterEvaluator;
import com.unboundid.scim2.common.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for applying SCIM filters using the UnboundID SCIM2 SDK
 * Replaces the custom ScimFilterService implementation
 */
@Service
public class SdkFilterService {
    
    /**
     * Filter a list of users based on a SCIM filter expression using SDK filtering
     * @param users The list of users to filter
     * @param filterExpression The SCIM filter expression (can be null)
     * @return Filtered list of users
     */
    public List<UserResource> filterUsers(List<UserResource> users, String filterExpression) {
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return users;
        }
        
        try {
            Filter filter = Filter.fromString(filterExpression);
            return users.stream()
                    .filter(user -> evaluateFilter(filter, user))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid filter expression: " + filterExpression, e);
        }
    }
    
    /**
     * Filter a list of groups based on a SCIM filter expression using SDK filtering
     * @param groups The list of groups to filter
     * @param filterExpression The SCIM filter expression (can be null)
     * @return Filtered list of groups
     */
    public List<GroupResource> filterGroups(List<GroupResource> groups, String filterExpression) {
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return groups;
        }
        
        try {
            Filter filter = Filter.fromString(filterExpression);
            return groups.stream()
                    .filter(group -> evaluateFilter(filter, group))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid filter expression: " + filterExpression, e);
        }
    }
    
    /**
     * Evaluate a filter against a UserResource by converting it to JsonNode
     * @param filter The SCIM filter
     * @param user The user resource
     * @return true if the user matches the filter
     */
    private boolean evaluateFilter(Filter filter, UserResource user) {
        try {
            JsonNode userNode = JsonUtils.valueToNode(user);
            return FilterEvaluator.evaluate(filter, userNode);
        } catch (Exception e) {
            // If evaluation fails, exclude the resource
            return false;
        }
    }
    
    /**
     * Evaluate a filter against a GroupResource by converting it to JsonNode
     * @param filter The SCIM filter
     * @param group The group resource
     * @return true if the group matches the filter
     */
    private boolean evaluateFilter(Filter filter, GroupResource group) {
        try {
            JsonNode groupNode = JsonUtils.valueToNode(group);
            return FilterEvaluator.evaluate(filter, groupNode);
        } catch (Exception e) {
            // If evaluation fails, exclude the resource
            return false;
        }
    }
}