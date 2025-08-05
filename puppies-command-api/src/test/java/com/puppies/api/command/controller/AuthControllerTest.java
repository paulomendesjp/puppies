package com.puppies.api.command.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puppies.api.command.dto.AuthRequest;
import com.puppies.api.command.dto.AuthResponse;
import com.puppies.api.command.service.AuthService;
import com.puppies.api.security.JwtService;
import com.puppies.api.security.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * 
 * These tests use @WebMvcTest to test the web layer in isolation,
 * with mocked service dependencies.
 */
@WebMvcTest(controllers = AuthController.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    @Primary
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/token - Should return token for valid credentials")
    void authenticate_WithValidCredentials_ShouldReturnToken() throws Exception {
        // Given
        AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.success(
                "jwt-token-123",
                1L,
                "John Doe",
                "john@example.com"
        );

        when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));
    }

    @Test
    @DisplayName("POST /api/auth/token - Should return 401 for invalid credentials")
    void authenticate_WithInvalidCredentials_ShouldReturn401() throws Exception {
        // Given
        AuthRequest request = AuthRequest.builder()
                .email("john@example.com")
                .password("wrongpassword")
                .build();

        when(authService.authenticate(any(AuthRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("POST /api/auth/token - Should return 400 for invalid request format")
    void authenticate_WithInvalidRequestFormat_ShouldReturn400() throws Exception {
        // Given - Invalid request (missing email)
        AuthRequest request = AuthRequest.builder()
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/token - Should return 400 for invalid email format")
    void authenticate_WithInvalidEmailFormat_ShouldReturn400() throws Exception {
        // Given - Invalid email format
        AuthRequest request = AuthRequest.builder()
                .email("invalid-email")
                .password("password123")
                .build();

        // When & Then
        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}