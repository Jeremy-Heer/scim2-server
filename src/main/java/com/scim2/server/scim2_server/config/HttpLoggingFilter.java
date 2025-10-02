package com.scim2.server.scim2_server.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;

/**
 * HTTP Request/Response Logging Filter
 * Logs detailed information about HTTP requests and responses including:
 * - Request method, URI, headers, and body
 * - Response status, headers, body, and processing time
 */
@Component
public class HttpLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(HttpLoggingFilter.class);
    
    private static final String REQUEST_PREFIX = "REQUEST  >>> ";
    private static final String RESPONSE_PREFIX = "RESPONSE <<< ";
    private static final String SEPARATOR = "─".repeat(80);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // Wrap request and response to cache content
            ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(httpRequest);
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
            
            long startTime = System.currentTimeMillis();
            
            // Log the incoming request
            logRequest(requestWrapper);
            
            try {
                // Process the request
                chain.doFilter(requestWrapper, responseWrapper);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                
                // Log the request body after processing (when it's available in the cache)
                logRequestBody(requestWrapper);
                
                // Log the outgoing response
                logResponse(responseWrapper, duration);
                
                // Important: Copy cached body content to actual response
                responseWrapper.copyBodyToResponse();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        StringBuilder requestLog = new StringBuilder();
        
        requestLog.append("\n").append(SEPARATOR).append("\n");
        requestLog.append(REQUEST_PREFIX).append("HTTP Request\n");
        requestLog.append(REQUEST_PREFIX).append("Method: ").append(request.getMethod()).append("\n");
        requestLog.append(REQUEST_PREFIX).append("URI: ").append(request.getRequestURI());
        
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestLog.append("?").append(queryString);
        }
        requestLog.append("\n");
        
        requestLog.append(REQUEST_PREFIX).append("Remote Address: ").append(request.getRemoteAddr()).append("\n");
        requestLog.append(REQUEST_PREFIX).append("Content Type: ").append(request.getContentType()).append("\n");
        requestLog.append(REQUEST_PREFIX).append("Content Length: ").append(request.getContentLength()).append("\n");
        
        // Log headers
        requestLog.append(REQUEST_PREFIX).append("Headers:\n");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            requestLog.append(REQUEST_PREFIX).append("  ").append(headerName).append(": ").append(headerValue).append("\n");
        }
        
        logger.info(requestLog.toString());
    }

    private void logRequestBody(ContentCachingRequestWrapper request) {
        // Log request body after the request has been processed
        String requestBody = getRequestBody(request);
        if (requestBody != null && !requestBody.isEmpty()) {
            StringBuilder requestBodyLog = new StringBuilder();
            requestBodyLog.append(REQUEST_PREFIX).append("Body:\n");
            requestBodyLog.append(REQUEST_PREFIX).append(requestBody).append("\n");
            logger.info(requestBodyLog.toString());
        }
    }

    private void logResponse(ContentCachingResponseWrapper response, long duration) {
        StringBuilder responseLog = new StringBuilder();
        
        responseLog.append(RESPONSE_PREFIX).append("HTTP Response\n");
        responseLog.append(RESPONSE_PREFIX).append("Status: ").append(response.getStatus()).append("\n");
        responseLog.append(RESPONSE_PREFIX).append("Processing Time: ").append(duration).append(" ms\n");
        responseLog.append(RESPONSE_PREFIX).append("Content Type: ").append(response.getContentType()).append("\n");
        
        // Log response headers
        responseLog.append(RESPONSE_PREFIX).append("Headers:\n");
        Collection<String> headerNames = response.getHeaderNames();
        for (String headerName : headerNames) {
            String headerValue = response.getHeader(headerName);
            responseLog.append(RESPONSE_PREFIX).append("  ").append(headerName).append(": ").append(headerValue).append("\n");
        }
        
        // Log response body
        String responseBody = getResponseBody(response);
        if (responseBody != null && !responseBody.isEmpty()) {
            responseLog.append(RESPONSE_PREFIX).append("Body:\n");
            responseLog.append(RESPONSE_PREFIX).append(responseBody).append("\n");
        }
        
        responseLog.append(SEPARATOR).append("\n");
        
        logger.info(responseLog.toString());
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] contentAsByteArray = request.getContentAsByteArray();
        if (contentAsByteArray.length > 0) {
            try {
                String encoding = request.getCharacterEncoding() != null ? 
                    request.getCharacterEncoding() : "UTF-8";
                return new String(contentAsByteArray, encoding);
            } catch (UnsupportedEncodingException e) {
                logger.warn("Failed to decode request body", e);
                return "[Unable to decode request body]";
            }
        }
        return "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] contentAsByteArray = response.getContentAsByteArray();
        if (contentAsByteArray.length > 0) {
            try {
                String encoding = response.getCharacterEncoding() != null ? 
                    response.getCharacterEncoding() : "UTF-8";
                String responseBody = new String(contentAsByteArray, encoding);
                
                return responseBody;
            } catch (UnsupportedEncodingException e) {
                logger.warn("Failed to decode response body", e);
                return "[Unable to decode response body]";
            }
        }
        return "";
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("HTTP Logging Filter initialized");
    }

    @Override
    public void destroy() {
        logger.info("HTTP Logging Filter destroyed");
    }
}