package com.puppies.api.read.controller;

import com.puppies.api.read.model.ReadPost;
import com.puppies.api.read.model.ReadUserProfile;
import com.puppies.api.read.service.QueryPostService;
import com.puppies.api.read.service.QueryUserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for post queries - read-only operations
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "📝 Posts", description = "Post query and search operations")
public class QueryPostController {

    private final QueryPostService queryPostService;
    private final QueryUserProfileService queryUserProfileService;

    /**
     * Get all posts with pagination
     */
    @Operation(summary = "📖 All Posts", 
               description = "Get all posts ordered by creation date (newest first) with caching")
    @GetMapping
    public ResponseEntity<Page<ReadPost>> getAllPosts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("📖 Getting all posts - page: {}, size: {}", page, size);
        Page<ReadPost> posts = queryPostService.getAllPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get trending posts
     */
    @Operation(summary = "🔥 Trending Posts", 
               description = "Get trending posts ordered by popularity score with Redis caching")
    @GetMapping("/trending")
    public ResponseEntity<Page<ReadPost>> getTrendingPosts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("🔥 Getting trending posts - page: {}, size: {}", page, size);
        Page<ReadPost> posts = queryPostService.getTrendingPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get most liked posts
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<ReadPost>> getPopularPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("❤️ Getting popular posts - page: {}, size: {}", page, size);
        Page<ReadPost> posts = queryPostService.getPopularPosts(page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get post by ID
     */
    @Operation(summary = "📝 Post Details", 
               description = "Get single post details by ID with caching")
    @GetMapping("/{id}")
    public ResponseEntity<ReadPost> getPostById(
            @Parameter(description = "Post ID") @PathVariable Long id) {
        log.debug("📝 Getting post by id: {}", id);
        return queryPostService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search posts
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ReadPost>> searchPosts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("🔍 Searching posts for: {}", q);
        Page<ReadPost> posts = queryPostService.searchPosts(q, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get posts by author (using author ID in URL)
     */
    @Operation(summary = "👤 Posts by Author ID", 
               description = "Get posts by specific author using author ID in URL")
    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<ReadPost>> getPostsByAuthor(
            @Parameter(description = "Author ID") @PathVariable Long authorId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("👤 Getting posts by author: {}", authorId);
        Page<ReadPost> posts = queryPostService.getPostsByAuthor(authorId, page, size);
        return ResponseEntity.ok(posts);
    }

    /**
     * Get current user's own posts (using JWT token)
     */
    @Operation(summary = "📝 My Posts", 
               description = "Get posts created by the authenticated user (no need to pass user ID)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my-posts")
    public ResponseEntity<?> getMyPosts(
            Authentication authentication,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Authentication required", 
                               "message", "Please provide a valid JWT token"));
        }
        
        String userEmail = authentication.getName();
        log.debug("📝 Getting posts for authenticated user: {}", userEmail);
        
        try {
            // First, find the user by email to get their ID
            ReadUserProfile userProfile = queryUserProfileService.getUserProfileByEmail(userEmail)
                    .orElse(null);
            
            if (userProfile == null) {
                return ResponseEntity.status(404)
                        .body(Map.of("error", "User not found", 
                                   "message", "User profile not found for email: " + userEmail));
            }
            
            // Get posts by the user's ID
            Page<ReadPost> posts = queryPostService.getPostsByAuthor(userProfile.getId(), page, size);
            
            log.info("📝 Retrieved {} posts for user {} (ID: {})", 
                    posts.getNumberOfElements(), userEmail, userProfile.getId());
            
            return ResponseEntity.ok(Map.of(
                "user", Map.of(
                    "id", userProfile.getId(),
                    "name", userProfile.getName(),
                    "email", userProfile.getEmail(),
                    "postsCount", userProfile.getPostsCount()
                ),
                "posts", posts
            ));
            
        } catch (Exception e) {
            log.error("Error getting posts for user {}", userEmail, e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal server error", 
                               "message", "Failed to retrieve user posts"));
        }
    }

    /**
     * Get recent trending posts
     */
    @GetMapping("/recent-trending")
    public ResponseEntity<List<ReadPost>> getRecentTrendingPosts(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("📈 Getting recent trending posts, limit: {}", limit);
        List<ReadPost> posts = queryPostService.getRecentTrendingPosts(limit);
        return ResponseEntity.ok(posts);
    }
}