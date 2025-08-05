package com.puppies.api.command.service;

import com.puppies.api.common.constants.ApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service responsible for downloading images from external URLs 
 * and converting them to MultipartFile for internal processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageDownloadService {

    private final RestTemplate restTemplate;

    /**
     * Downloads an image from the given URL and converts it to a MultipartFile.
     * 
     * @param imageUrl The URL of the image to download
     * @return MultipartFile representation of the downloaded image
     * @throws IOException if download fails or image is invalid
     */
    public MultipartFile downloadImageAsMultipartFile(String imageUrl) throws IOException {
        try {
            log.debug("📥 Downloading image from: {}", imageUrl);
            
            // Download image bytes
            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);
            
            if (imageBytes == null || imageBytes.length == 0) {
                throw new IOException("Downloaded image is empty");
            }
            
            // Determine file extension from URL
            String filename = extractFilename(imageUrl);
            String contentType = determineContentType(filename);
            
            log.debug("📸 Downloaded image: {} bytes, type: {}, filename: {}", 
                     imageBytes.length, contentType, filename);
            
            // Create MultipartFile from bytes
            return new DownloadedMultipartFile(imageBytes, filename, contentType);
            
        } catch (Exception e) {
            log.error("Failed to download image from: {}", imageUrl, e);
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract filename from URL.
     */
    private String extractFilename(String imageUrl) {
        try {
            String path = new java.net.URL(imageUrl).getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            
            // If no extension, add .jpg as default
            if (!filename.contains(".")) {
                filename += ".jpg";
            }
            
            // If filename is empty or just extension, generate one
            if (filename.startsWith(".") || filename.length() < 3) {
                filename = "dog_" + System.currentTimeMillis() + ".jpg";
            }
            
            return filename;
        } catch (Exception e) {
            return "dog_" + System.currentTimeMillis() + ".jpg";
        }
    }
    
    /**
     * Determine content type from filename.
     */
    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return ApiConstants.FileTypes.IMAGE_PNG;
        if (lower.endsWith(".gif")) return ApiConstants.FileTypes.IMAGE_GIF;
        if (lower.endsWith(".webp")) return ApiConstants.FileTypes.IMAGE_WEBP;
        return ApiConstants.FileTypes.IMAGE_JPEG; // Default to JPEG
    }
    
    /**
     * Custom MultipartFile implementation for downloaded images.
     */
    private static class DownloadedMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        private final String contentType;
        
        public DownloadedMultipartFile(byte[] content, String filename, String contentType) {
            this.content = content;
            this.filename = filename;
            this.contentType = contentType;
        }
        
        @Override
        public String getName() {
            return "image";
        }
        
        @Override
        public String getOriginalFilename() {
            return filename;
        }
        
        @Override
        public String getContentType() {
            return contentType;
        }
        
        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content.length;
        }
        
        @Override
        public byte[] getBytes() {
            return content;
        }
        
        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(java.io.File dest) throws IOException {
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}