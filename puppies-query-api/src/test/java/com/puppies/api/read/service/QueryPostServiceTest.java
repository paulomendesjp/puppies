package com.puppies.api.read.service;

import com.puppies.api.read.model.ReadPost;
import com.puppies.api.read.repository.ReadPostRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryPostService.
 * 
 * Tests read-only operations from read store, caching strategies,
 * and performance optimizations for query-side of CQRS.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueryPostService Tests")
class QueryPostServiceTest {

    @Mock
    private ReadPostRepository readPostRepository;
    
    @Mock
    private CacheManager cacheManager;
    
    @Mock
    private Cache cache;

    @InjectMocks
    private QueryPostService queryPostService;

    private List<ReadPost> testPosts;
    private ReadPost testPost;

    @BeforeEach
    void setUp() {
        testPost = ReadPost.builder()
                .id(1L)
                .authorId(1L)
                .authorName("John Doe")
                .content("Test post content")
                .imageUrl("http://example.com/image.jpg")
                .likeCount(10L)
                .commentCount(5L)
                .viewCount(100L)
                .popularityScore(85.5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ReadPost testPost2 = ReadPost.builder()
                .id(2L)
                .authorId(2L)
                .authorName("Jane Smith")
                .content("Another test post")
                .imageUrl("http://example.com/image2.jpg")
                .likeCount(20L)
                .commentCount(8L)
                .viewCount(150L)
                .popularityScore(92.0)
                .createdAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        testPosts = Arrays.asList(testPost, testPost2);
    }

    @Test
    @DisplayName("Should return posts from database when cache miss")
    void getAllPosts_WhenCacheMiss_ShouldLoadFromDatabase() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> expectedPage = new PageImpl<>(testPosts, pageable, testPosts.size());
        
        when(cacheManager.getCache("posts")).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null); // Cache miss
        when(readPostRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.getAllPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactlyElementsOf(testPosts);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(readPostRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    @DisplayName("Should return cached posts when cache hit")
    void getAllPosts_WhenCacheHit_ShouldReturnCachedData() {
        // Given
        int page = 0, size = 10;
        
        when(cacheManager.getCache("posts")).thenReturn(cache);
        when(cache.get("posts_content_0_10")).thenReturn(new Cache.ValueWrapper() {
            @Override
            public Object get() { return testPosts; }
        });
        when(cache.get("posts_total")).thenReturn(new Cache.ValueWrapper() {
            @Override
            public Object get() { return 2L; }
        });

        // When
        Page<ReadPost> result = queryPostService.getAllPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactlyElementsOf(testPosts);
        assertThat(result.getTotalElements()).isEqualTo(2);

        // Verify database was not called
        verify(readPostRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("Should get trending posts ordered by popularity score")
    void getTrendingPosts_ShouldReturnPostsOrderedByPopularity() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> expectedPage = new PageImpl<>(testPosts, pageable, testPosts.size());
        
        when(readPostRepository.findAllByOrderByPopularityScoreDesc(pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.getTrendingPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getPopularityScore())
                .isGreaterThanOrEqualTo(result.getContent().get(1).getPopularityScore());

        verify(readPostRepository).findAllByOrderByPopularityScoreDesc(pageable);
    }

    @Test
    @DisplayName("Should search posts by content")
    void searchPosts_ShouldReturnMatchingPosts() {
        // Given
        String searchTerm = "test";
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> expectedPage = new PageImpl<>(List.of(testPost), pageable, 1);
        
        when(readPostRepository.searchByContent(searchTerm, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.searchPosts(searchTerm, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testPost);

        verify(readPostRepository).searchByContent(searchTerm, pageable);
    }

    @Test
    @DisplayName("Should get post by ID from repository")
    void getPostById_WhenPostExists_ShouldReturnPost() {
        // Given
        Long postId = 1L;
        when(readPostRepository.findById(postId)).thenReturn(Optional.of(testPost));

        // When
        Optional<ReadPost> result = queryPostService.getPostById(postId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testPost);

        verify(readPostRepository).findById(postId);
    }

    @Test
    @DisplayName("Should return empty when post not found")
    void getPostById_WhenPostNotExists_ShouldReturnEmpty() {
        // Given
        Long postId = 999L;
        when(readPostRepository.findById(postId)).thenReturn(Optional.empty());

        // When
        Optional<ReadPost> result = queryPostService.getPostById(postId);

        // Then
        assertThat(result).isEmpty();

        verify(readPostRepository).findById(postId);
    }

    @Test
    @DisplayName("Should get posts by author ID")
    void getPostsByAuthorId_ShouldReturnAuthorPosts() {
        // Given
        Long authorId = 1L;
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> expectedPage = new PageImpl<>(List.of(testPost), pageable, 1);
        
        when(readPostRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.getPostsByAuthor(authorId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthorId()).isEqualTo(authorId);

        verify(readPostRepository).findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
    }

    @Test
    @DisplayName("Should get popular posts ordered by like count")
    void getPopularPosts_ShouldReturnPostsOrderedByLikes() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> expectedPage = new PageImpl<>(List.of(testPost), pageable, 1);
        
        when(readPostRepository.findAllByOrderByLikeCountDesc(pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.getPopularPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLikeCount()).isGreaterThan(0L);

        verify(readPostRepository).findAllByOrderByLikeCountDesc(pageable);
    }

    @Test
    @DisplayName("Should handle empty search results gracefully")
    void searchPosts_WithNoMatches_ShouldReturnEmptyPage() {
        // Given
        String searchTerm = "nonexistent";
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(readPostRepository.searchByContent(searchTerm, pageable))
                .thenReturn(emptyPage);

        // When
        Page<ReadPost> result = queryPostService.searchPosts(searchTerm, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(readPostRepository).searchByContent(searchTerm, pageable);
    }

    @Test
    @DisplayName("Should handle large page numbers gracefully")
    void getAllPosts_WithLargePageNumber_ShouldReturnEmptyPage() {
        // Given
        int page = 1000, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(cacheManager.getCache("posts")).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null); // Cache miss
        when(readPostRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(emptyPage);

        // When
        Page<ReadPost> result = queryPostService.getAllPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        verify(readPostRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    @DisplayName("Should handle zero or negative page sizes")
    void getAllPosts_WithInvalidPageSize_ShouldStillMakeRepositoryCall() {
        // Given
        int page = 0, size = 0;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(cacheManager.getCache("posts")).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null); // Cache miss
        when(readPostRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(emptyPage);

        // When
        Page<ReadPost> result = queryPostService.getAllPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        verify(readPostRepository).findAllByOrderByCreatedAtDesc(pageable);
    }
}