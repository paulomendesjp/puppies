package com.puppies.api.command.service;

import com.puppies.api.config.FileStorageConfig;
import com.puppies.api.exception.FileStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Service for handling local file storage operations.
 * This replaces AWS S3 for local development and testing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageConfig fileStorageConfig;

    /**
     * Upload a file and return its public URL.
     */
    public String uploadFile(MultipartFile file) {
        validateFile(file);
        
        try {
            // Create upload directory if it doesn't exist
            Path uploadDir = Paths.get(fileStorageConfig.getUploadDir());
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

            // Copy file to upload directory
            Path targetLocation = uploadDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("File uploaded successfully: {}", uniqueFilename);

            // Return public URL
            return fileStorageConfig.getBaseUrl() + "/" + uniqueFilename;

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            throw new FileStorageException("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Validate uploaded file.
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        // Check file size
        if (file.getSize() > fileStorageConfig.getMaxFileSize()) {
            throw new FileStorageException("File size exceeds maximum allowed size");
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new FileStorageException("Invalid filename");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!Arrays.asList(fileStorageConfig.getAllowedExtensions()).contains(extension)) {
            throw new FileStorageException("File type not allowed. Allowed types: " + 
                Arrays.toString(fileStorageConfig.getAllowedExtensions()));
        }
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new FileStorageException("Invalid file extension");
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Get the physical path for a filename.
     * Used for serving files.
     */
    public Path getFilePath(String filename) {
        return Paths.get(fileStorageConfig.getUploadDir()).resolve(filename);
    }
}