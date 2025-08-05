package com.puppies.api.read.service;

import com.puppies.api.read.model.ReadFeedItem;
import com.puppies.api.read.repository.ReadFeedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Query service for feeds - read-only operations from read store
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueryFeedService {

    private final ReadFeedItemRepository readFeedItemRepository;
    private final CacheManager cacheManager;

    /**
     * Get user's personalized feed (chronological)
     */
    public Page<ReadFeedItem> getUserFeed(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        String cachePrefix = "user_feed_" + userId;
        List<ReadFeedItem> cachedContent = getCachedFeedContent(cachePrefix, page, size);
        Long cachedTotal = getCachedFeedTotal(cachePrefix);
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("📱 CACHE HIT - Using cached user feed: userId={}, page={}, size={}, total={}", userId, page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("📱 CACHE MISS - Loading user feed from DB: userId={}, page={}, size={}", userId, page, size);
        Page<ReadFeedItem> result = readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        // Cache the content and total separately
        cacheFeedContent(cachePrefix, page, size, result.getContent());
        cacheFeedTotal(cachePrefix, result.getTotalElements());
        
        log.info("📱 CACHE STORE - Loaded {} feed items for user {}, total={}", result.getNumberOfElements(), userId, result.getTotalElements());
        return result;
    }

    /**
     * Get user's feed ordered by popularity
     */
    public Page<ReadFeedItem> getUserFeedByPopularity(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        String cachePrefix = "user_feed_popular_" + userId;
        List<ReadFeedItem> cachedContent = getCachedFeedContent(cachePrefix, page, size);
        Long cachedTotal = getCachedFeedTotal(cachePrefix);
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("🌟 CACHE HIT - Using cached popular feed: userId={}, page={}, size={}, total={}", userId, page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("🌟 CACHE MISS - Loading popular feed from DB: userId={}, page={}, size={}", userId, page, size);
        Page<ReadFeedItem> result = readFeedItemRepository.findByUserIdOrderByPopularityScoreDesc(userId, pageable);
        
        // Cache the content and total separately
        cacheFeedContent(cachePrefix, page, size, result.getContent());
        cacheFeedTotal(cachePrefix, result.getTotalElements());
        
        log.info("🌟 CACHE STORE - Loaded {} popular feed items for user {}, total={}", result.getNumberOfElements(), userId, result.getTotalElements());
        return result;
    }

    /**
     * Get global trending feed
     */
    public Page<ReadFeedItem> getTrendingFeed(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        String cachePrefix = "trending_feed";
        List<ReadFeedItem> cachedContent = getCachedFeedContent(cachePrefix, page, size);
        Long cachedTotal = getCachedFeedTotal(cachePrefix);
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("🌍 CACHE HIT - Using cached trending feed: page={}, size={}, total={}", page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("🌍 CACHE MISS - Loading trending feed from DB: page={}, size={}", page, size);
        Page<ReadFeedItem> result = readFeedItemRepository.findAllByOrderByPopularityScoreDesc(pageable);
        
        // Cache the content and total separately
        cacheFeedContent(cachePrefix, page, size, result.getContent());
        cacheFeedTotal(cachePrefix, result.getTotalElements());
        
        log.info("🌍 CACHE STORE - Loaded {} trending feed items, total={}", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    /**
     * Get recent engaging posts for user
     */
    @Cacheable(value = "engaging_posts", key = "#userId + '_' + #minLikes + '_' + #limit")
    public List<ReadFeedItem> getRecentEngagingPosts(Long userId, Long minLikes, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return readFeedItemRepository.findRecentEngagingPosts(userId, minLikes, pageable);
    }

    /**
     * Get posts from specific author in user's feed
     */
    public Page<ReadFeedItem> getAuthorPostsInFeed(Long userId, Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return readFeedItemRepository.findByUserIdAndPostAuthorIdOrderByCreatedAtDesc(userId, authorId, pageable);
    }

    /**
     * Get posts user has liked
     */
    public Page<ReadFeedItem> getUserLikedPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        String cachePrefix = "user_liked_posts_" + userId;
        List<ReadFeedItem> cachedContent = getCachedFeedContent(cachePrefix, page, size);
        Long cachedTotal = getCachedFeedTotal(cachePrefix);
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("❤️ CACHE HIT - Using cached liked posts: userId={}, page={}, size={}, total={}", userId, page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("❤️ CACHE MISS - Loading user liked posts from DB: userId={}, page={}, size={}", userId, page, size);
        Page<ReadFeedItem> result = readFeedItemRepository.findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(userId, pageable);
        
        // Cache the content and total separately
        cacheFeedContent(cachePrefix, page, size, result.getContent());
        cacheFeedTotal(cachePrefix, result.getTotalElements());
        
        log.info("❤️ CACHE STORE - Loaded {} liked posts for user {}, total={}", result.getNumberOfElements(), userId, result.getTotalElements());
        return result;
    }

    /**
     * Get discovery feed (high quality content from all users)
     */
    @Cacheable(value = "discovery_feed", key = "#minScore + '_' + #limit")
    public List<ReadFeedItem> getDiscoveryFeed(Double minScore, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return readFeedItemRepository.findDiscoveryFeed(minScore, pageable);
    }

    /**
     * Count feed items for user
     */
    @Cacheable(value = "user_feed_count", key = "#userId")
    public Long countUserFeedItems(Long userId) {
        return readFeedItemRepository.countByUserId(userId);
    }

    // ========== Feed Cache Helper Methods ==========
    
    /**
     * Get cached feed content for pagination
     */
    @SuppressWarnings("unchecked")
    private List<ReadFeedItem> getCachedFeedContent(String cachePrefix, int page, int size) {
        try {
            Cache cache = cacheManager.getCache("feed_content");
            String key = cachePrefix + "_content_" + page + "_" + size;
            log.debug("🔍 Looking for feed cache key: {}", key);
            
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    log.debug("🔍 Found cached feed value of type: {}", value != null ? value.getClass().getSimpleName() : "null");
                    if (value instanceof List) {
                        List<ReadFeedItem> result = (List<ReadFeedItem>) value;
                        log.info("✅ CACHE HIT - Found {} cached feed items for key: {}", result.size(), key);
                        return result;
                    } else {
                        log.warn("❌ Cache feed value is not a List, it's: {}", value != null ? value.getClass().getName() : "null");
                    }
                } else {
                    log.debug("❌ No cache wrapper found for feed key: {}", key);
                }
            } else {
                log.warn("❌ Cache 'feed_content' not found");
            }
        } catch (Exception e) {
            log.warn("Failed to get cached feed content for {}: {}", cachePrefix, e.getMessage());
        }
        return null;
    }
    
    /**
     * Get cached feed total count
     */
    private Long getCachedFeedTotal(String cachePrefix) {
        try {
            Cache cache = cacheManager.getCache("feed_total");
            String key = cachePrefix + "_total";
            log.debug("🔍 Looking for feed total cache key: {}", key);
            
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    Object value = wrapper.get();
                    log.debug("🔍 Found cached feed total value of type: {}", value != null ? value.getClass().getSimpleName() : "null");
                    if (value instanceof Long) {
                        Long result = (Long) value;
                        log.info("✅ CACHE HIT - Found cached feed total: {} for key: {}", result, key);
                        return result;
                    } else if (value instanceof Number) {
                        Long result = ((Number) value).longValue();
                        log.info("✅ CACHE HIT - Found cached feed total (converted): {} for key: {}", result, key);
                        return result;
                    } else {
                        log.warn("❌ Cache feed total is not a Number, it's: {}", value != null ? value.getClass().getName() : "null");
                    }
                } else {
                    log.debug("❌ No cache wrapper found for feed total key: {}", key);
                }
            } else {
                log.warn("❌ Cache 'feed_total' not found");
            }
        } catch (Exception e) {
            log.warn("Failed to get cached feed total for {}: {}", cachePrefix, e.getMessage());
        }
        return null;
    }
    
    /**
     * Cache feed content for pagination
     */
    private void cacheFeedContent(String cachePrefix, int page, int size, List<ReadFeedItem> content) {
        try {
            Cache cache = cacheManager.getCache("feed_content");
            if (cache != null) {
                String key = cachePrefix + "_content_" + page + "_" + size;
                cache.put(key, content);
                log.debug("💾 Cached feed content: {} items for key {}", content.size(), key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache feed content for {}: {}", cachePrefix, e.getMessage());
        }
    }
    
    /**
     * Cache feed total count
     */
    private void cacheFeedTotal(String cachePrefix, Long total) {
        try {
            Cache cache = cacheManager.getCache("feed_total");
            if (cache != null) {
                String key = cachePrefix + "_total";
                cache.put(key, total);
                log.debug("💾 Cached feed total: {} for key {}", total, key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache feed total for {}: {}", cachePrefix, e.getMessage());
        }
    }
}