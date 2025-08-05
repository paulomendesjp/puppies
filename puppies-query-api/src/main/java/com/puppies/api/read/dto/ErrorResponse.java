package com.puppies.api.read.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for API operations.
 * Provides consistent error format across all endpoints.
 */
@Data
@Builder
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String code;
    private final Object details;

    /**
     * Create a simple error response with message and code.
     */
    public static ErrorResponse simple(String message, String code) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .message(message)
                .code(code)
                .build();
    }

    /**
     * Create a detailed error response with HTTP status.
     */
    public static ErrorResponse detailed(int status, String error, String message) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .build();
    }
}