package com.puppies.api.cache;

import com.puppies.api.cache.metrics.CacheMetrics;
import com.puppies.api.cache.strategy.HotPostsCacheStrategy;
import com.puppies.api.cache.strategy.UserBehaviorCacheStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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

    private final IntelligentCacheService intelligentCacheService;
    private final CacheMetrics cacheMetrics;
    private final HotPostsCacheStrategy hotPostsStrategy;
    private final UserBehaviorCacheStrategy userBehaviorStrategy;
    private final CacheManager cacheManager;

    /**
     * Get comprehensive cache statistics and performance metrics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Overall cache statistics
            response.put("overview", intelligentCacheService.getCacheStatistics());
            
            // Performance metrics
            response.put("performance", cacheMetrics.getOverallStats());
            response.put("trends", cacheMetrics.getPerformanceTrends());
            
            // Strategy statistics
            response.put("hotPostsStrategy", hotPostsStrategy.getStrategyStats());
            response.put("userBehaviorStrategy", userBehaviorStrategy.getStrategyStats());
            
            // Optimization recommendations
            response.put("recommendations", cacheMetrics.getOptimizationRecommendations());
            
            response.put("status", "success");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get cache statistics", "message", e.getMessage()));
        }
    }

    /**
     * Get user-specific cache behavior analysis.
     */
    @GetMapping("/users/{userId}/analysis")
    public ResponseEntity<Map<String, Object>> getUserCacheAnalysis(@PathVariable Long userId) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // User behavior analysis
            response.put("behaviorAnalysis", userBehaviorStrategy.analyzeUserBehavior(userId.toString()));
            response.put("recommendations", userBehaviorStrategy.getPersonalizedRecommendations(userId.toString()));
            
            response.put("status", "success");
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error analyzing user cache behavior for user {}", userId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to analyze user cache behavior"));
        }
    }

    /**
     * Get cache performance for a specific post.
     */
    @GetMapping("/posts/{postId}/metrics")
    public ResponseEntity<Map<String, Object>> getPostCacheMetrics(@PathVariable Long postId) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Get post metrics from hot posts strategy
            PostMetrics postMetrics = hotPostsStrategy.getPostMetrics(postId.toString());
            
            if (postMetrics != null) {
                // Real metrics from PostMetrics
                response.put("postId", postId);
                response.put("status", "success");
                response.put("metrics", postMetrics.getSummary());
                
                // Cache performance data
                response.put("cacheInfo", Map.of(
                    "isWarmed", postMetrics.isWarmed(),
                    "shouldEvict", postMetrics.shouldEvict(),
                    "lastAccessed", postMetrics.getLastAccessed(),
                    "recommendedCacheLayer", determineOptimalCacheLayer(postMetrics)
                ));
                
                // Performance indicators
                response.put("performance", Map.of(
                    "trending", postMetrics.isTrending(),
                    "popularityScore", postMetrics.getPopularityScore(),
                    "engagementRate", postMetrics.getEngagementRate(),
                    "viewsLastHour", postMetrics.getViewsInLastHour()
                ));
                
                log.info("📊 Retrieved real metrics for post {}: {} views, {}% engagement", 
                        postId, postMetrics.getTotalViews().get(), 
                        String.format("%.2f", postMetrics.getEngagementRate() * 100));
                
            } else {
                // Post not in cache metrics yet - provide basic info
                response.put("postId", postId);
                response.put("status", "no_metrics");
                response.put("message", "Post not yet tracked in cache metrics");
                response.put("suggestion", "Post will be tracked after first access");
                
                // Initialize metrics for this post
                PostMetrics newMetrics = new PostMetrics(postId);
                hotPostsStrategy.updatePostMetrics(postId.toString(), newMetrics);
                
                response.put("metrics", newMetrics.getSummary());
                
                log.info("📊 Created new metrics tracking for post {}", postId);
            }
            
            response.put("timestamp", java.time.LocalDateTime.now());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error getting post cache metrics for post {}", postId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                        "error", "Failed to get post metrics", 
                        "postId", postId,
                        "message", e.getMessage()
                    ));
        }
    }
    
    /**
     * Determine optimal cache layer for a post based on its metrics.
     */
    private String determineOptimalCacheLayer(PostMetrics metrics) {
        if (metrics.isTrending() || metrics.getViewsInLastHour() > 50) {
            return "hot_posts";
        } else if (metrics.getTotalViews().get() > 20 || metrics.getEngagementRate() > 0.02) {
            return "warm_posts";
        } else {
            return "cold_posts";
        }
    }

    /**
     * Force cache warming for trending content.
     */
    @PostMapping("/warm")
    public ResponseEntity<Map<String, Object>> forceCacheWarming() {
        try {
            intelligentCacheService.warmCacheWithTrendingContent();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache warming triggered successfully");
            response.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during forced cache warming", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to warm cache", "message", e.getMessage()));
        }
    }

    /**
     * Clear specific cache layers.
     */
    @DeleteMapping("/layers/{layerName}")
    public ResponseEntity<Map<String, Object>> clearCacheLayer(@PathVariable String layerName) {
        try {
            var cache = cacheManager.getCache(layerName);
            if (cache != null) {
                cache.clear();
                log.info("🧹 Cleared cache layer: {}", layerName);
                
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Cache layer cleared: " + layerName,
                    "timestamp", java.time.LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cache layer not found: " + layerName));
            }
            
        } catch (Exception e) {
            log.error("Error clearing cache layer {}", layerName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to clear cache layer"));
        }
    }

    /**
     * Get list of available cache layers.
     */
    @GetMapping("/layers")
    public ResponseEntity<Map<String, Object>> getCacheLayers() {
        List<String> layers = Arrays.asList(
            "hot_posts", "warm_posts", "cold_posts",
            "hot_feed", "warm_feed", "cold_feed",
            "user_behavior", "posts", "users", "feed"
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("cacheLayers", layers);
        response.put("description", Map.of(
            "hot_posts", "Trending/viral content (30min TTL)",
            "warm_posts", "Popular content (15min TTL)",
            "cold_posts", "Less popular content (5min TTL)",
            "hot_feed", "High engagement user feeds (10min TTL)",
            "warm_feed", "Standard user feeds (5min TTL)",
            "cold_feed", "Low engagement user feeds (2min TTL)",
            "user_behavior", "User behavior profiles (1hour TTL)",
            "posts", "Legacy post cache (5min TTL)",
            "users", "Legacy user cache (15min TTL)",
            "feed", "Legacy feed cache (2min TTL)"
        ));
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get cache insights and recommendations dashboard.
     */
    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getCacheInsights() {
        Map<String, Object> insights = new HashMap<>();
        
        try {
            // Performance insights
            Map<String, Object> performance = cacheMetrics.getOverallStats();
            insights.put("performance", performance);
            
            // Usage patterns
            Map<String, Object> patterns = new HashMap<>();
            patterns.put("userEngagement", userBehaviorStrategy.getUsersByEngagementLevel());
            patterns.put("strategies", Map.of(
                "hotPosts", hotPostsStrategy.getStrategyStats(),
                "userBehavior", userBehaviorStrategy.getStrategyStats()
            ));
            insights.put("patterns", patterns);
            
            // Recommendations
            List<String> recommendations = cacheMetrics.getOptimizationRecommendations();
            insights.put("recommendations", recommendations);
            
            // Health indicators
            Map<String, String> health = new HashMap<>();
            
            // Check overall cache health
            Map<String, Object> stats = intelligentCacheService.getCacheStatistics();
            Object metricsObj = stats.get("cacheMetrics");
            if (metricsObj instanceof Map<?, ?> metrics) {
                // Analyze hit rates
                Object hitRatesObj = metrics.get("hitRates");
                if (hitRatesObj instanceof Map<?, ?> hitRates) {
                    boolean allGood = hitRates.values().stream()
                            .allMatch(rate -> {
                                String rateStr = rate.toString().replace("%", "");
                                return Double.parseDouble(rateStr) > 70.0;
                            });
                    health.put("hitRates", allGood ? "GOOD" : "NEEDS_ATTENTION");
                }
            }
            
            // Check for active alerts
            if (recommendations.isEmpty()) {
                health.put("overall", "EXCELLENT");
            } else if (recommendations.size() <= 2) {
                health.put("overall", "GOOD");
            } else {
                health.put("overall", "NEEDS_OPTIMIZATION");
            }
            
            insights.put("health", health);
            insights.put("status", "success");
            
            return ResponseEntity.ok(insights);
            
        } catch (Exception e) {
            log.error("Error getting cache insights", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get cache insights"));
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
        
        if (requests > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximum 1000 requests allowed for simulation"));
        }
        
        try {
            // Simulate cache operations for testing
            Random random = new Random();
            
            for (int i = 0; i < requests; i++) {
                String userId = String.valueOf(random.nextInt(users) + 1);
                String postId = String.valueOf(random.nextInt(posts) + 1);
                boolean cacheHit = random.nextBoolean();
                
                // Simulate user interaction
                userBehaviorStrategy.recordUserInteraction(userId, "view", cacheHit);
                
                // Simulate cache metrics
                String cacheLayer = random.nextBoolean() ? "hot_posts" : "warm_posts";
                if (cacheHit) {
                    cacheMetrics.recordHit(cacheLayer);
                } else {
                    cacheMetrics.recordMiss(cacheLayer);
                }
                
                // Simulate response time
                long responseTime = 50 + random.nextInt(200); // 50-250ms
                cacheMetrics.recordResponseTime("getPost", responseTime);
            }
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", String.format("Simulated %d requests for %d users and %d posts", 
                                       requests, users, posts),
                "timestamp", java.time.LocalDateTime.now()
            ));
            
        } catch (Exception e) {
            log.error("Error during cache load simulation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to simulate cache load"));
        }
    }
}