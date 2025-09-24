package com.scim2.server.scim2_server.exception;

import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ErrorResponse;
import com.unboundid.scim2.common.types.Meta;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ScimExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value());
        errorResponse.setDetail(ex.getMessage());
        errorResponse.setScimType("noTarget");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflict(ResourceConflictException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.value());
        errorResponse.setDetail(ex.getMessage());
        errorResponse.setScimType("uniqueness");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value());
        errorResponse.setDetail(ex.getMessage());
        errorResponse.setScimType("invalidValue");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ScimException.class)
    public ResponseEntity<ErrorResponse> handleScimException(ScimException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getScimError().getStatus());
        errorResponse.setDetail(ex.getMessage());
        errorResponse.setScimType(ex.getScimError().getScimType() != null ? ex.getScimError().getScimType() : null);
        return ResponseEntity.status(ex.getScimError().getStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        // Don't handle exceptions for OpenAPI/Swagger endpoints
        String requestUri = ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI();
        if (requestUri.startsWith("/v3/api-docs") || requestUri.startsWith("/swagger-ui")) {
            throw new RuntimeException(ex); // Re-throw to let SpringDoc handle it
        }
        
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.setDetail("Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}