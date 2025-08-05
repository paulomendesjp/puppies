package com.puppies.api.command.service;

import com.puppies.api.command.dto.AuthRequest;
import com.puppies.api.command.dto.AuthResponse;
import com.puppies.api.data.entity.User;
import com.puppies.api.data.repository.UserRepository;
import com.puppies.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service for handling authentication operations.
 * Implements JWT token generation for successful authentication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Authenticate user and generate JWT token.
     * 
     * @param request Authentication request with email and password
     * @return Response with JWT token and user information
     * @throws AuthenticationException if authentication fails
     */
    public AuthResponse authenticate(AuthRequest request) {
        log.info("Attempting authentication for user: {}", request.getEmail());

        try {
            // Authenticate user using Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            log.info("Authentication successful for user: {}", request.getEmail());

            // Extract user details from authentication object
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String authenticatedEmail = userDetails.getUsername();
            
            // Since authentication was successful, user must exist
            // But we still need full User entity for response data
            User user = userRepository.findByEmail(authenticatedEmail)
                    .orElseThrow(() -> new RuntimeException("User not found after successful authentication"));

            // Generate JWT token using authenticated email (more secure)
            String token = jwtService.generateToken(authenticatedEmail);

            // Return response with token and user info
            return AuthResponse.success(token, user.getId(), user.getName(), user.getEmail());

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", request.getEmail());
            throw e;
        }
    }
}
