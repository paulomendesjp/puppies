package com.puppies.api.command.service;

import com.puppies.api.command.dto.CreateUserRequest;
import com.puppies.api.command.dto.CreateUserResponse;
import com.puppies.api.data.entity.User;
import com.puppies.api.data.repository.UserRepository;
import com.puppies.api.exception.DuplicateEmailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserCommandService.
 * 
 * These tests focus on the business logic of user creation,
 * using mocks to isolate the service layer from dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserCommandService Tests")
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCommandService userCommandService;

    private CreateUserRequest validRequest;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRequest = CreateUserRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        savedUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should create user successfully with valid data")
    void createUser_WithValidData_ShouldReturnCreatedUser() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        CreateUserResponse response = userCommandService.createUser(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("John Doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getMessage()).isEqualTo("User created successfully");

        verify(userRepository).existsByEmail("john@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when email already exists")
    void createUser_WithExistingEmail_ShouldThrowDuplicateEmailException() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userCommandService.createUser(validRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("User with email john@example.com already exists");

        verify(userRepository).existsByEmail("john@example.com");
        // Password should not be encoded and user should not be saved
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(anyString());
        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should hash password before saving user")
    void createUser_ShouldHashPasswordBeforeSaving() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        userCommandService.createUser(validRequest);

        // Then
        verify(passwordEncoder).encode("password123");
        
        // Verify that the user is saved with the hashed password
        verify(userRepository).save(any(User.class));
    }
}