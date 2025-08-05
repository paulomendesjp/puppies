package com.puppies.api.read.service;

import com.puppies.api.read.model.ReadPost;
import com.puppies.api.read.repository.ReadPostRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Query service for posts - read-only operations from read store
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueryPostService {

    private final ReadPostRepository readPostRepository;
    private final CacheManager cacheManager;

    /**
     * Get all posts with pagination
     */
    public Page<ReadPost> getAllPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        List<ReadPost> cachedContent = getCachedPostContent("posts", page, size);
        Long cachedTotal = getCachedPostTotal("posts");
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("📦 CACHE HIT - Using cached posts: page={}, size={}, total={}", page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("📦 CACHE MISS - Loading posts from DB: page={}, size={}", page, size);
        Page<ReadPost> result = readPostRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        // Cache the content and total separately
        cachePostContent("posts", page, size, result.getContent());
        cachePostTotal("posts", result.getTotalElements());
        
        log.info("📦 CACHE STORE - Loaded {} posts for page {}, total={}", result.getNumberOfElements(), page, result.getTotalElements());
        return result;
    }

    /**
     * Get trending posts
     */
    public Page<ReadPost> getTrendingPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        List<ReadPost> cachedContent = getCachedPostContent("trending_posts", page, size);
        Long cachedTotal = getCachedPostTotal("trending_posts");
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("🔥 CACHE HIT - Using cached trending posts: page={}, size={}, total={}", page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("🔥 CACHE MISS - Loading trending posts from DB: page={}, size={}", page, size);
        Page<ReadPost> result = readPostRepository.findAllByOrderByPopularityScoreDesc(pageable);
        
        // Cache the content and total separately
        cachePostContent("trending_posts", page, size, result.getContent());
        cachePostTotal("trending_posts", result.getTotalElements());
        
        log.info("🔥 CACHE STORE - Loaded {} trending posts for page {}, total={}", result.getNumberOfElements(), page, result.getTotalElements());
        return result;
    }

    /**
     * Get most liked posts
     */
    public Page<ReadPost> getPopularPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        List<ReadPost> cachedContent = getCachedPostContent("popular_posts", page, size);
        Long cachedTotal = getCachedPostTotal("popular_posts");
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("❤️ CACHE HIT - Using cached popular posts: page={}, size={}, total={}", page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("❤️ CACHE MISS - Loading popular posts from DB: page={}, size={}", page, size);
        Page<ReadPost> result = readPostRepository.findAllByOrderByLikeCountDesc(pageable);
        
        // Cache the content and total separately
        cachePostContent("popular_posts", page, size, result.getContent());
        cachePostTotal("popular_posts", result.getTotalElements());
        
        log.info("❤️ CACHE STORE - Loaded {} popular posts for page {}, total={}", result.getNumberOfElements(), page, result.getTotalElements());
        return result;
    }

    /**
     * Get posts by author
     */
    public Page<ReadPost> getPostsByAuthor(Long authorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        String cachePrefix = "author_posts_" + authorId;
        List<ReadPost> cachedContent = getCachedPostContent(cachePrefix, page, size);
        Long cachedTotal = getCachedPostTotal(cachePrefix);
        
        if (cachedContent != null && cachedTotal != null) {
            log.info("👤 CACHE HIT - Using cached author posts: authorId={}, page={}, size={}, total={}", authorId, page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info("👤 CACHE MISS - Loading posts by author from DB: authorId={}, page={}, size={}", authorId, page, size);
        Page<ReadPost> result = readPostRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
        
        // Cache the content and total separately
        cachePostContent(cachePrefix, page, size, result.getContent());
        cachePostTotal(cachePrefix, result.getTotalElements());
        
        log.info("👤 CACHE STORE - Loaded {} posts for author {}, total={}", result.getNumberOfElements(), authorId, result.getTotalElements());
        return result;
    }

    /**
     * Get post by ID
     */
    @Cacheable(value = "post", key = "#id")
    public Optional<ReadPost> getPostById(Long id) {
        log.info("📝 CACHE MISS - Loading post from DB: id={}", id);
        Optional<ReadPost> result = readPostRepository.findById(id);
        if (result.isPresent()) {
            log.info("📝 CACHE STORE - Loaded post: id={}, title={}", id, result.get().getContent().substring(0, Math.min(30, result.get().getContent().length())));
        } else {
            log.warn("📝 CACHE STORE - Post not found: id={}", id);
        }
        return result;
    }

    /**
     * Search posts by content
     */
    public Page<ReadPost> searchPosts(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return readPostRepository.searchByContent(searchTerm, pageable);
    }

    /**
     * Get recent trending posts (last 24 hours)
     */
    @Cacheable(value = "recent_trending", key = "#limit")
    public List<ReadPost> getRecentTrendingPosts(int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Pageable pageable = PageRequest.of(0, limit);
        return readPostRepository.findRecentTrendingPosts(since, pageable);
    }

    /**
     * Get high engagement posts
     */
    @Cacheable(value = "high_engagement", key = "#threshold + '_' + #limit")
    public List<ReadPost> getHighEngagementPosts(Long threshold, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return readPostRepository.findHighEngagementPosts(threshold, pageable);
    }

    /**
     * Count posts by author
     */
    @Cacheable(value = "author_post_count", key = "#authorId")
    public Long countPostsByAuthor(Long authorId) {
        return readPostRepository.countByAuthorId(authorId);
    }

    // ========== Cache Helper Methods ==========
    
    /**
     * Get cached post content for pagination
     */
    @SuppressWarnings("unchecked")
    private List<ReadPost> getCachedPostContent(String cachePrefix, int page, int size) {
        try {
            Cache cache = cacheManager.getCache("post_content");
            String key = cachePrefix + "_content_" + page + "_" + size;
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
                log.warn("❌ Cache 'post_content' not found");
            }
        } catch (Exception e) {
            log.warn("Failed to get cached content for {}: {}", cachePrefix, e.getMessage());
        }
        return null;
    }
    
    /**
     * Get cached total count
     */
    private Long getCachedPostTotal(String cachePrefix) {
        try {
            Cache cache = cacheManager.getCache("post_total");
            String key = cachePrefix + "_total";
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
                log.warn("❌ Cache 'post_total' not found");
            }
        } catch (Exception e) {
            log.warn("Failed to get cached total for {}: {}", cachePrefix, e.getMessage());
        }
        return null;
    }
    
    /**
     * Cache post content for pagination
     */
    private void cachePostContent(String cachePrefix, int page, int size, List<ReadPost> content) {
        try {
            Cache cache = cacheManager.getCache("post_content");
            if (cache != null) {
                String key = cachePrefix + "_content_" + page + "_" + size;
                cache.put(key, content);
                log.debug("💾 Cached content: {} items for key {}", content.size(), key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache content for {}: {}", cachePrefix, e.getMessage());
        }
    }
    
    /**
     * Cache total count
     */
    private void cachePostTotal(String cachePrefix, Long total) {
        try {
            Cache cache = cacheManager.getCache("post_total");
            if (cache != null) {
                String key = cachePrefix + "_total";
                cache.put(key, total);
                log.debug("💾 Cached total: {} for key {}", total, key);
            }
        } catch (Exception e) {
            log.warn("Failed to cache total for {}: {}", cachePrefix, e.getMessage());
        }
    }
}