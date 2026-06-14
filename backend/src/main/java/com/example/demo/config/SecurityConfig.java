package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF as we are building a stateless REST API
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/incidents/**").authenticated() // Require JWT authentication for incident
                                                                              // endpoints
                        .anyRequest().permitAll() // Allow other endpoints (like websockets/healthchecks) without token
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {
                        }) // Enable OAuth2 Resource Server support via JWT validation
                );
        return http.build();
    }
}
