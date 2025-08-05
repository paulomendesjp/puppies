package com.puppies.api.cache.strategy;

import com.puppies.api.cache.PostMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache strategy for hot/trending posts based on engagement metrics.
 * 
 * This strategy prioritizes caching of popular content that's likely
 * to be accessed frequently, using real-time engagement data to make
 * intelligent caching decisions.
 */
@Component
@Slf4j
public class HotPostsCacheStrategy implements CacheStrategy {

    // Track post metrics for decision making
    private final Map<String, PostMetrics> postMetricsCache = new ConcurrentHashMap<>();
    
    // Thresholds for hot post classification
    private static final int HOT_VIEWS_THRESHOLD = 50;
    private static final double HOT_ENGAGEMENT_THRESHOLD = 0.1; // 10%
    private static final int TRENDING_VIEWS_PER_HOUR = 20;

    @Override
    public boolean shouldCache(Object content, String cacheKey, Object... context) {
        PostMetrics metrics = extractPostMetrics(cacheKey, context);
        if (metrics == null) return true; // Default to caching if no metrics
        
        // Always cache if it's trending or has high engagement
        boolean shouldCache = metrics.isTrending() || 
                            metrics.getTotalViews().get() > HOT_VIEWS_THRESHOLD ||
                            metrics.getEngagementRate() > HOT_ENGAGEMENT_THRESHOLD;
        
        log.debug("🔥 Hot posts strategy - Post {}: shouldCache={}, trending={}, views={}, engagement={:.2f}%",
                extractPostId(cacheKey), shouldCache, metrics.isTrending(), 
                metrics.getTotalViews(), metrics.getEngagementRate() * 100);
        
        return shouldCache;
    }

    @Override
    public int getRecommendedTtlSeconds(Object content, String cacheKey, Object... context) {
        PostMetrics metrics = extractPostMetrics(cacheKey, context);
        if (metrics == null) return 300; // 5 minutes default
        
        // Dynamic TTL based on engagement level
        if (metrics.isTrending()) {
            return 1800; // 30 minutes for trending posts
        }
        
        if (metrics.getViewsInLastHour() > TRENDING_VIEWS_PER_HOUR) {
            return 900; // 15 minutes for active posts
        }
        
        if (metrics.getEngagementRate() > HOT_ENGAGEMENT_THRESHOLD) {
            return 600; // 10 minutes for high engagement
        }
        
        return 300; // 5 minutes for regular posts
    }

    @Override
    public String getTargetCacheLayer(Object content, String cacheKey, Object... context) {
        PostMetrics metrics = extractPostMetrics(cacheKey, context);
        if (metrics == null) return "warm_posts";
        
        // Classify into cache layers based on hotness
        if (metrics.isTrending() && metrics.getViewsInLastHour() > TRENDING_VIEWS_PER_HOUR) {
            return "hot_posts";
        }
        
        if (metrics.getTotalViews().get() > HOT_VIEWS_THRESHOLD || 
            metrics.getEngagementRate() > HOT_ENGAGEMENT_THRESHOLD) {
            return "warm_posts";
        }
        
        return "cold_posts";
    }

    @Override
    public boolean shouldEvict(String cacheKey, Object cachedContent, Object... context) {
        PostMetrics metrics = extractPostMetrics(cacheKey, context);
        if (metrics == null) return false;
        
        // Don't evict trending or highly engaged content
        if (metrics.isTrending() || metrics.getEngagementRate() > HOT_ENGAGEMENT_THRESHOLD) {
            return false;
        }
        
        // Evict if post has become inactive
        boolean shouldEvict = metrics.shouldEvict();
        
        if (shouldEvict) {
            log.debug("🗑️ Evicting post {} - no recent activity", extractPostId(cacheKey));
        }
        
        return shouldEvict;
    }

    @Override
    public String getStrategyName() {
        return "HotPostsStrategy";
    }

    /**
     * Extract post metrics from context or cache key.
     */
    private PostMetrics extractPostMetrics(String cacheKey, Object... context) {
        // Try to get from context first
        for (Object ctx : context) {
            if (ctx instanceof PostMetrics) {
                return (PostMetrics) ctx;
            }
        }
        
        // Try to extract from cache key and get from our cache
        String postId = extractPostId(cacheKey);
        if (postId != null) {
            return postMetricsCache.get(postId);
        }
        
        return null;
    }

    /**
     * Extract post ID from cache key.
     * Assumes cache key format like "post:123:user:456"
     */
    private String extractPostId(String cacheKey) {
        if (cacheKey != null && cacheKey.startsWith("post:")) {
            String[] parts = cacheKey.split(":");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    /**
     * Update post metrics for strategy decisions.
     */
    public void updatePostMetrics(String postId, PostMetrics metrics) {
        postMetricsCache.put(postId, metrics);
        
        // Clean up old metrics periodically
        if (postMetricsCache.size() > 10000) {
            cleanupOldMetrics();
        }
    }
    
    /**
     * Get post metrics for a specific post ID.
     */
    public PostMetrics getPostMetrics(String postId) {
        return postMetricsCache.get(postId);
    }

    /**
     * Clean up old metrics to prevent memory leaks.
     */
    private void cleanupOldMetrics() {
        postMetricsCache.entrySet().removeIf(entry -> 
            entry.getValue().shouldEvict());
        
        log.debug("🧹 Cleaned up old post metrics, remaining: {}", postMetricsCache.size());
    }

    /**
     * Get strategy statistics for monitoring.
     */
    public Map<String, Object> getStrategyStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        
        long trendingPosts = postMetricsCache.values().stream()
                .mapToLong(m -> m.isTrending() ? 1 : 0)
                .sum();
        
        long hotPosts = postMetricsCache.values().stream()
                .mapToLong(m -> m.getTotalViews().get() > HOT_VIEWS_THRESHOLD ? 1 : 0)
                .sum();
        
        double avgEngagementRate = postMetricsCache.values().stream()
                .mapToDouble(PostMetrics::getEngagementRate)
                .average()
                .orElse(0.0);
        
        stats.put("trackedPosts", postMetricsCache.size());
        stats.put("trendingPosts", trendingPosts);
        stats.put("hotPosts", hotPosts);
        stats.put("avgEngagementRate", String.format("%.2f%%", avgEngagementRate * 100));
        stats.put("strategyName", getStrategyName());
        
        return stats;
    }
}