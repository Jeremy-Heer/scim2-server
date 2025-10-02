package com.scim2.server.scim2_server.service.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.unboundid.scim2.common.types.*;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.filters.Filter;
import com.unboundid.scim2.common.utils.FilterEvaluator;
import com.unboundid.scim2.common.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the UnboundID SCIM2 SDK Filter and FilterEvaluator functionality.
 * This test class validates that the SDK correctly handles various SCIM filter expressions.
 */
class SdkFilterEvaluatorTest {

    @Test
    void testSimpleEqualityFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName eq \"bjensen\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testEqualityFilterNoMatch() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName eq \"jsmith\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertFalse(result);
    }

    @Test
    void testContainsFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName co \"jensen\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testStartsWithFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName sw \"bje\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testEndsWithFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName ew \"sen\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testNotEqualFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName ne \"jsmith\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testPresentFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName pr");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testNotPresentFilter() throws ScimException {
        UserResource user = new UserResource();
        // Don't set userName
        
        Filter filter = Filter.fromString("userName pr");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertFalse(result);
    }

    @Test
    void testAndLogicalOperator() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        user.setActive(true);
        
        Filter filter = Filter.fromString("userName eq \"bjensen\" and active eq true");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testOrLogicalOperator() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("userName eq \"bjensen\" or userName eq \"jsmith\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testNotLogicalOperator() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        Filter filter = Filter.fromString("not (userName eq \"jsmith\")");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testComplexAttributeFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        Name name = new Name();
        name.setGivenName("Barbara");
        name.setFamilyName("Jensen");
        user.setName(name);
        
        Filter filter = Filter.fromString("name.givenName eq \"Barbara\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testGroupFilter() throws ScimException {
        GroupResource group = new GroupResource();
        group.setDisplayName("Administrators");
        
        Filter filter = Filter.fromString("displayName eq \"Administrators\"");
        JsonNode groupNode = JsonUtils.valueToNode(group);
        
        boolean result = FilterEvaluator.evaluate(filter, groupNode);
        assertTrue(result);
    }

    @Test
    void testValuePathFilter() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        Email email = new Email();
        email.setValue("bjensen@example.com");
        email.setType("work");
        email.setPrimary(true);
        user.setEmails(java.util.List.of(email));
        
        Filter filter = Filter.fromString("emails[type eq \"work\"]");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result = FilterEvaluator.evaluate(filter, userNode);
        assertTrue(result);
    }

    @Test
    void testCaseInsensitiveComparison() throws ScimException {
        UserResource user = new UserResource();
        user.setUserName("bjensen");
        
        // Test uppercase filter value against lowercase attribute value
        Filter filter1 = Filter.fromString("userName eq \"BJENSEN\"");
        JsonNode userNode = JsonUtils.valueToNode(user);
        
        boolean result1 = FilterEvaluator.evaluate(filter1, userNode);
        assertTrue(result1, "SDK should handle case-insensitive string comparisons");
        
        // Test lowercase filter value against lowercase attribute value
        Filter filter2 = Filter.fromString("userName eq \"bjensen\"");
        boolean result2 = FilterEvaluator.evaluate(filter2, userNode);
        assertTrue(result2, "Exact case match should also work");
    }

    @Test
    void testInvalidFilterExpression() {
        assertThrows(ScimException.class, () -> {
            Filter.fromString("invalid filter expression");
        });
    }
}