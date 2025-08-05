package com.puppies.api.cache.service;

import com.puppies.api.cache.IntelligentCacheService;
import com.puppies.api.cache.metrics.CacheMetrics;
import com.puppies.api.cache.strategy.HotPostsCacheStrategy;
import com.puppies.api.cache.strategy.UserBehaviorCacheStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service responsible for cache statistics and performance metrics.
 * Centralizes cache monitoring and analytics functionality.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheStatsService {

    private final IntelligentCacheService intelligentCacheService;
    private final CacheMetrics cacheMetrics;
    private final HotPostsCacheStrategy hotPostsStrategy;
    private final UserBehaviorCacheStrategy userBehaviorStrategy;

    /**
     * Get comprehensive cache statistics and performance metrics.
     */
    public Map<String, Object> getCacheStatistics() {
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
            response.put("timestamp", LocalDateTime.now());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            throw new RuntimeException("Failed to get cache statistics: " + e.getMessage());
        }
    }

    /**
     * Get user-specific cache behavior analysis.
     */
    public Map<String, Object> getUserCacheAnalysis(Long userId) {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // User behavior analysis
            response.put("behaviorAnalysis", userBehaviorStrategy.analyzeUserBehavior(userId.toString()));
            response.put("recommendations", userBehaviorStrategy.getPersonalizedRecommendations(userId.toString()));
            
            response.put("status", "success");
            response.put("userId", userId);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error analyzing user cache behavior for user {}", userId, e);
            throw new RuntimeException("Failed to analyze user cache behavior");
        }
    }
}