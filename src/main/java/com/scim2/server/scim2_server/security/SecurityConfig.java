package com.scim2.server.scim2_server.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final ScimAuthenticationFilter scimAuthenticationFilter;
    
    public SecurityConfig(ScimAuthenticationFilter scimAuthenticationFilter) {
        this.scimAuthenticationFilter = scimAuthenticationFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/scim/v2/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(scimAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }
}