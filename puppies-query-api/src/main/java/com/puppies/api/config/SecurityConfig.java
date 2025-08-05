package com.puppies.api.config;

import com.puppies.api.common.constants.QueryApiConstants;
import com.puppies.api.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the Query API.
 * 
 * Provides both authenticated and public endpoints:
 * - Public endpoints for general queries
 * - Authenticated endpoints for user-specific data
 * 
 * This configuration provides:
 * - JWT authentication for user-specific endpoints
 * - CORS for frontend integration
 * - Stateless session management
 * - Mixed security (some endpoints require auth, others are public)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Configure the security filter chain.
     * Query API is read-only and doesn't handle authentication.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as we're stateless
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure authorization - mix of public and authenticated endpoints
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no authentication required)
                .requestMatchers(QueryApiConstants.ApiUrls.PUBLIC_POST_ENDPOINTS).permitAll()
                .requestMatchers(QueryApiConstants.ApiUrls.PUBLIC_FEED_ENDPOINTS).permitAll()
                .requestMatchers(QueryApiConstants.ApiUrls.PUBLIC_USER_ENDPOINTS).permitAll()
                .requestMatchers(QueryApiConstants.ApiUrls.PUBLIC_CACHE_ENDPOINTS).permitAll()
                .requestMatchers(QueryApiConstants.ApiUrls.PUBLIC_SYSTEM_ENDPOINTS).permitAll()
                .requestMatchers(QueryApiConstants.ApiUrls.PUBLIC_SWAGGER_ENDPOINTS).permitAll()
                
                // Authenticated endpoints (require JWT token)
                .requestMatchers(QueryApiConstants.ApiUrls.AUTHENTICATED_ENDPOINTS).authenticated()
                
                .anyRequest().permitAll()                              // Default: permit all
            )
            
            // Configure session management - stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add JWT authentication filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Disable frame options for H2 console (development)
        http.headers(headers -> headers.frameOptions().disable());

        return http.build();
    }

    /**
     * Configure CORS to allow frontend integration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow common frontend origins
        configuration.setAllowedOriginPatterns(List.of(QueryApiConstants.ApiUrls.CORS_ORIGIN_PATTERNS));
        
        // Allow read-only HTTP methods (Query API should be read-only)
        configuration.setAllowedMethods(List.of(
            "GET", "OPTIONS"
        ));
        
        // Allow common headers
        configuration.setAllowedHeaders(List.of(
            "Content-Type", "Accept", "Origin", "X-Requested-With", "Authorization"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}