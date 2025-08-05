package com.puppies.api.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for user registration responses.
 * Returns safe user information after successful registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserResponse {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    private String message;

    public static CreateUserResponse from(Long id, String name, String email, LocalDateTime createdAt) {
        return CreateUserResponse.builder()
                .id(id)
                .name(name)
                .email(email)
                .createdAt(createdAt)
                .message("User created successfully")
                .build();
    }
}