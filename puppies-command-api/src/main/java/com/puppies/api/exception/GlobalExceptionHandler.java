package com.puppies.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Puppies API.
 * Provides consistent error responses across all endpoints.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request validation failed")
                .details(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle resource not found errors.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Resource Not Found")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle duplicate email errors.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        log.warn("Duplicate email attempt: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Duplicate Email")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle file storage errors.
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageError(FileStorageException ex) {
        log.error("File storage error: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("File Storage Error")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle file upload size exceeded errors.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("File Too Large")
                .message("File size exceeds the maximum allowed size")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle authentication errors.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Authentication Failed")
                .message("Invalid credentials")
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle illegal state errors (like trying to like an already liked post).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Operation Not Allowed")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle all other unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .build();

        return ResponseEntity.internalServerError().body(errorResponse);
    }

    /**
     * Standard error response format.
     */
    public static class ErrorResponse {
        public final LocalDateTime timestamp;
        public final int status;
        public final String error;
        public final String message;
        public final Object details;

        private ErrorResponse(LocalDateTime timestamp, int status, String error, String message, Object details) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.details = details;
        }

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        public static class ErrorResponseBuilder {
            private LocalDateTime timestamp;
            private int status;
            private String error;
            private String message;
            private Object details;

            public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public ErrorResponseBuilder status(int status) {
                this.status = status;
                return this;
            }

            public ErrorResponseBuilder error(String error) {
                this.error = error;
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ErrorResponseBuilder details(Object details) {
                this.details = details;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(timestamp, status, error, message, details);
            }
        }
    }
}