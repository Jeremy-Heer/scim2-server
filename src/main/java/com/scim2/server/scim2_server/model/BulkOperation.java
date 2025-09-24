package com.scim2.server.scim2_server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BulkOperation {
    
    @JsonProperty("method")
    private String method;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("bulkId")
    private String bulkId;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("location")
    private String location;
    
    @JsonProperty("response")
    private Object response;
    
    @JsonProperty("status")
    private String status;
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getBulkId() {
        return bulkId;
    }
    
    public void setBulkId(String bulkId) {
        this.bulkId = bulkId;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Object getResponse() {
        return response;
    }
    
    public void setResponse(Object response) {
        this.response = response;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}