package com.puppies.api.command.controller;

import com.puppies.api.command.dto.CreateUserRequest;
import com.puppies.api.command.dto.CreateUserResponse;
import com.puppies.api.command.service.UserCommandService;
import com.puppies.api.exception.DuplicateEmailException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user command operations.
 * Handles user registration and other write operations.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8000"})
public class UserCommandController {

    private final UserCommandService userCommandService;

    /**
     * Register a new user.
     * 
     * POST /api/users
     * 
     * @param request User registration request
     * @return Created user information (without password)
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            CreateUserResponse response = userCommandService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DuplicateEmailException e) {
            log.warn("Duplicate email registration attempt: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage(), "DUPLICATE_EMAIL"));
        }
    }

    /**
     * Simple error response class for user creation failures.
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