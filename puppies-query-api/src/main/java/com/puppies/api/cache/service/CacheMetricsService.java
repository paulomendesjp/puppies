package com.puppies.api.cache.service;

import com.puppies.api.cache.PostMetrics;
import com.puppies.api.cache.strategy.HotPostsCacheStrategy;
import com.puppies.api.common.constants.QueryApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Service responsible for cache metrics operations for individual posts.
 * Handles post-specific cache performance analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheMetricsService {

    private final HotPostsCacheStrategy hotPostsStrategy;

    /**
     * Get cache performance metrics for a specific post.
     */
    public Map<String, Object> getPostCacheMetrics(Long postId) {
        try {
            Map<String, Object> response = Map.of();
            
            // Get post metrics from hot posts strategy
            PostMetrics postMetrics = hotPostsStrategy.getPostMetrics(postId.toString());
            
            if (postMetrics != null) {
                // Real metrics from PostMetrics
                response = Map.of(
                    "postId", postId,
                    "status", "success",
                    "metrics", postMetrics.getSummary(),
                    "cacheInfo", Map.of(
                        "isWarmed", postMetrics.isWarmed(),
                        "shouldEvict", postMetrics.shouldEvict(),
                        "lastAccessed", postMetrics.getLastAccessed(),
                        "recommendedCacheLayer", determineOptimalCacheLayer(postMetrics)
                    ),
                    "performance", Map.of(
                        "trending", postMetrics.isTrending(),
                        "popularityScore", postMetrics.getPopularityScore(),
                        "engagementRate", postMetrics.getEngagementRate(),
                        "viewsLastHour", postMetrics.getViewsInLastHour()
                    ),
                    "timestamp", LocalDateTime.now()
                );
                
                log.info("📊 Retrieved real metrics for post {}: {} views, {}% engagement", 
                        postId, postMetrics.getTotalViews().get(), 
                        String.format("%.2f", postMetrics.getEngagementRate() * 100));
                
            } else {
                // Post not in cache metrics yet - provide basic info
                PostMetrics newMetrics = new PostMetrics(postId);
                hotPostsStrategy.updatePostMetrics(postId.toString(), newMetrics);
                
                response = Map.of(
                    "postId", postId,
                    "status", "no_metrics",
                    "message", "Post not yet tracked in cache metrics",
                    "suggestion", "Post will be tracked after first access",
                    "metrics", newMetrics.getSummary(),
                    "timestamp", LocalDateTime.now()
                );
                
                log.info("📊 Created new metrics tracking for post {}", postId);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting post cache metrics for post {}", postId, e);
            throw new RuntimeException("Failed to get post metrics for post " + postId + ": " + e.getMessage());
        }
    }
    
    /**
     * Determine optimal cache layer for a post based on its metrics.
     */
    private String determineOptimalCacheLayer(PostMetrics metrics) {
        if (metrics.isTrending() || metrics.getViewsInLastHour() > QueryApiConstants.BusinessRules.HOT_CACHE_VIEWS_THRESHOLD) {
            return QueryApiConstants.CacheNames.HOT_POSTS;
        } else if (metrics.getTotalViews().get() > QueryApiConstants.BusinessRules.WARM_CACHE_VIEWS_THRESHOLD || 
                   metrics.getEngagementRate() > QueryApiConstants.BusinessRules.WARM_CACHE_ENGAGEMENT_THRESHOLD) {
            return QueryApiConstants.CacheNames.WARM_POSTS;
        } else {
            return QueryApiConstants.CacheNames.COLD_POSTS;
        }
    }
}