package com.puppies.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.puppies.api.command.dto.AuthRequest;
import com.puppies.api.command.dto.CreateUserRequest;
import com.puppies.api.data.entity.User;
import com.puppies.api.data.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full integration tests for the authentication flow.
 * 
 * These tests use @SpringBootTest to load the complete application context
 * and test the end-to-end authentication workflow.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Authentication Integration Tests")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete registration and authentication flow")
    void completeAuthenticationFlow_ShouldWork() throws Exception {
        // Step 1: Register a new user
        CreateUserRequest registrationRequest = CreateUserRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.message").value("User created successfully"));

        // Step 2: Authenticate with the registered user
        AuthRequest authRequest = AuthRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.message").value("Authentication successful"));
    }

    @Test
    @DisplayName("Authentication with wrong password should fail")
    void authentication_WithWrongPassword_ShouldFail() throws Exception {
        // Setup: Create a user in the database
        User user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password(passwordEncoder.encode("correctpassword"))
                .build();
        userRepository.save(user);

        // Try to authenticate with wrong password
        AuthRequest authRequest = AuthRequest.builder()
                .email("john@example.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registration with duplicate email should fail")
    void registration_WithDuplicateEmail_ShouldFail() throws Exception {
        // Setup: Create a user in the database
        User existingUser = User.builder()
                .name("Existing User")
                .email("john@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(existingUser);

        // Try to register with the same email
        CreateUserRequest duplicateRequest = CreateUserRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }
}