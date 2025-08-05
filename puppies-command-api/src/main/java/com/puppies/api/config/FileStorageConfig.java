package com.puppies.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for local file storage.
 * 
 * This configuration replaces AWS S3 with local file storage for
 * development and testing purposes.
 */
@Configuration
@ConfigurationProperties(prefix = "file")
@Data
public class FileStorageConfig {

    /**
     * Directory where uploaded files will be stored.
     * Default: ./uploads
     */
    private String uploadDir = "./uploads";

    /**
     * Base URL for serving files.
     * Default: http://localhost:8080/api/files
     */
    private String baseUrl = "http://localhost:8080/api/files";

    /**
     * Maximum file size allowed (in bytes).
     * Default: 10MB
     */
    private long maxFileSize = 10 * 1024 * 1024; // 10MB

    /**
     * Allowed file extensions for images.
     */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
}