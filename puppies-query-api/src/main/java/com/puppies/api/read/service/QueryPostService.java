package com.puppies.api.read.service;

import com.puppies.api.common.constants.QueryApiConstants;
import com.puppies.api.read.model.ReadPost;
import com.puppies.api.read.repository.ReadPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.function.Function;

/**
 * Query service for posts - read-only operations from read store
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QueryPostService {

    private final ReadPostRepository readPostRepository;
    private final PostCacheService postCacheService;

    /**
     * Get all posts with pagination
     */
    public Page<ReadPost> getAllPosts(int page, int size) {
        return getPostsWithCache(
            QueryApiConstants.CacheKeys.POSTS_PREFIX,
            page, 
            size,
            pageable -> readPostRepository.findAllByOrderByCreatedAtDesc(pageable),
            QueryApiConstants.LogMessages.CACHE_HIT_POSTS,
            QueryApiConstants.LogMessages.CACHE_MISS_POSTS,
            QueryApiConstants.LogMessages.CACHE_STORE_POSTS
        );
    }

    /**
     * Get trending posts
     */
    public Page<ReadPost> getTrendingPosts(int page, int size) {
        return getPostsWithCache(
            QueryApiConstants.CacheKeys.TRENDING_POSTS_PREFIX,
            page, 
            size,
            pageable -> readPostRepository.findAllByOrderByPopularityScoreDesc(pageable),
            QueryApiConstants.LogMessages.CACHE_HIT_TRENDING,
            QueryApiConstants.LogMessages.CACHE_MISS_TRENDING,
            QueryApiConstants.LogMessages.CACHE_STORE_TRENDING
        );
    }

    /**
     * Get most liked posts
     */
    public Page<ReadPost> getPopularPosts(int page, int size) {
        return getPostsWithCache(
            QueryApiConstants.CacheKeys.POPULAR_POSTS_PREFIX,
            page, 
            size,
            pageable -> readPostRepository.findAllByOrderByLikeCountDesc(pageable),
            QueryApiConstants.LogMessages.CACHE_HIT_POPULAR,
            QueryApiConstants.LogMessages.CACHE_MISS_POPULAR,
            QueryApiConstants.LogMessages.CACHE_STORE_POPULAR
        );
    }

    /**
     * Get posts by author
     */
    public Page<ReadPost> getPostsByAuthor(Long authorId, int page, int size) {
        String cachePrefix = QueryApiConstants.CacheKeys.AUTHOR_POSTS_PREFIX + authorId;
        return getPostsWithCache(
            cachePrefix,
            page, 
            size,
            pageable -> readPostRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable),
            QueryApiConstants.LogMessages.CACHE_HIT_AUTHOR,
            QueryApiConstants.LogMessages.CACHE_MISS_AUTHOR,
            QueryApiConstants.LogMessages.CACHE_STORE_AUTHOR
        );
    }

    /**
     * Get post by ID
     */
    @Cacheable(value = QueryApiConstants.CacheNames.POST, key = "#id")
    public Optional<ReadPost> getPostById(Long id) {
        log.info(QueryApiConstants.LogMessages.CACHE_MISS_POST, id);
        Optional<ReadPost> result = readPostRepository.findById(id);
        if (result.isPresent()) {
            log.info(QueryApiConstants.LogMessages.CACHE_STORE_POST, id, result.get().getContent().substring(0, Math.min(30, result.get().getContent().length())));
        } else {
            log.warn(QueryApiConstants.LogMessages.CACHE_STORE_POST_NOT_FOUND, id);
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
    @Cacheable(value = QueryApiConstants.CacheNames.RECENT_TRENDING, key = "#limit")
    public List<ReadPost> getRecentTrendingPosts(int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(QueryApiConstants.BusinessRules.CACHE_CLEANUP_HOURS);
        Pageable pageable = PageRequest.of(0, limit);
        return readPostRepository.findRecentTrendingPosts(since, pageable);
    }

    /**
     * Get high engagement posts
     */
    @Cacheable(value = QueryApiConstants.CacheNames.HIGH_ENGAGEMENT, key = "#threshold + '_' + #limit")
    public List<ReadPost> getHighEngagementPosts(Long threshold, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return readPostRepository.findHighEngagementPosts(threshold, pageable);
    }

    /**
     * Count posts by author
     */
    @Cacheable(value = QueryApiConstants.CacheNames.AUTHOR_POST_COUNT, key = "#authorId")
    public Long countPostsByAuthor(Long authorId) {
        return readPostRepository.countByAuthorId(authorId);
    }

    // ========== Template Method for Cache Pattern ==========
    
    /**
     * Template method for getting posts with cache pattern.
     * Reduces code duplication by centralizing the cache logic.
     */
    private Page<ReadPost> getPostsWithCache(
            String cachePrefix,
            int page,
            int size,
            Function<Pageable, Page<ReadPost>> dataLoader,
            String cacheHitLogMessage,
            String cacheMissLogMessage,
            String cacheStoreLogMessage) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        // Try cache first for the content
        List<ReadPost> cachedContent = postCacheService.getCachedPostContent(cachePrefix, page, size);
        Long cachedTotal = postCacheService.getCachedPostTotal(cachePrefix);
        
        if (cachedContent != null && cachedTotal != null) {
            log.info(cacheHitLogMessage, page, size, cachedTotal);
            return new PageImpl<>(cachedContent, pageable, cachedTotal);
        }
        
        // Cache miss - load from DB
        log.info(cacheMissLogMessage, page, size);
        Page<ReadPost> result = dataLoader.apply(pageable);
        
        // Cache the content and total separately
        postCacheService.cachePostContent(cachePrefix, page, size, result.getContent());
        postCacheService.cachePostTotal(cachePrefix, result.getTotalElements());
        
        log.info(cacheStoreLogMessage, result.getNumberOfElements(), page, result.getTotalElements());
        return result;
    }
}