package com.puppies.api.command.controller;

import com.puppies.api.command.dto.AuthRequest;
import com.puppies.api.command.dto.AuthResponse;
import com.puppies.api.command.service.AuthService;
import com.puppies.api.common.constants.ApiConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Handles JWT token generation for user login.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {ApiConstants.CorsOrigins.LOCALHOST_3000, ApiConstants.CorsOrigins.LOCALHOST_8000})
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
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }
}