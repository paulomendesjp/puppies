package com.puppies.api.cache.strategy;

/**
 * Base interface for intelligent caching strategies.
 * 
 * Each strategy implements specific logic for:
 * - When to cache content
 * - How long to cache it (TTL)
 * - Which cache layer to use
 * - When to evict content
 */
public interface CacheStrategy {
    
    /**
     * Determine if content should be cached based on strategy criteria.
     */
    boolean shouldCache(Object content, String cacheKey, Object... context);
    
    /**
     * Get recommended TTL in seconds for this content.
     */
    int getRecommendedTtlSeconds(Object content, String cacheKey, Object... context);
    
    /**
     * Get the cache layer name to use for this content.
     */
    String getTargetCacheLayer(Object content, String cacheKey, Object... context);
    
    /**
     * Determine if cached content should be evicted.
     */
    boolean shouldEvict(String cacheKey, Object cachedContent, Object... context);
    
    /**
     * Get strategy name for logging and monitoring.
     */
    String getStrategyName();
}