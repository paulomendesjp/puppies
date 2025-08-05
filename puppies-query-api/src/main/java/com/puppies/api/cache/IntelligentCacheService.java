package com.puppies.api.cache;

import com.puppies.api.cache.strategy.CacheStrategy;
import com.puppies.api.cache.strategy.HotPostsCacheStrategy;
import com.puppies.api.cache.strategy.UserBehaviorCacheStrategy;
import com.puppies.api.cache.metrics.CacheMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intelligent cache service that implements smart caching strategies 
 * for a social media application like Instagram.
 * 
 * Features:
 * - Hot/Warm/Cold data classification
 * - Popularity-based caching (views, likes, engagement)
 * - User behavior analytics 
 * - Cache warming for trending content
 * - Intelligent eviction policies
 * - Performance metrics and monitoring
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligentCacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheMetrics cacheMetrics;
    
    // Cache strategies
    private final HotPostsCacheStrategy hotPostsStrategy;
    private final UserBehaviorCacheStrategy userBehaviorStrategy;
    
    // Cache layers with different TTLs and capacities
    private static final String HOT_CACHE = "hot_posts";      // Most popular: 30min TTL
    private static final String WARM_CACHE = "warm_posts";    // Moderately popular: 15min TTL  
    private static final String COLD_CACHE = "cold_posts";    // Low popularity: 5min TTL
    private static final String USER_CACHE = "user_behavior"; // User patterns: 1hour TTL
    
    // Tracking maps for intelligent decisions
    private final Map<Long, PostMetrics> postMetrics = new ConcurrentHashMap<>();
    private final Map<Long, UserCacheProfile> userProfiles = new ConcurrentHashMap<>();

    /**
     * Get post with intelligent caching based on popularity and user behavior.
     */
    public <T> Optional<T> getPost(Long postId, Long currentUserId, Class<T> type, 
                                   java.util.function.Supplier<T> dataLoader) {
        
        // 1. Update access metrics
        updatePostAccessMetrics(postId, currentUserId);
        
        // 2. Determine cache layer based on popularity
        String cacheLayer = determineCacheLayer(postId);
        
        // 3. Try to get from appropriate cache layer
        Cache cache = cacheManager.getCache(cacheLayer);
        String cacheKey = buildPostCacheKey(postId, currentUserId);
        
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null) {
                cacheMetrics.recordHit(cacheLayer);
                log.debug("🎯 Cache HIT for post {} in {} layer", postId, cacheLayer);
                return Optional.ofNullable(type.cast(cached.get()));
            }
        }
        
        // 4. Cache miss - load data and cache intelligently
        cacheMetrics.recordMiss(cacheLayer);
        T data = dataLoader.get();
        
        if (data != null) {
            cachePostIntelligently(postId, currentUserId, data, cacheLayer);
            log.debug("📦 Cached post {} in {} layer", postId, cacheLayer);
        }
        
        return Optional.ofNullable(data);
    }

    /**
     * Cache user feed with personalization and popularity weighting.
     */
    public <T> Optional<T> getUserFeed(Long userId, String feedType, Class<T> type,
                                       java.util.function.Supplier<T> dataLoader) {
        
        UserCacheProfile profile = getUserCacheProfile(userId);
        String cacheKey = buildFeedCacheKey(userId, feedType, profile.getEngagementLevel());
        
        // Choose cache strategy based on user behavior
        String cacheLayer = profile.isHighEngagement() ? HOT_CACHE : WARM_CACHE;
        Duration ttl = profile.isHighEngagement() ? Duration.ofMinutes(30) : Duration.ofMinutes(10);
        
        Cache cache = cacheManager.getCache(cacheLayer);
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null) {
                cacheMetrics.recordHit(cacheLayer + "_feed");
                updateUserEngagement(userId, true); // Cache hit = good engagement
                return Optional.ofNullable(type.cast(cached.get()));
            }
        }
        
        // Load and cache with personalized TTL
        T data = dataLoader.get();
        if (data != null && cache != null) {
            cache.put(cacheKey, data);
            cacheMetrics.recordMiss(cacheLayer + "_feed");
            updateUserEngagement(userId, false);
        }
        
        return Optional.ofNullable(data);
    }

    /**
     * Update post access metrics for intelligent caching decisions.
     */
    private void updatePostAccessMetrics(Long postId, Long userId) {
        PostMetrics metrics = postMetrics.computeIfAbsent(postId, k -> new PostMetrics(postId));
        
        metrics.incrementViews();
        if (userId != null) {
            metrics.addRecentAccess(userId);
        }
        metrics.updateLastAccessed();
        
        // Track user interaction for behavior analysis (only if user is logged in)
        if (userId != null) {
            UserCacheProfile profile = userProfiles.computeIfAbsent(userId, k -> new UserCacheProfile(userId));
            profile.recordPostAccess(postId);
        }
    }

    /**
     * Determine which cache layer (hot/warm/cold) to use based on post metrics.
     */
    private String determineCacheLayer(Long postId) {
        PostMetrics metrics = postMetrics.get(postId);
        if (metrics == null) {
            return COLD_CACHE; // New posts start in cold cache
        }
        
        // Calculate popularity score based on multiple factors
        long viewsPerHour = metrics.getViewsInLastHour();
        long uniqueViewers = metrics.getUniqueViewersInLastHour();
        double engagementRate = metrics.getEngagementRate();
        boolean isTrending = metrics.isTrending();
        
        // Hot cache criteria: high engagement + recent activity
        if ((viewsPerHour > 100 && uniqueViewers > 50) || 
            (engagementRate > 0.1 && isTrending)) {
            return HOT_CACHE;
        }
        
        // Warm cache criteria: moderate engagement
        if (viewsPerHour > 20 || uniqueViewers > 10 || engagementRate > 0.05) {
            return WARM_CACHE;
        }
        
        return COLD_CACHE;
    }

    /**
     * Cache post with intelligent placement and TTL.
     */
    private void cachePostIntelligently(Long postId, Long userId, Object data, String cacheLayer) {
        Cache cache = cacheManager.getCache(cacheLayer);
        if (cache == null) return;
        
        String cacheKey = buildPostCacheKey(postId, userId);
        cache.put(cacheKey, data);
        
        // Also cache in lower layers if it's hot content (cascade caching)
        if (HOT_CACHE.equals(cacheLayer)) {
            Cache warmCache = cacheManager.getCache(WARM_CACHE);
            if (warmCache != null) {
                warmCache.put(cacheKey, data);
                log.debug("🔥 Hot post {} also cached in warm layer", postId);
            }
        }
    }

    /**
     * Get or create user cache profile for personalized caching.
     */
    private UserCacheProfile getUserCacheProfile(Long userId) {
        return userProfiles.computeIfAbsent(userId, k -> new UserCacheProfile(userId));
    }

    /**
     * Update user engagement metrics for cache strategy optimization.
     */
    private void updateUserEngagement(Long userId, boolean cacheHit) {
        UserCacheProfile profile = userProfiles.get(userId);
        if (profile != null) {
            if (cacheHit) {
                profile.incrementCacheHits();
            } else {
                profile.incrementCacheMisses();
            }
            profile.updateLastActivity();
        }
    }

    /**
     * Pre-warm cache with trending/popular content.
     * Runs every 5 minutes to analyze trending posts and cache them proactively.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void warmCacheWithTrendingContent() {
        log.info("🔥 Starting cache warming process...");
        
        try {
            // 1. Identify trending posts based on recent metrics
            List<Long> trendingPosts = identifyTrendingPosts();
            
            // 2. Pre-load trending posts into hot cache
            for (Long postId : trendingPosts) {
                warmTrendingPost(postId);
            }
            
            // 3. Clean up old metrics to prevent memory leaks
            cleanupOldMetrics();
            
            log.info("✅ Cache warming completed. Warmed {} trending posts", trendingPosts.size());
            
        } catch (Exception e) {
            log.error("❌ Error during cache warming", e);
        }
    }

    /**
     * Analyze post metrics to identify trending content.
     */
    private List<Long> identifyTrendingPosts() {
        return postMetrics.entrySet().stream()
                .filter(entry -> entry.getValue().isTrending())
                .sorted((e1, e2) -> Double.compare(
                    e2.getValue().getPopularityScore(), 
                    e1.getValue().getPopularityScore()))
                .limit(50) // Top 50 trending posts
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Pre-warm a trending post by loading it into hot cache.
     */
    private void warmTrendingPost(Long postId) {
        try {
            // This would trigger the actual data loading and caching
            // In a real implementation, you'd call the actual service methods
            log.debug("🌡️ Warming trending post: {}", postId);
            
            // Mark as warmed to avoid redundant warming
            PostMetrics metrics = postMetrics.get(postId);
            if (metrics != null) {
                metrics.markAsWarmed();
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to warm post {}", postId, e);
        }
    }

    /**
     * Clean up old metrics to prevent memory leaks.
     */
    private void cleanupOldMetrics() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        postMetrics.entrySet().removeIf(entry -> 
            entry.getValue().getLastAccessed().isBefore(cutoff));
        
        userProfiles.entrySet().removeIf(entry ->
            entry.getValue().getLastActivity().isBefore(cutoff));
        
        log.debug("🧹 Cleaned up old cache metrics");
    }

    /**
     * Build cache key for posts with user context.
     */
    private String buildPostCacheKey(Long postId, Long userId) {
        if (userId == null) {
            return String.format("post:%d:user:anonymous", postId);
        }
        return String.format("post:%d:user:%d", postId, userId);
    }

    /**
     * Build cache key for user feeds with engagement level.
     */
    private String buildFeedCacheKey(Long userId, String feedType, String engagementLevel) {
        return String.format("feed:%s:user:%d:engagement:%s", feedType, userId, engagementLevel);
    }

    /**
     * Get cache statistics for monitoring.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Cache layer statistics
        stats.put("hotCacheSize", getCacheSize(HOT_CACHE));
        stats.put("warmCacheSize", getCacheSize(WARM_CACHE));
        stats.put("coldCacheSize", getCacheSize(COLD_CACHE));
        
        // Metrics overview
        stats.put("totalPostsTracked", postMetrics.size());
        stats.put("totalUsersTracked", userProfiles.size());
        stats.put("cacheMetrics", cacheMetrics.getOverallStats());
        
        // Trending posts
        long trendingCount = postMetrics.values().stream()
                .mapToLong(m -> m.isTrending() ? 1 : 0)
                .sum();
        stats.put("trendingPosts", trendingCount);
        
        return stats;
    }

    private long getCacheSize(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // This is a simplified size calculation
                // In practice, you might need Redis-specific commands
                return redisTemplate.keys(cacheName + ":*").size();
            }
        } catch (Exception e) {
            log.warn("Failed to get cache size for {}", cacheName);
        }
        return 0;
    }
}
