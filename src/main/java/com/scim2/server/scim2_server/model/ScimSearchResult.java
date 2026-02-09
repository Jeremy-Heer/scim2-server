package com.scim2.server.scim2_server.model;

import java.util.List;

/**
 * Wrapper class to hold search results along with total count.
 * Used for efficient pagination with Virtual List View.
 * 
 * @param <T> The resource type (UserResource or GroupResource)
 */
public class ScimSearchResult<T> {
    
    private final List<T> resources;
    private final int totalResults;
    
    public ScimSearchResult(List<T> resources, int totalResults) {
        this.resources = resources;
        this.totalResults = totalResults;
    }
    
    public List<T> getResources() {
        return resources;
    }
    
    public int getTotalResults() {
        return totalResults;
    }
}
