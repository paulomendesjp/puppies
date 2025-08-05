package com.puppies.api.command.controller;

import com.puppies.api.command.dto.CreatePostRequest;
import com.puppies.api.command.dto.CreatePostResponse;
import com.puppies.api.command.service.DemoStatsService;
import com.puppies.api.command.service.ImageDownloadService;
import com.puppies.api.command.service.PostCommandService;
import com.puppies.api.command.service.SystemHealthService;
import com.puppies.api.common.constants.ApiConstants;
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

import java.util.Arrays;
import java.util.Map;

/**
 * REST controller for demo operations with dog images API.
 * Provides endpoints to test and demonstrate the system functionality.
 */
@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {ApiConstants.CorsOrigins.LOCALHOST_3000, ApiConstants.CorsOrigins.LOCALHOST_8000})
@Tag(name = "🐕 Demo & Dog API", description = "Demo endpoints using Dog Images API and trending data")
public class DemoController {

    private final DogImageService dogImageService;
    private final PostCommandService postCommandService;
    private final ImageDownloadService imageDownloadService;
    private final SystemHealthService systemHealthService;
    private final DemoStatsService demoStatsService;
    private final RestTemplate restTemplate;

    @Value("${app.query-api.base-url:" + ApiConstants.ApiUrls.DEFAULT_QUERY_API_BASE_URL + "}")
    private String queryApiBaseUrl;

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
            MultipartFile imageFile = imageDownloadService.downloadImageAsMultipartFile(dogImageUrl);
            
            // Get random message
            String randomMessage = demoStatsService.getRandomDogMessage();
            
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
        
        if (count > ApiConstants.BusinessRules.MAX_DEMO_POSTS) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", ApiConstants.ErrorMessages.MAX_POSTS_EXCEEDED));
        }
        
        try {
            for (int i = 0; i < count; i++) {
                String dogImageUrl = dogImageService.getRandomDogImage();
                String randomMessage = demoStatsService.getRandomDogMessage(i + 1);
                
                log.info("📥 Creating post #{} - downloading image from: {}", (i + 1), dogImageUrl);
                
                // Download the image and convert to MultipartFile
                MultipartFile imageFile = imageDownloadService.downloadImageAsMultipartFile(dogImageUrl);
                
                CreatePostRequest request = CreatePostRequest.builder()
                        .textContent(randomMessage)
                        .build();

                // Use normal post creation with local file storage
                postCommandService.createPost(request, imageFile, authentication.getName());
                
                log.info("✅ Created post #{} with local image (size: {} bytes)", (i + 1), imageFile.getSize());
                
                // Small delay to avoid overwhelming the APIs
                Thread.sleep(ApiConstants.BusinessRules.DEMO_POST_DELAY_MS);
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
            return ResponseEntity.ok(demoStatsService.getDemoStats());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get stats: " + e.getMessage()));
        }
    }

    @Operation(summary = "🩺 System Health Check", 
               description = "Check health of all system components (Command API, Query API, Sync Worker, External APIs)")
    @GetMapping("/health")
    public ResponseEntity<?> getSystemHealth() {
        return ResponseEntity.ok(systemHealthService.getSystemHealth());
    }

}