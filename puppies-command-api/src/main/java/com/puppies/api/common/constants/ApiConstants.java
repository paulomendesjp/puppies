package com.puppies.api.common.constants;

/**
 * API constants for the Puppies Command API.
 * Centralized location for all magic numbers and strings.
 */
public final class ApiConstants {

    private ApiConstants() {
        // Utility class - prevent instantiation
    }

    // HTTP Status Messages
    public static final class ErrorMessages {
        public static final String INVALID_CREDENTIALS = "Invalid credentials";
        public static final String RESOURCE_NOT_FOUND = "Resource not found";
        public static final String FILE_TOO_LARGE = "File size exceeds the maximum allowed size";
        public static final String ALREADY_LIKED = "Post already liked by user";
        public static final String LIKE_NOT_FOUND = "Like not found for post";
        public static final String USER_NOT_FOUND = "User not found";
        public static final String POST_NOT_FOUND = "Post not found";
        public static final String UNEXPECTED_ERROR = "An unexpected error occurred";
        public static final String VALIDATION_FAILED = "Request validation failed";
        public static final String DUPLICATE_EMAIL = "User with email already exists";
        public static final String FILE_UPLOAD_FAILED = "Failed to upload file";
        public static final String MAX_POSTS_EXCEEDED = "Maximum 10 posts allowed";
    }

    // Error Codes
    public static final class ErrorCodes {
        public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
        public static final String FILE_UPLOAD_FAILED = "FILE_UPLOAD_FAILED";
        public static final String ALREADY_LIKED = "ALREADY_LIKED";
        public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
        public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
        public static final String DUPLICATE_EMAIL = "DUPLICATE_EMAIL";
        public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
        public static final String ILLEGAL_STATE = "ILLEGAL_STATE";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    }

    // Business Rules
    public static final class BusinessRules {
        public static final int MAX_DEMO_POSTS = 10;
        public static final int MAX_TEXT_CONTENT_LENGTH = 2000;
        public static final int MAX_IMAGE_URL_LENGTH = 500;
        public static final long DEMO_POST_DELAY_MS = 500L;
    }

    // API URLs
    public static final class ApiUrls {
        public static final String DEFAULT_QUERY_API_BASE_URL = "http://localhost:8082";
        public static final String SYNC_WORKER_HEALTH_URL = "http://localhost:8083/actuator/health";
        public static final String DOG_CEO_API_RANDOM = "https://dog.ceo/api/breeds/image/random";
        public static final String RANDOM_DOG_API = "https://random.dog/woof.json";
    }

    // Cache Names
    public static final class CacheNames {
        public static final String FEED = "feed";
        public static final String POSTS = "posts";
    }

    // CORS Origins
    public static final class CorsOrigins {
        public static final String LOCALHOST_3000 = "http://localhost:3000";
        public static final String LOCALHOST_8000 = "http://localhost:8000";
    }

    // File Types
    public static final class FileTypes {
        public static final String IMAGE_JPEG = "image/jpeg";
        public static final String IMAGE_PNG = "image/png";
        public static final String IMAGE_GIF = "image/gif";
        public static final String IMAGE_WEBP = "image/webp";
    }

    // Demo Messages
    public static final class DemoMessages {
        public static final String[] DOG_MESSAGES = {
            "Look at this adorable puppy! 🐶❤️",
            "Just adopted this beautiful dog! 🏠🐕",
            "Morning walk with my furry friend 🌅🚶‍♀️🐕",
            "Puppy eyes that melt your heart 👀💕",
            "Training session complete! Such a good boy! 🎾🏆",
            "Lazy Sunday with my doggo 😴🐶",
            "New toy, who dis? 🧸🐕",
            "Beach day with the best companion! 🏖️🐕‍🦺",
            "Guess who learned a new trick today? 🎪🐶",
            "Dogs make everything better! ✨🐕"
        };

        public static final String[] DOG_BREEDS = {
            "golden-retriever", "labrador", "husky", "bulldog", "poodle",
            "german-shepherd", "beagle", "rottweiler", "yorkie", "chihuahua"
        };
    }

    // Ports
    public static final class Ports {
        public static final int COMMAND_API_PORT = 8081;
        public static final int QUERY_API_PORT = 8082;
        public static final int SYNC_WORKER_PORT = 8083;
    }
}