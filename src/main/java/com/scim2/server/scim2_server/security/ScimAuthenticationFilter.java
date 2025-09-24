package com.scim2.server.scim2_server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ScimAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_TOKEN = "scim-token-123";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Allow Swagger UI and OpenAPI docs without authentication
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check for SCIM endpoints - they all start with /scim/v2
        if (path.startsWith("/scim/v2")) {
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                sendUnauthorized(response, "Missing or invalid Authorization header");
                return;
            }
            
            String token = authHeader.substring(BEARER_PREFIX.length());
            
            if (!BEARER_TOKEN.equals(token)) {
                sendUnauthorized(response, "Invalid token");
                return;
            }
            
            // Set authentication in SecurityContext for valid token
            Authentication auth = new UsernamePasswordAuthenticationToken(
                "scim-user", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/scim+json");
        response.getWriter().write(String.format(
            "{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:Error\"],\"status\":\"401\",\"detail\":\"%s\"}", 
            message));
    }
}