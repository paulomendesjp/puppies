package com.puppies.api.cache;

import com.puppies.api.cache.service.CacheInsightsService;
import com.puppies.api.cache.service.CacheManagementService;
import com.puppies.api.cache.service.CacheMetricsService;
import com.puppies.api.cache.service.CacheSimulationService;
import com.puppies.api.cache.service.CacheStatsService;
import com.puppies.api.common.constants.QueryApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for cache monitoring, analytics, and management.
 * 
 * Provides endpoints for:
 * - Cache performance statistics
 * - Intelligent caching insights
 * - User behavior analysis
 * - Cache optimization recommendations
 * - Manual cache management operations
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheStatsService cacheStatsService;
    private final CacheMetricsService cacheMetricsService;
    private final CacheManagementService cacheManagementService;
    private final CacheInsightsService cacheInsightsService;
    private final CacheSimulationService cacheSimulationService;

    /**
     * Get comprehensive cache statistics and performance metrics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        try {
            Map<String, Object> response = cacheStatsService.getCacheStatistics();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", QueryApiConstants.ErrorMessages.FAILED_TO_GET_CACHE_STATS, "message", e.getMessage()));
        }
    }

    /**
     * Get user-specific cache behavior analysis.
     */
    @GetMapping("/users/{userId}/analysis")
    public ResponseEntity<Map<String, Object>> getUserCacheAnalysis(@PathVariable Long userId) {
        try {
            Map<String, Object> response = cacheStatsService.getUserCacheAnalysis(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error analyzing user cache behavior for user {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", QueryApiConstants.ErrorMessages.FAILED_TO_ANALYZE_USER_CACHE));
        }
    }

    /**
     * Get cache performance for a specific post.
     */
    @GetMapping("/posts/{postId}/metrics")
    public ResponseEntity<Map<String, Object>> getPostCacheMetrics(@PathVariable Long postId) {
        try {
            Map<String, Object> response = cacheMetricsService.getPostCacheMetrics(postId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting post cache metrics for post {}", postId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                        "error", QueryApiConstants.ErrorMessages.FAILED_TO_GET_POST_METRICS, 
                        "postId", postId,
                        "message", e.getMessage()
                    ));
        }
    }


    /**
     * Force cache warming for trending content.
     */
    @PostMapping("/warm")
    public ResponseEntity<Map<String, Object>> forceCacheWarming() {
        try {
            Map<String, Object> response = cacheManagementService.forceCacheWarming();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during forced cache warming", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", QueryApiConstants.ErrorMessages.FAILED_TO_WARM_CACHE, "message", e.getMessage()));
        }
    }

    /**
     * Clear specific cache layers.
     */
    @DeleteMapping("/layers/{layerName}")
    public ResponseEntity<Map<String, Object>> clearCacheLayer(@PathVariable String layerName) {
        try {
            Map<String, Object> response = cacheManagementService.clearCacheLayer(layerName);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error clearing cache layer {}", layerName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", QueryApiConstants.ErrorMessages.FAILED_TO_CLEAR_CACHE));
        }
    }

    /**
     * Get list of available cache layers.
     */
    @GetMapping("/layers")
    public ResponseEntity<Map<String, Object>> getCacheLayers() {
        Map<String, Object> response = cacheManagementService.getCacheLayers();
        return ResponseEntity.ok(response);
    }

    /**
     * Get cache insights and recommendations dashboard.
     */
    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getCacheInsights() {
        try {
            Map<String, Object> insights = cacheInsightsService.getCacheInsights();
            return ResponseEntity.ok(insights);
        } catch (Exception e) {
            log.error("Error getting cache insights", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", QueryApiConstants.ErrorMessages.FAILED_TO_GET_CACHE_INSIGHTS));
        }
    }

    /**
     * Simulate cache load for testing (development only).
     */
    @PostMapping("/simulate-load")
    public ResponseEntity<Map<String, Object>> simulateLoad(
            @RequestParam(defaultValue = "100") int requests,
            @RequestParam(defaultValue = "10") int users,
            @RequestParam(defaultValue = "50") int posts) {
        
        try {
            Map<String, Object> response = cacheSimulationService.simulateLoad(requests, users, posts);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error during cache load simulation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", QueryApiConstants.ErrorMessages.FAILED_TO_SIMULATE_CACHE_LOAD));
        }
    }
}