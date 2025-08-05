package com.puppies.api.command.service;

import com.puppies.api.command.dto.AuthRequest;
import com.puppies.api.command.dto.AuthResponse;
import com.puppies.api.data.entity.User;
import com.puppies.api.data.repository.UserRepository;
import com.puppies.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * 
 * Tests authentication logic, JWT token generation,
 * and error handling scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private AuthRequest validAuthRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validAuthRequest = AuthRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should authenticate successfully with valid credentials")
    void authenticate_WithValidCredentials_ShouldReturnAuthResponse() {
        // Given
        String generatedToken = "jwt-token-123";
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(validAuthRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser.getEmail()))
                .thenReturn(generatedToken);

        // When
        AuthResponse response = authService.authenticate(validAuthRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(generatedToken);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getName()).isEqualTo(testUser.getName());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getMessage()).isEqualTo("Authentication successful");

        // Verify interactions
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(validAuthRequest.getEmail());
        verify(jwtService).generateToken(testUser.getEmail());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException for invalid credentials")
    void authenticate_WithInvalidCredentials_ShouldThrowBadCredentialsException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        assertThatThrownBy(() -> authService.authenticate(validAuthRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        // Verify no token generation or user lookup happened
        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should throw RuntimeException when user not found after successful authentication")
    void authenticate_WithUserNotFoundAfterAuth_ShouldThrowRuntimeException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(validAuthRequest.getEmail()))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.authenticate(validAuthRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found after successful authentication");

        // Verify authentication happened but no token generation
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(validAuthRequest.getEmail());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Should create correct authentication token with email and password")
    void authenticate_ShouldCreateCorrectAuthenticationToken() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(validAuthRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser.getEmail()))
                .thenReturn("token");

        // When
        authService.authenticate(validAuthRequest);

        // Then
        verify(authenticationManager).authenticate(argThat(token -> {
            UsernamePasswordAuthenticationToken authToken = (UsernamePasswordAuthenticationToken) token;
            return authToken.getPrincipal().equals("john@example.com") &&
                   authToken.getCredentials().equals("password123");
        }));
    }

    @Test
    @DisplayName("Should handle null request gracefully")
    void authenticate_WithNullRequest_ShouldThrowNullPointerException() {
        // When/Then
        assertThatThrownBy(() -> authService.authenticate(null))
                .isInstanceOf(NullPointerException.class);

        // Verify no interactions
        verifyNoInteractions(authenticationManager, userRepository, jwtService);
    }

    @Test
    @DisplayName("Should handle empty email gracefully")
    void authenticate_WithEmptyEmail_ShouldAttemptAuthentication() {
        // Given
        AuthRequest requestWithEmptyEmail = AuthRequest.builder()
                .email("")
                .password("password123")
                .build();
        
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        assertThatThrownBy(() -> authService.authenticate(requestWithEmptyEmail))
                .isInstanceOf(BadCredentialsException.class);

        // Verify authentication was attempted
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should handle JWT token generation failure")
    void authenticate_WithJwtGenerationFailure_ShouldPropagateException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(validAuthRequest.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser.getEmail()))
                .thenThrow(new RuntimeException("JWT generation failed"));

        // When/Then
        assertThatThrownBy(() -> authService.authenticate(validAuthRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("JWT generation failed");

        // Verify all steps up to JWT generation were called
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(validAuthRequest.getEmail());
        verify(jwtService).generateToken(testUser.getEmail());
    }
}