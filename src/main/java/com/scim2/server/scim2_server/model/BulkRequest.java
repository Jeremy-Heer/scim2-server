package com.scim2.server.scim2_server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BulkRequest {
    
    @JsonProperty("schemas")
    private List<String> schemas = List.of("urn:ietf:params:scim:api:messages:2.0:BulkRequest");
    
    @JsonProperty("failOnErrors")
    private Integer failOnErrors;
    
    @JsonProperty("Operations")
    private List<BulkOperation> operations;
    
    public List<String> getSchemas() {
        return schemas;
    }
    
    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }
    
    public Integer getFailOnErrors() {
        return failOnErrors;
    }
    
    public void setFailOnErrors(Integer failOnErrors) {
        this.failOnErrors = failOnErrors;
    }
    
    public List<BulkOperation> getOperations() {
        return operations;
    }
    
    public void setOperations(List<BulkOperation> operations) {
        this.operations = operations;
    }
}