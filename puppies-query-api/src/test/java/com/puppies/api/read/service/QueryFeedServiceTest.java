package com.puppies.api.read.service;

import com.puppies.api.read.model.ReadFeedItem;
import com.puppies.api.read.repository.ReadFeedItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryFeedService.
 * 
 * Tests user feed functionality, liked posts, caching behavior,
 * and pagination for the query-side of CQRS architecture.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueryFeedService Tests")
class QueryFeedServiceTest {

    @Mock
    private ReadFeedItemRepository readFeedItemRepository;
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private Cache feedContentCache;
    
    @Mock
    private Cache feedTotalCache;

    @InjectMocks
    private QueryFeedService queryFeedService;

    private List<ReadFeedItem> testFeedItems;
    private ReadFeedItem testFeedItem1;
    private ReadFeedItem testFeedItem2;
    private ReadFeedItem testLikedFeedItem;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        
        testFeedItem1 = ReadFeedItem.builder()
                .id(1L)
                .userId(testUserId)
                .postId(1L)
                .postAuthorId(2L)
                .postAuthorName("John Doe")
                .postContent("First test post content")
                .postImageUrl("http://example.com/image1.jpg")
                .likeCount(10L)
                .isLikedByUser(false)
                .popularityScore(85.5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testFeedItem2 = ReadFeedItem.builder()
                .id(2L)
                .userId(testUserId)
                .postId(2L)
                .postAuthorId(3L)
                .postAuthorName("Jane Smith")
                .postContent("Second test post content")
                .postImageUrl("http://example.com/image2.jpg")
                .likeCount(25L)
                .isLikedByUser(false)
                .popularityScore(92.0)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        testLikedFeedItem = ReadFeedItem.builder()
                .id(3L)
                .userId(testUserId)
                .postId(3L)
                .postAuthorId(4L)
                .postAuthorName("Bob Wilson")
                .postContent("Liked post content")
                .postImageUrl("http://example.com/image3.jpg")
                .likeCount(15L)
                .isLikedByUser(true)
                .popularityScore(78.0)
                .createdAt(LocalDateTime.now().minusHours(2))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .build();

        // Order feed items by creation time (newest first) for chronological feed
        testFeedItems = Arrays.asList(testFeedItem1, testFeedItem2);
    }

    // ========== REQUIREMENT 5: Fetch user's feed (ordered by date, newest first) ==========

    @Test
    @DisplayName("Should get user feed from database when cache miss")
    void getUserFeed_WhenCacheMiss_ShouldLoadFromDatabase() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(testFeedItems, pageable, testFeedItems.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null); // Cache miss
        when(feedTotalCache.get(anyString())).thenReturn(null); // Cache miss
        when(readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactlyElementsOf(testFeedItems);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(readFeedItemRepository).findByUserIdOrderByCreatedAtDesc(testUserId, pageable);
        verify(feedContentCache).put(eq("user_feed_1_content_0_10"), eq(testFeedItems));
        verify(feedTotalCache).put(eq("user_feed_1_total"), eq(2L));
    }

    @Test
    @DisplayName("Should return cached user feed when cache hit")
    void getUserFeed_WhenCacheHit_ShouldReturnCachedData() {
        // Given
        int page = 0, size = 10;
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get("user_feed_1_content_0_10")).thenReturn(new Cache.ValueWrapper() {
            @Override
            public Object get() { return testFeedItems; }
        });
        when(feedTotalCache.get("user_feed_1_total")).thenReturn(new Cache.ValueWrapper() {
            @Override
            public Object get() { return 2L; }
        });

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactlyElementsOf(testFeedItems);
        assertThat(result.getTotalElements()).isEqualTo(2);

        // Verify database was not called
        verify(readFeedItemRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("Should handle empty user feed gracefully")
    void getUserFeed_WithNoFeedItems_ShouldReturnEmptyPage() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(emptyPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(readFeedItemRepository).findByUserIdOrderByCreatedAtDesc(testUserId, pageable);
    }

    @Test
    @DisplayName("Should verify user feed is ordered by creation date (newest first)")
    void getUserFeed_ShouldOrderByCreatedAtDesc() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(testFeedItems, pageable, testFeedItems.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        verify(readFeedItemRepository).findByUserIdOrderByCreatedAtDesc(testUserId, pageable);
    }

    // ========== REQUIREMENT 8: Fetch user's liked posts ==========

    @Test
    @DisplayName("Should get user liked posts from database when cache miss")
    void getUserLikedPosts_WhenCacheMiss_ShouldLoadFromDatabase() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<ReadFeedItem> likedPosts = List.of(testLikedFeedItem);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(likedPosts, pageable, likedPosts.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null); // Cache miss
        when(feedTotalCache.get(anyString())).thenReturn(null); // Cache miss
        when(readFeedItemRepository.findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserLikedPosts(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testLikedFeedItem);
        assertThat(result.getContent().get(0).getIsLikedByUser()).isTrue();
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(readFeedItemRepository).findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(testUserId, pageable);
        verify(feedContentCache).put(eq("user_liked_posts_1_content_0_10"), eq(likedPosts));
        verify(feedTotalCache).put(eq("user_liked_posts_1_total"), eq(1L));
    }

    @Test
    @DisplayName("Should return cached liked posts when cache hit")
    void getUserLikedPosts_WhenCacheHit_ShouldReturnCachedData() {
        // Given
        int page = 0, size = 10;
        List<ReadFeedItem> likedPosts = List.of(testLikedFeedItem);
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get("user_liked_posts_1_content_0_10")).thenReturn(new Cache.ValueWrapper() {
            @Override
            public Object get() { return likedPosts; }
        });
        when(feedTotalCache.get("user_liked_posts_1_total")).thenReturn(new Cache.ValueWrapper() {
            @Override
            public Object get() { return 1L; }
        });

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserLikedPosts(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent()).containsExactlyElementsOf(likedPosts);
        assertThat(result.getTotalElements()).isEqualTo(1);

        // Verify database was not called
        verify(readFeedItemRepository, never()).findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("Should handle empty liked posts gracefully")
    void getUserLikedPosts_WithNoLikedPosts_ShouldReturnEmptyPage() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(emptyPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserLikedPosts(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(readFeedItemRepository).findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(testUserId, pageable);
    }

    @Test
    @DisplayName("Should verify liked posts are ordered by creation date (newest first)")
    void getUserLikedPosts_ShouldOrderByCreatedAtDesc() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<ReadFeedItem> likedPosts = List.of(testLikedFeedItem);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(likedPosts, pageable, likedPosts.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        queryFeedService.getUserLikedPosts(testUserId, page, size);

        // Then
        verify(readFeedItemRepository).findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(testUserId, pageable);
    }

    // ========== Additional Feed Functionality Tests ==========

    @Test
    @DisplayName("Should get user feed by popularity")
    void getUserFeedByPopularity_ShouldOrderByPopularityScore() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<ReadFeedItem> popularityOrderedItems = Arrays.asList(testFeedItem2, testFeedItem1); // Higher score first
        Page<ReadFeedItem> expectedPage = new PageImpl<>(popularityOrderedItems, pageable, popularityOrderedItems.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findByUserIdOrderByPopularityScoreDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeedByPopularity(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getPopularityScore())
                .isGreaterThanOrEqualTo(result.getContent().get(1).getPopularityScore());

        verify(readFeedItemRepository).findByUserIdOrderByPopularityScoreDesc(testUserId, pageable);
    }

    @Test
    @DisplayName("Should get trending feed")
    void getTrendingFeed_ShouldReturnGlobalTrendingPosts() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(testFeedItems, pageable, testFeedItems.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findAllByOrderByPopularityScoreDesc(pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getTrendingFeed(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(readFeedItemRepository).findAllByOrderByPopularityScoreDesc(pageable);
    }

    @Test
    @DisplayName("Should get recent engaging posts")
    void getRecentEngagingPosts_ShouldReturnEngagingContent() {
        // Given
        Long minLikes = 5L;
        int limit = 10;
        Pageable pageable = PageRequest.of(0, limit);
        List<ReadFeedItem> engagingPosts = List.of(testFeedItem1, testFeedItem2);
        
        when(readFeedItemRepository.findRecentEngagingPosts(testUserId, minLikes, pageable))
                .thenReturn(engagingPosts);

        // When
        List<ReadFeedItem> result = queryFeedService.getRecentEngagingPosts(testUserId, minLikes, limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(engagingPosts);

        verify(readFeedItemRepository).findRecentEngagingPosts(testUserId, minLikes, pageable);
    }

    @Test
    @DisplayName("Should get author posts in feed")
    void getAuthorPostsInFeed_ShouldReturnPostsFromSpecificAuthor() {
        // Given
        Long authorId = 2L;
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        List<ReadFeedItem> authorPosts = List.of(testFeedItem1);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(authorPosts, pageable, authorPosts.size());
        
        when(readFeedItemRepository.findByUserIdAndPostAuthorIdOrderByCreatedAtDesc(testUserId, authorId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getAuthorPostsInFeed(testUserId, authorId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPostAuthorId()).isEqualTo(authorId);

        verify(readFeedItemRepository).findByUserIdAndPostAuthorIdOrderByCreatedAtDesc(testUserId, authorId, pageable);
    }

    @Test
    @DisplayName("Should count user feed items")
    void countUserFeedItems_ShouldReturnCorrectCount() {
        // Given
        Long expectedCount = 5L;
        when(readFeedItemRepository.countByUserId(testUserId)).thenReturn(expectedCount);

        // When
        Long result = queryFeedService.countUserFeedItems(testUserId);

        // Then
        assertThat(result).isEqualTo(expectedCount);
        verify(readFeedItemRepository).countByUserId(testUserId);
    }

    @Test
    @DisplayName("Should get discovery feed")
    void getDiscoveryFeed_ShouldReturnHighQualityContent() {
        // Given
        Double minScore = 80.0;
        int limit = 10;
        Pageable pageable = PageRequest.of(0, limit);
        List<ReadFeedItem> discoveryPosts = List.of(testFeedItem1, testFeedItem2);
        
        when(readFeedItemRepository.findDiscoveryFeed(minScore, pageable))
                .thenReturn(discoveryPosts);

        // When
        List<ReadFeedItem> result = queryFeedService.getDiscoveryFeed(minScore, limit);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(discoveryPosts);

        verify(readFeedItemRepository).findDiscoveryFeed(minScore, pageable);
    }

    // ========== Cache Edge Cases ==========

    @Test
    @DisplayName("Should handle cache manager returning null cache")
    void getUserFeed_WhenCacheManagerReturnsNull_ShouldStillWork() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(testFeedItems, pageable, testFeedItems.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(null);
        when(cacheManager.getCache("feed_total")).thenReturn(null);
        when(readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(readFeedItemRepository).findByUserIdOrderByCreatedAtDesc(testUserId, pageable);
    }

    @Test
    @DisplayName("Should handle cache exception gracefully")
    void getUserFeed_WhenCacheThrowsException_ShouldFallbackToDatabase() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(testFeedItems, pageable, testFeedItems.size());
        
        when(cacheManager.getCache("feed_content")).thenThrow(new RuntimeException("Cache error"));
        when(readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        verify(readFeedItemRepository).findByUserIdOrderByCreatedAtDesc(testUserId, pageable);
    }

    // ========== Pagination Tests ==========

    @Test
    @DisplayName("Should handle large page numbers gracefully")
    void getUserFeed_WithLargePageNumber_ShouldReturnEmptyPage() {
        // Given
        int page = 1000, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(emptyPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle different page sizes")
    void getUserFeed_WithDifferentPageSizes_ShouldWork() {
        // Given
        int page = 0, size = 5;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadFeedItem> expectedPage = new PageImpl<>(testFeedItems.subList(0, 1), pageable, testFeedItems.size());
        
        when(cacheManager.getCache("feed_content")).thenReturn(feedContentCache);
        when(cacheManager.getCache("feed_total")).thenReturn(feedTotalCache);
        when(feedContentCache.get(anyString())).thenReturn(null);
        when(feedTotalCache.get(anyString())).thenReturn(null);
        when(readFeedItemRepository.findByUserIdOrderByCreatedAtDesc(testUserId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadFeedItem> result = queryFeedService.getUserFeed(testUserId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(readFeedItemRepository).findByUserIdOrderByCreatedAtDesc(testUserId, pageable);
    }
}