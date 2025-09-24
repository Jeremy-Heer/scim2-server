package com.scim2.server.scim2_server.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BulkResponse {
    
    @JsonProperty("schemas")
    private List<String> schemas = List.of("urn:ietf:params:scim:api:messages:2.0:BulkResponse");
    
    @JsonProperty("Operations")
    private List<BulkOperation> operations;
    
    public List<String> getSchemas() {
        return schemas;
    }
    
    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }
    
    public List<BulkOperation> getOperations() {
        return operations;
    }
    
    public void setOperations(List<BulkOperation> operations) {
        this.operations = operations;
    }
}