package com.puppies.api.command.service;

import com.puppies.api.command.dto.CreateUserRequest;
import com.puppies.api.command.dto.CreateUserResponse;
import com.puppies.api.data.entity.User;
import com.puppies.api.data.repository.UserRepository;
import com.puppies.api.exception.DuplicateEmailException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user-related command operations.
 * Implements the Command side of CQRS for user management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user (registration).
     * 
     * @param request The user creation request
     * @return Response with created user information
     * @throws DuplicateEmailException if email already exists
     */
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("User with email " + request.getEmail() + " already exists");
        }

        // Hash the password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Create new user entity
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(hashedPassword)
                .build();

        // Save user to database
        User savedUser = userRepository.save(user);

        log.info("User created successfully with ID: {}", savedUser.getId());

        // Return response DTO (never expose password)
        return CreateUserResponse.from(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }
}