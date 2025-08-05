package com.puppies.api.read.controller;

import com.puppies.api.read.model.ReadFeedItem;
import com.puppies.api.read.service.QueryFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for feed queries - read-only operations
 */
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "📱 Feeds", description = "Feed and timeline operations")
public class QueryFeedController {

    private final QueryFeedService queryFeedService;

    /**
     * Get user's personalized feed
     */
    @Operation(summary = "📱 User Feed", 
               description = "Get user's personalized chronological feed with caching")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<ReadFeedItem>> getUserFeed(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("📱 Getting feed for user: {}", userId);
        Page<ReadFeedItem> feed = queryFeedService.getUserFeed(userId, page, size);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get user's feed by popularity
     */
    @GetMapping("/user/{userId}/popular")
    public ResponseEntity<Page<ReadFeedItem>> getUserFeedByPopularity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.debug("🔥 Getting popular feed for user: {}", userId);
        Page<ReadFeedItem> feed = queryFeedService.getUserFeedByPopularity(userId, page, size);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get global trending feed
     */
    @Operation(summary = "🌍 Trending Feed", 
               description = "Get global trending content feed with Redis caching")
    @GetMapping("/trending")
    public ResponseEntity<Page<ReadFeedItem>> getTrendingFeed(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("🌍 Getting global trending feed");
        Page<ReadFeedItem> feed = queryFeedService.getTrendingFeed(page, size);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get discovery feed
     */
    @GetMapping("/discover")
    public ResponseEntity<List<ReadFeedItem>> getDiscoveryFeed(
            @RequestParam(defaultValue = "1.0") Double minScore,
            @RequestParam(defaultValue = "20") int limit) {
        log.debug("🔭 Getting discovery feed with minScore: {}", minScore);
        List<ReadFeedItem> feed = queryFeedService.getDiscoveryFeed(minScore, limit);
        return ResponseEntity.ok(feed);
    }

    /**
     * Get posts user has liked
     */
    @Operation(summary = "❤️ User Liked Posts", 
               description = "Get all posts that a user has liked with caching")
    @GetMapping("/user/{userId}/liked")
    public ResponseEntity<Page<ReadFeedItem>> getUserLikedPosts(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("❤️ Getting liked posts for user: {}", userId);
        Page<ReadFeedItem> feed = queryFeedService.getUserLikedPosts(userId, page, size);
        return ResponseEntity.ok(feed);
    }
}