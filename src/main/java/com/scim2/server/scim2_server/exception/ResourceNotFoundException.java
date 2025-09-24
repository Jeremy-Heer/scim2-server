package com.scim2.server.scim2_server.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resourceType, String id) {
        super(resourceType + " with id " + id + " not found");
    }
}