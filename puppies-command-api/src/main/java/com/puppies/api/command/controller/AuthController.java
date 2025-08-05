package com.puppies.api.command.controller;

import com.puppies.api.command.dto.AuthRequest;
import com.puppies.api.command.dto.AuthResponse;
import com.puppies.api.command.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles JWT token generation for user login.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8000"})
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticate user and return JWT token.
     * 
     * POST /api/auth/token
     * POST /api/auth/login (alias)
     * 
     * @param request Authentication request with email and password
     * @return JWT token and user information
     */
    @PostMapping({"/token", "/login"})
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthRequest request) {
        try {
            AuthResponse response = authService.authenticate(request);
            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", request.getEmail());
            return ResponseEntity.status(401)
                    .body(new ErrorResponse("Invalid credentials", "AUTHENTICATION_FAILED"));
        }
    }

    /**
     * Simple error response class for authentication failures.
     */
    public static class ErrorResponse {
        public final String message;
        public final String code;

        public ErrorResponse(String message, String code) {
            this.message = message;
            this.code = code;
        }
    }
}