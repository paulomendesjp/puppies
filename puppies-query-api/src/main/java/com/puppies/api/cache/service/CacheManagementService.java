package com.puppies.api.cache.service;

import com.puppies.api.cache.IntelligentCacheService;
import com.puppies.api.common.constants.QueryApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for cache management operations.
 * Handles cache warming, clearing, and layer management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementService {

    private final IntelligentCacheService intelligentCacheService;
    private final CacheManager cacheManager;

    /**
     * Force cache warming for trending content.
     */
    public Map<String, Object> forceCacheWarming() {
        try {
            intelligentCacheService.warmCacheWithTrendingContent();
            
            return Map.of(
                "status", "success",
                "message", QueryApiConstants.SuccessMessages.CACHE_WARMING_TRIGGERED,
                "timestamp", LocalDateTime.now()
            );
            
        } catch (Exception e) {
            log.error("Error during forced cache warming", e);
            throw new RuntimeException("Failed to warm cache: " + e.getMessage());
        }
    }

    /**
     * Clear specific cache layers.
     */
    public Map<String, Object> clearCacheLayer(String layerName) {
        try {
            var cache = cacheManager.getCache(layerName);
            if (cache != null) {
                cache.clear();
                log.info(QueryApiConstants.LogMessages.CACHE_CLEARED, layerName);
                
                return Map.of(
                    "status", "success",
                    "message", QueryApiConstants.SuccessMessages.CACHE_LAYER_CLEARED + layerName,
                    "timestamp", LocalDateTime.now()
                );
            } else {
                throw new IllegalArgumentException(QueryApiConstants.ErrorMessages.CACHE_LAYER_NOT_FOUND + layerName);
            }
            
        } catch (Exception e) {
            log.error("Error clearing cache layer {}", layerName, e);
            throw new RuntimeException(QueryApiConstants.ErrorMessages.FAILED_TO_CLEAR_CACHE);
        }
    }

    /**
     * Get list of available cache layers with descriptions.
     */
    public Map<String, Object> getCacheLayers() {
        List<String> layers = Arrays.asList(
            QueryApiConstants.CacheNames.HOT_POSTS, QueryApiConstants.CacheNames.WARM_POSTS, QueryApiConstants.CacheNames.COLD_POSTS,
            QueryApiConstants.CacheNames.HOT_FEED, QueryApiConstants.CacheNames.WARM_FEED, QueryApiConstants.CacheNames.COLD_FEED,
            QueryApiConstants.CacheNames.USER_BEHAVIOR, QueryApiConstants.CacheNames.POSTS, 
            QueryApiConstants.CacheNames.USERS, QueryApiConstants.CacheNames.FEED
        );
        
        return Map.of(
            "cacheLayers", layers,
            "description", Map.of(
                QueryApiConstants.CacheNames.HOT_POSTS, QueryApiConstants.CacheDescriptions.HOT_POSTS_DESC,
                QueryApiConstants.CacheNames.WARM_POSTS, QueryApiConstants.CacheDescriptions.WARM_POSTS_DESC,
                QueryApiConstants.CacheNames.COLD_POSTS, QueryApiConstants.CacheDescriptions.COLD_POSTS_DESC,
                QueryApiConstants.CacheNames.HOT_FEED, QueryApiConstants.CacheDescriptions.HOT_FEED_DESC,
                QueryApiConstants.CacheNames.WARM_FEED, QueryApiConstants.CacheDescriptions.WARM_FEED_DESC,
                QueryApiConstants.CacheNames.COLD_FEED, QueryApiConstants.CacheDescriptions.COLD_FEED_DESC,
                QueryApiConstants.CacheNames.USER_BEHAVIOR, QueryApiConstants.CacheDescriptions.USER_BEHAVIOR_DESC,
                QueryApiConstants.CacheNames.POSTS, QueryApiConstants.CacheDescriptions.POSTS_DESC,
                QueryApiConstants.CacheNames.USERS, QueryApiConstants.CacheDescriptions.USERS_DESC,
                QueryApiConstants.CacheNames.FEED, QueryApiConstants.CacheDescriptions.FEED_DESC
            )
        );
    }
}