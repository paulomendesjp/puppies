package com.puppies.api.read.controller;

import com.puppies.api.read.model.ReadUserProfile;
import com.puppies.api.read.service.QueryUserProfileService;
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
 * REST Controller for user profile queries - read-only operations
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "👤 User Profiles", description = "User profile query operations")
public class QueryUserProfileController {

    private final QueryUserProfileService queryUserProfileService;

    /**
     * Get user profile by ID
     */
    @Operation(summary = "👤 Get User Profile", 
               description = "Get detailed user profile by ID including stats (posts, likes, followers)")
    @GetMapping("/{userId}")
    public ResponseEntity<ReadUserProfile> getUserProfile(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        log.debug("👤 Getting user profile: {}", userId);
        return queryUserProfileService.getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user profile by email
     */
    @Operation(summary = "📧 Get User by Email", 
               description = "Find user profile by email address")
    @GetMapping("/email/{email}")
    public ResponseEntity<ReadUserProfile> getUserProfileByEmail(
            @Parameter(description = "User email") @PathVariable String email) {
        log.debug("📧 Getting user profile by email: {}", email);
        return queryUserProfileService.getUserProfileByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get top content creators
     */
    @Operation(summary = "🏆 Top Content Creators", 
               description = "Get users ranked by number of posts created")
    @GetMapping("/top-creators")
    public ResponseEntity<Page<ReadUserProfile>> getTopContentCreators(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("🏆 Getting top content creators - page: {}, size: {}", page, size);
        Page<ReadUserProfile> users = queryUserProfileService.getTopContentCreators(page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Get top liked users
     */
    @Operation(summary = "❤️ Most Liked Users", 
               description = "Get users ranked by total likes received on their posts")
    @GetMapping("/top-liked")
    public ResponseEntity<Page<ReadUserProfile>> getTopLikedUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("❤️ Getting top liked users - page: {}, size: {}", page, size);
        Page<ReadUserProfile> users = queryUserProfileService.getTopLikedUsers(page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Get most active users
     */
    @Operation(summary = "🔥 Most Active Users", 
               description = "Get users ranked by overall activity (posts + likes given + likes received)")
    @GetMapping("/most-active")
    public ResponseEntity<List<ReadUserProfile>> getMostActiveUsers(
            @Parameter(description = "Maximum number of users to return") @RequestParam(defaultValue = "10") int limit) {
        log.debug("🔥 Getting most active users, limit: {}", limit);
        List<ReadUserProfile> users = queryUserProfileService.getMostActiveUsers(limit);
        return ResponseEntity.ok(users);
    }

    /**
     * Search users by name
     */
    @Operation(summary = "🔍 Search Users", 
               description = "Search users by name (case-insensitive partial match)")
    @GetMapping("/search")
    public ResponseEntity<Page<ReadUserProfile>> searchUsers(
            @Parameter(description = "Search term (user name)") @RequestParam String q,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        log.debug("🔍 Searching users for: {}", q);
        Page<ReadUserProfile> users = queryUserProfileService.searchUsers(q, page, size);
        return ResponseEntity.ok(users);
    }

    /**
     * Get prolific users
     */
    @Operation(summary = "📝 Prolific Users", 
               description = "Get users who have created more than a specified number of posts")
    @GetMapping("/prolific")
    public ResponseEntity<List<ReadUserProfile>> getProlificUsers(
            @Parameter(description = "Minimum number of posts") @RequestParam(defaultValue = "5") Long threshold) {
        log.debug("📝 Getting prolific users with >{}posts", threshold);
        List<ReadUserProfile> users = queryUserProfileService.getProlificUsers(threshold);
        return ResponseEntity.ok(users);
    }
}