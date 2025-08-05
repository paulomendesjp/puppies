package com.puppies.api.read.service;

import com.puppies.api.common.constants.QueryApiConstants;
import com.puppies.api.read.model.ReadPost;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for caching operations related to posts.
 * Centralizes cache management and provides consistent caching behavior.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCacheService {

    private final CacheManager cacheManager;

    /**
     * Get cached post content for pagination.
     */
    @SuppressWarnings("unchecked")
    public List<ReadPost> getCachedPostContent(String cachePrefix, int page, int size) {
        try {
            Cache cache = cacheManager.getCache(QueryApiConstants.CacheNames.POST_CONTENT);
            String key = buildContentCacheKey(cachePrefix, page, size);
            log.debug("🔍 Looking for cache key: {}", key);
            
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    log.debug("🔍 Found cached value of type: {}", value != null ? value.getClass().getSimpleName() : "null");
                    if (value instanceof List) {
                        List<ReadPost> result = (List<ReadPost>) value;
                        log.info("✅ CACHE HIT - Found {} cached posts for key: {}", result.size(), key);
                        return result;
                    } else {
                        log.warn("❌ Cache value is not a List, it's: {}", value != null ? value.getClass().getName() : "null");
                    }
                } else {
                    log.debug("❌ No cache wrapper found for key: {}", key);
                }
            } else {
                log.warn("❌ Cache '{}' not found", QueryApiConstants.CacheNames.POST_CONTENT);
            }
        } catch (Exception e) {
            log.warn("Failed to get cached content for {}: {}", cachePrefix, e.getMessage());
        }
        return null;
    }
    
    /**
     * Get cached total count.
     */
    public Long getCachedPostTotal(String cachePrefix) {
        try {
            Cache cache = cacheManager.getCache(QueryApiConstants.CacheNames.POST_TOTAL);
            String key = buildTotalCacheKey(cachePrefix);
            log.debug("🔍 Looking for total cache key: {}", key);
            
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    log.debug("🔍 Found cached total value of type: {}", value != null ? value.getClass().getSimpleName() : "null");
                    if (value instanceof Long) {
                        Long result = (Long) value;
                        log.info("✅ CACHE HIT - Found cached total: {} for key: {}", result, key);
                        return result;
                    } else if (value instanceof Number) {
                        Long result = ((Number) value).longValue();
                        log.info("✅ CACHE HIT - Found cached total (converted): {} for key: {}", result, key);
                        return result;
                    } else {
                        log.warn("❌ Cache total is not a Number, it's: {}", value != null ? value.getClass().getName() : "null");
                    }
                } else {
                    log.debug("❌ No cache wrapper found for total key: {}", key);
                }
            } else {
                log.warn("❌ Cache '{}' not found", QueryApiConstants.CacheNames.POST_TOTAL);
            }
        } catch (Exception e) {
            log.warn("Failed to get cached total for {}: {}", cachePrefix, e.getMessage());
        }
        return null;
    }
    
    /**
     * Cache post content for pagination.
     */
    public void cachePostContent(String cachePrefix, int page, int size, List<ReadPost> content) {
        try {
            Cache cache = cacheManager.getCache(QueryApiConstants.CacheNames.POST_CONTENT);
            if (cache != null) {
                String key = buildContentCacheKey(cachePrefix, page, size);
                cache.put(key, content);
                log.debug("💾 Cached content: {} items for key {}", content.size(), key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache content for {}: {}", cachePrefix, e.getMessage());
        }
    }
    
    /**
     * Cache total count.
     */
    public void cachePostTotal(String cachePrefix, Long total) {
        try {
            Cache cache = cacheManager.getCache(QueryApiConstants.CacheNames.POST_TOTAL);
            if (cache != null) {
                String key = buildTotalCacheKey(cachePrefix);
                cache.put(key, total);
                log.debug("💾 Cached total: {} for key {}", total, key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache total for {}: {}", cachePrefix, e.getMessage());
        }
    }

    /**
     * Build cache key for content.
     */
    private String buildContentCacheKey(String cachePrefix, int page, int size) {
        return cachePrefix + QueryApiConstants.CacheKeys.CONTENT_SUFFIX + page + "_" + size;
    }

    /**
     * Build cache key for total count.
     */
    private String buildTotalCacheKey(String cachePrefix) {
        return cachePrefix + QueryApiConstants.CacheKeys.TOTAL_SUFFIX;
    }
}