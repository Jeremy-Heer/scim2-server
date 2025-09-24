package com.scim2.server.scim2_server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Web Configuration for HTTP Request/Response Logging
 * Registers the HttpLoggingFilter to capture all HTTP traffic
 */
@Configuration
public class WebConfig {

    @Autowired
    private HttpLoggingFilter httpLoggingFilter;

    /**
     * Register the HTTP logging filter with high priority
     * This ensures it captures all requests and responses
     */
    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilterRegistration() {
        FilterRegistrationBean<HttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(httpLoggingFilter);
        registration.addUrlPatterns("/*"); // Apply to all URLs
        registration.setName("HttpLoggingFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // Execute first
        return registration;
    }
}