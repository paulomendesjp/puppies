package com.puppies.api.cache.service;

import com.puppies.api.cache.metrics.CacheMetrics;
import com.puppies.api.cache.strategy.HotPostsCacheStrategy;
import com.puppies.api.cache.strategy.UserBehaviorCacheStrategy;
import com.puppies.api.common.constants.QueryApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for cache insights and recommendations.
 * Analyzes cache performance patterns and provides optimization suggestions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheInsightsService {

    private final CacheMetrics cacheMetrics;
    private final HotPostsCacheStrategy hotPostsStrategy;
    private final UserBehaviorCacheStrategy userBehaviorStrategy;
    private final CacheStatsService cacheStatsService;

    /**
     * Get cache insights and recommendations dashboard.
     */
    public Map<String, Object> getCacheInsights() {
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
            Map<String, String> health = analyzeSystemHealth(recommendations);
            insights.put("health", health);
            insights.put("status", "success");
            
            return insights;
            
        } catch (Exception e) {
            log.error("Error getting cache insights", e);
            throw new RuntimeException(QueryApiConstants.ErrorMessages.FAILED_TO_GET_CACHE_INSIGHTS);
        }
    }

    /**
     * Analyze system health based on cache statistics and recommendations.
     */
    private Map<String, String> analyzeSystemHealth(List<String> recommendations) {
        Map<String, String> health = new HashMap<>();
        
        // Check overall cache health using CacheStatsService
        Map<String, Object> stats = cacheStatsService.getCacheStatistics();
        Object metricsObj = stats.get("overview");
        if (metricsObj instanceof Map<?, ?> overview) {
            Object cacheMetricsObj = ((Map<?, ?>) overview).get("cacheMetrics");
            if (cacheMetricsObj instanceof Map<?, ?> metrics) {
                // Analyze hit rates
                Object hitRatesObj = metrics.get("hitRates");
                if (hitRatesObj instanceof Map<?, ?> hitRates) {
                    boolean allGood = hitRates.values().stream()
                            .allMatch(rate -> {
                                String rateStr = rate.toString().replace("%", "");
                                return Double.parseDouble(rateStr) > 70.0;
                            });
                    health.put("hitRates", allGood ? QueryApiConstants.HealthStatus.GOOD : QueryApiConstants.HealthStatus.NEEDS_ATTENTION);
                }
            }
        }
        
        // Check for active alerts
        if (recommendations.isEmpty()) {
            health.put("overall", QueryApiConstants.HealthStatus.EXCELLENT);
        } else if (recommendations.size() <= 2) {
            health.put("overall", QueryApiConstants.HealthStatus.GOOD);
        } else {
            health.put("overall", QueryApiConstants.HealthStatus.NEEDS_OPTIMIZATION);
        }
        
        return health;
    }
}