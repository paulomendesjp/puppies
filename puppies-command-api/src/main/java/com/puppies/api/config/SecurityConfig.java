package com.puppies.api.config;

import com.puppies.api.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the Puppies API.
 * 
 * This configuration implements:
 * - JWT-based stateless authentication
 * - CORS configuration for frontend integration
 * - Public endpoints for registration and authentication
 * - Protected endpoints for all other operations
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Configure the security filter chain.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as we're using JWT tokens (stateless)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/**").permitAll()           // Authentication endpoints
                .requestMatchers("/api/users").permitAll()             // User registration (POST only)
                .requestMatchers("/api/files/**").permitAll()          // File serving endpoints
                .requestMatchers("/h2-console/**").permitAll()         // H2 console (for testing)
                .requestMatchers("/actuator/**").permitAll()           // Actuator endpoints
                .requestMatchers("/error").permitAll()                 // Error endpoint
                
                // Demo endpoints - public for easy testing
                .requestMatchers("/api/demo/random-dog-image").permitAll()        // Get dog image
                .requestMatchers("/api/demo/dog-image/**").permitAll()            // Breed-specific images
                .requestMatchers("/api/demo/dog-api/health").permitAll()          // API health check
                .requestMatchers("/api/demo/health").permitAll()                  // System health check
                .requestMatchers("/api/demo/stats").permitAll()                   // Demo stats
                .requestMatchers("/api/demo/trending/**").permitAll()             // Trending data bridge
                
                // Swagger/OpenAPI endpoints
                .requestMatchers("/swagger-ui/**").permitAll()         // Swagger UI
                .requestMatchers("/swagger-ui.html").permitAll()       // Swagger UI HTML
                .requestMatchers("/v3/api-docs/**").permitAll()        // OpenAPI docs
                .requestMatchers("/v3/api-docs.yaml").permitAll()      // OpenAPI YAML
                .requestMatchers("/swagger-resources/**").permitAll()  // Swagger resources
                .requestMatchers("/webjars/**").permitAll()            // Swagger static files
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Configure session management - stateless for JWT
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Set authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Disable frame options for H2 console (only needed in development)
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
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*"
        ));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Allow common headers
        configuration.setAllowedHeaders(List.of(
            "Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Password encoder bean using BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider configuration.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication manager bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}