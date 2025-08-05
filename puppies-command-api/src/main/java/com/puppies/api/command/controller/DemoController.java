package com.puppies.api.command.controller;

import com.puppies.api.command.dto.CreatePostRequest;
import com.puppies.api.command.dto.CreatePostResponse;
import com.puppies.api.command.service.PostCommandService;
import com.puppies.api.service.DogImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * REST controller for demo operations with dog images API.
 * Provides endpoints to test and demonstrate the system functionality.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8000"})
@Tag(name = "🐕 Demo & Dog API", description = "Demo endpoints using Dog Images API and trending data")
public class DemoController {

    private final DogImageService dogImageService;
    private final PostCommandService postCommandService;
    private final RestTemplate restTemplate;

    @Value("${app.query-api.base-url:http://localhost:8082}")
    private String queryApiBaseUrl;

    private final Random random = new Random();
    
    // Demo content for random posts
    private static final List<String> DOG_MESSAGES = Arrays.asList(
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
    );

    @Operation(summary = "🎲 Create Random Dog Post", 
               description = "Downloads a random dog image from external APIs, saves it locally, and creates a post like a manual upload",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/create-random-dog-post")
    public ResponseEntity<?> createRandomDogPost(Authentication authentication) {
        try {
            // Get random dog image URL from external API
            String dogImageUrl = dogImageService.getRandomDogImage();
            log.info("🎲 Downloading dog image from: {}", dogImageUrl);
            
            // Download the image and convert to MultipartFile
            MultipartFile imageFile = downloadImageAsMultipartFile(dogImageUrl);
            
            // Get random message
            String randomMessage = DOG_MESSAGES.get(random.nextInt(DOG_MESSAGES.size()));
            
            log.info("📸 Creating post with downloaded image (size: {} bytes)", imageFile.getSize());
            
            CreatePostRequest request = CreatePostRequest.builder()
                    .textContent(randomMessage)
                    .build();

            // Use the normal post creation flow with file upload
            CreatePostResponse response = postCommandService.createPost(
                    request, imageFile, authentication.getName());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Random dog post created successfully! 🎉",
                "post", response,
                "originalImageUrl", dogImageUrl,
                "localImageSaved", true
            ));
            
        } catch (Exception e) {
            log.error("Failed to create random dog post", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create post: " + e.getMessage()));
        }
    }

    @Operation(summary = "🐕 Get Random Dog Image", 
               description = "Get a random dog image URL from external APIs (dog.ceo, random.dog)")
    @GetMapping("/random-dog-image")
    public ResponseEntity<?> getRandomDogImage() {
        try {
            String imageUrl = dogImageService.getRandomDogImage();
            return ResponseEntity.ok(Map.of(
                "imageUrl", imageUrl,
                "message", "Random dog image fetched successfully! 🐕"
            ));
        } catch (Exception e) {
            log.error("Failed to fetch dog image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dog image: " + e.getMessage()));
        }
    }

    @Operation(summary = "🎯 Get Breed-Specific Dog Image", 
               description = "Get a dog image for a specific breed from dog.ceo API")
    @GetMapping("/dog-image/breed/{breed}")
    public ResponseEntity<?> getBreedSpecificImage(
            @Parameter(description = "Dog breed (e.g., golden-retriever, labrador, husky)")
            @PathVariable String breed) {
        try {
            String imageUrl = dogImageService.getBreedSpecificImage(breed);
            return ResponseEntity.ok(Map.of(
                "breed", breed,
                "imageUrl", imageUrl,
                "message", "Breed-specific image fetched successfully! 🐕"
            ));
        } catch (Exception e) {
            log.error("Failed to fetch breed-specific image for: {}", breed, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch image for breed: " + breed));
        }
    }

    @Operation(summary = "🏥 Dog API Health Check", 
               description = "Check the health status of external dog image APIs")
    @GetMapping("/dog-api/health")
    public ResponseEntity<?> getDogApiHealth() {
        try {
            DogImageService.DogApiHealthStatus healthStatus = dogImageService.getHealthStatus();
            return ResponseEntity.ok(Map.of(
                "dogCeoStatus", healthStatus.dogCeoStatus,
                "randomDogStatus", healthStatus.randomDogStatus,
                "fallbackAvailable", healthStatus.fallbackAvailable,
                "message", "Dog API health check completed! 🏥"
            ));
        } catch (Exception e) {
            log.error("Failed to check dog API health", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Health check failed: " + e.getMessage()));
        }
    }

    @Operation(summary = "🔥 Get Trending Posts", 
               description = "Get trending posts from the Query API (bridge endpoint)")
    @GetMapping("/trending/posts")
    public ResponseEntity<?> getTrendingPosts(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {
        try {
            String url = queryApiBaseUrl + "/api/posts/trending?page=" + page + "&size=" + size;
            log.info("🔗 Calling Query API: {}", url);
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(Map.of(
                "message", "Successfully fetched trending posts from Query API! 🔥",
                "queryApiUrl", url,
                "data", response
            ));
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("Query API returned 500 error: {}", e.getResponseBodyAsString(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "error", "Query API internal error (500)",
                        "possibleCauses", Arrays.asList(
                            "Query API database is empty (no posts in read store)",
                            "Query API has configuration issues",
                            "Sync worker hasn't populated read store yet"
                        ),
                        "solutions", Arrays.asList(
                            "1. Create some posts first using: POST /api/demo/create-random-dog-post",
                            "2. Check if Query API is running: curl http://localhost:8082/actuator/health", 
                            "3. Check sync worker logs for events processing"
                        ),
                        "queryApiError", e.getResponseBodyAsString()
                    ));
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to Query API", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "error", "Cannot connect to Query API", 
                        "queryApiUrl", queryApiBaseUrl,
                        "solution", "Make sure Query API is running on port 8082: ./start-query-api.sh"
                    ));
        } catch (Exception e) {
            log.error("Unexpected error calling Query API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    @Operation(summary = "📱 Get Trending Feed", 
               description = "Get trending feed from the Query API (bridge endpoint)")
    @GetMapping("/trending/feed")
    public ResponseEntity<?> getTrendingFeed(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size) {
        try {
            String url = queryApiBaseUrl + "/api/feed/trending?page=" + page + "&size=" + size;
            log.info("🔗 Calling Query API: {}", url);
            Object response = restTemplate.getForObject(url, Object.class);
            return ResponseEntity.ok(Map.of(
                "message", "Successfully fetched trending feed from Query API! 📱",
                "queryApiUrl", url,
                "data", response
            ));
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("Query API returned 500 error: {}", e.getResponseBodyAsString(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "error", "Query API internal error (500)",
                        "possibleCauses", Arrays.asList(
                            "No feed items in read store",
                            "Query API configuration issues"
                        ),
                        "solutions", Arrays.asList(
                            "1. Create posts first: POST /api/demo/create-random-dog-post",
                            "2. Check Query API health: curl http://localhost:8082/actuator/health"
                        ),
                        "queryApiError", e.getResponseBodyAsString()
                    ));
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to Query API", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                        "error", "Cannot connect to Query API",
                        "queryApiUrl", queryApiBaseUrl,
                        "solution", "Start Query API: ./start-query-api.sh"
                    ));
        } catch (Exception e) {
            log.error("Unexpected error calling Query API", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected error: " + e.getMessage()));
        }
    }

    @Operation(summary = "🎪 Create Multiple Random Posts", 
               description = "Create multiple random dog posts for demo purposes",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/create-multiple-posts")
    public ResponseEntity<?> createMultipleRandomPosts(
            @Parameter(description = "Number of posts to create (max 10)")
            @RequestParam(defaultValue = "5") int count,
            Authentication authentication) {
        
        if (count > 10) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximum 10 posts allowed"));
        }
        
        try {
            for (int i = 0; i < count; i++) {
                String dogImageUrl = dogImageService.getRandomDogImage();
                String randomMessage = DOG_MESSAGES.get(random.nextInt(DOG_MESSAGES.size()));
                
                log.info("📥 Creating post #{} - downloading image from: {}", (i + 1), dogImageUrl);
                
                // Download the image and convert to MultipartFile
                MultipartFile imageFile = downloadImageAsMultipartFile(dogImageUrl);
                
                CreatePostRequest request = CreatePostRequest.builder()
                        .textContent(randomMessage + " (Demo Post #" + (i + 1) + ")")
                        .build();

                // Use normal post creation with local file storage
                postCommandService.createPost(request, imageFile, authentication.getName());
                
                log.info("✅ Created post #{} with local image (size: {} bytes)", (i + 1), imageFile.getSize());
                
                // Small delay to avoid overwhelming the APIs
                Thread.sleep(500);
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Successfully created " + count + " random dog posts! 🎉",
                "postsCreated", count
            ));
            
        } catch (Exception e) {
            log.error("Failed to create multiple posts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create posts: " + e.getMessage()));
        }
    }

    @Operation(summary = "📊 Demo Stats", 
               description = "Get demo statistics and system info")
    @GetMapping("/stats")
    public ResponseEntity<?> getDemoStats() {
        try {
            return ResponseEntity.ok(Map.of(
                "availableBreeds", Arrays.asList("golden-retriever", "labrador", "husky", "bulldog", "poodle", 
                                                "german-shepherd", "beagle", "rottweiler", "yorkie", "chihuahua"),
                "dogApiEndpoints", Arrays.asList("https://dog.ceo/api/breeds/image/random", 
                                                "https://random.dog/woof.json"),
                "demoMessages", DOG_MESSAGES.size(),
                "commandApiPort", 8081,
                "queryApiPort", 8082,
                "syncWorkerPort", 8083,
                "message", "Demo system ready! 🚀"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get stats: " + e.getMessage()));
        }
    }

    @Operation(summary = "🩺 System Health Check", 
               description = "Check health of all system components (Command API, Query API, Sync Worker, External APIs)")
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        Map<String, Object> health = new java.util.HashMap<>();
        
        // Check Command API (self)
        health.put("commandApi", Map.of("status", "UP", "port", 8081));
        
        // Check Query API
        try {
            restTemplate.getForObject(queryApiBaseUrl + "/actuator/health", Object.class);
            health.put("queryApi", Map.of("status", "UP", "port", 8082, "url", queryApiBaseUrl));
        } catch (Exception e) {
            health.put("queryApi", Map.of(
                "status", "DOWN", 
                "port", 8082, 
                "url", queryApiBaseUrl,
                "error", e.getMessage(),
                "solution", "Start with: ./start-query-api.sh"
            ));
        }
        
        // Check Sync Worker
        try {
            restTemplate.getForObject("http://localhost:8083/actuator/health", Object.class);
            health.put("syncWorker", Map.of("status", "UP", "port", 8083));
        } catch (Exception e) {
            health.put("syncWorker", Map.of(
                "status", "DOWN", 
                "port", 8083,
                "error", e.getMessage(),
                "solution", "Start with: ./start-sync-worker.sh"
            ));
        }
        
        // Check Dog APIs
        health.put("dogApis", dogImageService.getHealthStatus());
        
        // Overall status
        boolean allUp = true;
        for (Object component : health.values()) {
            if (component instanceof Map && "DOWN".equals(((Map<?, ?>) component).get("status"))) {
                allUp = false;
                break;
            }
        }
        
        health.put("overall", Map.of(
            "status", allUp ? "UP" : "DEGRADED",
            "timestamp", java.time.LocalDateTime.now(),
            "message", allUp ? "All systems operational! 🟢" : "Some components are down 🟡"
        ));
        
        return ResponseEntity.ok(health);
    }

    /**
     * Helper method to download an image from URL and convert to MultipartFile
     */
    private MultipartFile downloadImageAsMultipartFile(String imageUrl) throws IOException {
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
            return new CustomMultipartFile(imageBytes, filename, contentType);
            
        } catch (Exception e) {
            log.error("Failed to download image from: {}", imageUrl, e);
            throw new IOException("Failed to download image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract filename from URL
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
     * Determine content type from filename
     */
    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "image/jpeg"; // Default to JPEG
    }
    
    /**
     * Custom MultipartFile implementation for downloaded images
     */
    private static class CustomMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;
        private final String contentType;
        
        public CustomMultipartFile(byte[] content, String filename, String contentType) {
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