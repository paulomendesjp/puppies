package com.puppies.api.cache.service;

import com.puppies.api.cache.metrics.CacheMetrics;
import com.puppies.api.cache.strategy.UserBehaviorCacheStrategy;
import com.puppies.api.common.constants.QueryApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

/**
 * Service responsible for cache load simulation for testing purposes.
 * Provides controlled simulation of cache operations for development and testing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheSimulationService {

    private final CacheMetrics cacheMetrics;
    private final UserBehaviorCacheStrategy userBehaviorStrategy;

    /**
     * Simulate cache load for testing (development only).
     */
    public Map<String, Object> simulateLoad(int requests, int users, int posts) {
        if (requests > QueryApiConstants.BusinessRules.MAX_SIMULATION_REQUESTS) {
            throw new IllegalArgumentException(QueryApiConstants.ErrorMessages.MAX_SIMULATION_REQUESTS_EXCEEDED);
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
                String cacheLayer = random.nextBoolean() ? 
                    QueryApiConstants.CacheNames.HOT_POSTS : 
                    QueryApiConstants.CacheNames.WARM_POSTS;
                    
                if (cacheHit) {
                    cacheMetrics.recordHit(cacheLayer);
                } else {
                    cacheMetrics.recordMiss(cacheLayer);
                }
                
                // Simulate response time (50-250ms)
                long responseTime = 50 + random.nextInt(200);
                cacheMetrics.recordResponseTime("getPost", responseTime);
            }
            
            return Map.of(
                "status", "success",
                "message", String.format(QueryApiConstants.SuccessMessages.SIMULATION_COMPLETED, 
                                       requests, users, posts),
                "timestamp", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("Error during cache load simulation", e);
            throw new RuntimeException(QueryApiConstants.ErrorMessages.FAILED_TO_SIMULATE_CACHE_LOAD);
        }
    }
}