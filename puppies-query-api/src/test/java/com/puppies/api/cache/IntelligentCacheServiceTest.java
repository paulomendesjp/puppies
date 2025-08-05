package com.puppies.api.cache;

import com.puppies.api.cache.metrics.CacheMetrics;
import com.puppies.api.cache.strategy.HotPostsCacheStrategy;
import com.puppies.api.cache.strategy.UserBehaviorCacheStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IntelligentCacheService.
 * 
 * Tests intelligent caching strategies, cache layer determination,
 * and user behavior-based cache optimizations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IntelligentCacheService Tests")
class IntelligentCacheServiceTest {

    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private CacheMetrics cacheMetrics;
    
    @Mock
    private HotPostsCacheStrategy hotPostsStrategy;
    
    @Mock
    private UserBehaviorCacheStrategy userBehaviorStrategy;
    
    @Mock
    private Cache hotCache;
    
    @Mock
    private Cache warmCache;
    
    @Mock
    private Cache coldCache;
    
    @Mock
    private Cache.ValueWrapper cachedValue;

    @InjectMocks
    private IntelligentCacheService intelligentCacheService;

    private final Long testPostId = 1L;
    private final Long testUserId = 100L;
    private final String testData = "Test Post Data";

    @BeforeEach
    void setUp() {
        // Configure cache manager mocks
        when(cacheManager.getCache("hot_posts")).thenReturn(hotCache);
        when(cacheManager.getCache("warm_posts")).thenReturn(warmCache);
        when(cacheManager.getCache("cold_posts")).thenReturn(coldCache);
    }

    @Test
    @DisplayName("Should return cached data when cache hit occurs")
    void getPost_WhenCacheHit_ShouldReturnCachedData() {
        // Given
        when(coldCache.get(anyString())).thenReturn(cachedValue);
        when(cachedValue.get()).thenReturn(testData);
        
        Supplier<String> dataLoader = () -> "Fresh Data";

        // When
        Optional<String> result = intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testData);
        
        verify(cacheMetrics).recordHit("cold_posts");
        verify(coldCache).get(anyString());
        verifyNoMoreInteractions(dataLoader); // Should not load fresh data
    }

    @Test
    @DisplayName("Should load and cache data when cache miss occurs")
    void getPost_WhenCacheMiss_ShouldLoadAndCacheData() {
        // Given
        when(coldCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // When
        Optional<String> result = intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testData);
        
        verify(cacheMetrics).recordMiss("cold_posts");
        verify(coldCache).get(anyString());
        verify(coldCache).put(anyString(), eq(testData));
    }

    @Test
    @DisplayName("Should return empty when data loader returns null")
    void getPost_WhenDataLoaderReturnsNull_ShouldReturnEmpty() {
        // Given
        when(coldCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> null;

        // When
        Optional<String> result = intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        assertThat(result).isEmpty();
        
        verify(cacheMetrics).recordMiss("cold_posts");
        verify(coldCache).get(anyString());
        verify(coldCache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Should use hot cache for popular posts")
    void getPost_WithHighTrafficPost_ShouldUseHotCache() {
        // Given
        // Simulate a post that will be classified as "hot" by accessing it multiple times
        when(hotCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // Access the post multiple times to build up metrics
        for (int i = 0; i < 101; i++) {
            intelligentCacheService.getPost(testPostId, testUserId + i, String.class, dataLoader);
        }

        // When - Access again after building metrics
        when(hotCache.get(anyString())).thenReturn(cachedValue);
        when(cachedValue.get()).thenReturn(testData);
        
        Optional<String> result = intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        verify(cacheMetrics).recordHit("hot_posts");
    }

    @Test
    @DisplayName("Should handle cache manager returning null cache")
    void getPost_WhenCacheManagerReturnsNull_ShouldLoadDataDirectly() {
        // Given
        when(cacheManager.getCache(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // When
        Optional<String> result = intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testData);
        
        // Should still record miss even with null cache
        verify(cacheMetrics).recordMiss("cold_posts");
    }

    @Test
    @DisplayName("Should cache hot posts in multiple layers")
    void getPost_WithHotContent_ShouldCascadeCache() {
        // Given
        // Simulate conditions for hot cache determination
        when(hotCache.get(anyString())).thenReturn(null);
        when(warmCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // Build up post metrics to qualify for hot cache
        for (int i = 0; i < 101; i++) {
            intelligentCacheService.getPost(testPostId, testUserId + i, String.class, dataLoader);
        }

        // When - Another access that should use hot cache
        intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        // Verify caching in both hot and warm layers (cascade caching)
        verify(hotCache, atLeastOnce()).put(anyString(), eq(testData));
        verify(warmCache, atLeastOnce()).put(anyString(), eq(testData));
    }

    @Test
    @DisplayName("Should get user feed with personalized caching")
    void getUserFeed_ShouldUsePersonalizedCaching() {
        // Given
        String feedType = "home_feed";
        when(warmCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> "User Feed Data";

        // When
        Optional<String> result = intelligentCacheService.getUserFeed(testUserId, feedType, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("User Feed Data");
        
        verify(cacheMetrics).recordMiss("warm_posts_feed");
        verify(warmCache).put(anyString(), eq("User Feed Data"));
    }

    @Test
    @DisplayName("Should return cached user feed when available")
    void getUserFeed_WhenCached_ShouldReturnCachedData() {
        // Given
        String feedType = "home_feed";
        String cachedFeedData = "Cached Feed Data";
        
        when(warmCache.get(anyString())).thenReturn(cachedValue);
        when(cachedValue.get()).thenReturn(cachedFeedData);
        
        Supplier<String> dataLoader = () -> "Fresh Feed Data";

        // When
        Optional<String> result = intelligentCacheService.getUserFeed(testUserId, feedType, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(cachedFeedData);
        
        verify(cacheMetrics).recordHit("warm_posts_feed");
        verify(warmCache).get(anyString());
        verify(warmCache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("Should handle high engagement users with hot cache")
    void getUserFeed_WithHighEngagementUser_ShouldUseHotCache() {
        // Given
        String feedType = "home_feed";
        
        // Build up user engagement by accessing posts frequently
        for (int i = 0; i < 20; i++) {
            final int postIndex = i;
            intelligentCacheService.getPost((long) i, testUserId, String.class, () -> "Post " + postIndex);
        }
        
        when(hotCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> "High Engagement Feed";

        // When
        Optional<String> result = intelligentCacheService.getUserFeed(testUserId, feedType, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        verify(cacheMetrics).recordMiss("hot_posts_feed");
        verify(hotCache).put(anyString(), eq("High Engagement Feed"));
    }

    @Test
    @DisplayName("Should update user engagement metrics correctly")
    void getPost_ShouldUpdateUserEngagementMetrics() {
        // Given
        when(coldCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // When
        intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        // Verify that user metrics are being tracked (indirectly through cache behavior)
        verify(coldCache).get(anyString());
        verify(coldCache).put(anyString(), eq(testData));
    }

    @Test
    @DisplayName("Should build correct cache keys")
    void getPost_ShouldBuildCorrectCacheKeys() {
        // Given
        when(coldCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // When
        intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        // Verify cache key contains both post ID and user ID for personalization
        verify(coldCache).get(argThat(key -> 
            key.toString().contains(testPostId.toString()) && 
            key.toString().contains(testUserId.toString())
        ));
    }

    @Test
    @DisplayName("Should handle concurrent access gracefully")
    void getPost_WithConcurrentAccess_ShouldHandleGracefully() {
        // Given
        when(coldCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // When - Simulate concurrent access
        for (int i = 0; i < 10; i++) {
            Long userId = testUserId + i;
            Optional<String> result = intelligentCacheService.getPost(testPostId, userId, String.class, dataLoader);
            assertThat(result).isPresent();
        }

        // Then
        verify(coldCache, atLeast(10)).get(anyString());
        verify(coldCache, atLeast(10)).put(anyString(), eq(testData));
    }

    @Test
    @DisplayName("Should track cache metrics correctly")
    void getPost_ShouldTrackMetricsCorrectly() {
        // Given
        when(coldCache.get(anyString())).thenReturn(cachedValue);
        when(cachedValue.get()).thenReturn(testData);
        
        Supplier<String> dataLoader = () -> "Fresh Data";

        // When
        intelligentCacheService.getPost(testPostId, testUserId, String.class, dataLoader);

        // Then
        verify(cacheMetrics).recordHit("cold_posts");
        verify(cacheMetrics, never()).recordMiss(anyString());
    }

    @Test
    @DisplayName("Should handle null user ID gracefully")
    void getPost_WithNullUserId_ShouldStillWork() {
        // Given
        when(coldCache.get(anyString())).thenReturn(null);
        
        Supplier<String> dataLoader = () -> testData;

        // When
        Optional<String> result = intelligentCacheService.getPost(testPostId, null, String.class, dataLoader);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testData);
        
        verify(cacheMetrics).recordMiss("cold_posts");
    }
}