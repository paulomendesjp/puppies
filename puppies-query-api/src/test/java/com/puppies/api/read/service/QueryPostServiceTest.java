package com.puppies.api.read.service;

import com.puppies.api.read.model.ReadPost;
import com.puppies.api.read.repository.ReadPostRepository;
import com.puppies.api.read.service.PostCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private PostCacheService postCacheService;

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

        // Order posts by popularity score descending for trending tests
        testPosts = Arrays.asList(testPost2, testPost);
    }

    @Test
    @DisplayName("Should return posts from database when cache miss")
    void getAllPosts_WhenCacheMiss_ShouldLoadFromDatabase() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> expectedPage = new PageImpl<>(testPosts, pageable, testPosts.size());
        
        // Mock cache miss
        when(postCacheService.getCachedPostContent("posts", page, size)).thenReturn(null);
        when(postCacheService.getCachedPostTotal("posts")).thenReturn(null);
        when(readPostRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.getAllPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactlyElementsOf(testPosts);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(readPostRepository).findAllByOrderByCreatedAtDesc(pageable);
        verify(postCacheService).cachePostContent("posts", page, size, testPosts);
        verify(postCacheService).cachePostTotal("posts", 2L);
    }

    @Test
    @DisplayName("Should return cached posts when cache hit")
    void getAllPosts_WhenCacheHit_ShouldReturnCachedData() {
        // Given
        int page = 0, size = 10;
        
        // Mock cache hit
        when(postCacheService.getCachedPostContent("posts", page, size)).thenReturn(testPosts);
        when(postCacheService.getCachedPostTotal("posts")).thenReturn(2L);

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
        
        // Mock cache miss
        when(postCacheService.getCachedPostContent("trending_posts", page, size)).thenReturn(null);
        when(postCacheService.getCachedPostTotal("trending_posts")).thenReturn(null);
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
        verify(postCacheService).cachePostContent("trending_posts", page, size, testPosts);
        verify(postCacheService).cachePostTotal("trending_posts", 2L);
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
        
        // Mock cache miss
        when(postCacheService.getCachedPostContent("author_posts_" + authorId, page, size)).thenReturn(null);
        when(postCacheService.getCachedPostTotal("author_posts_" + authorId)).thenReturn(null);
        when(readPostRepository.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.getPostsByAuthor(authorId, page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAuthorId()).isEqualTo(authorId);

        verify(readPostRepository).findByAuthorIdOrderByCreatedAtDesc(authorId, pageable);
        verify(postCacheService).cachePostContent("author_posts_" + authorId, page, size, List.of(testPost));
        verify(postCacheService).cachePostTotal("author_posts_" + authorId, 1L);
    }

    @Test
    @DisplayName("Should get popular posts ordered by like count")
    void getPopularPosts_ShouldReturnPostsOrderedByLikes() {
        // Given
        int page = 0, size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<ReadPost> expectedPage = new PageImpl<>(List.of(testPost), pageable, 1);
        
        // Mock cache miss
        when(postCacheService.getCachedPostContent("popular_posts", page, size)).thenReturn(null);
        when(postCacheService.getCachedPostTotal("popular_posts")).thenReturn(null);
        when(readPostRepository.findAllByOrderByLikeCountDesc(pageable))
                .thenReturn(expectedPage);

        // When
        Page<ReadPost> result = queryPostService.getPopularPosts(page, size);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLikeCount()).isGreaterThan(0L);

        verify(readPostRepository).findAllByOrderByLikeCountDesc(pageable);
        verify(postCacheService).cachePostContent("popular_posts", page, size, List.of(testPost));
        verify(postCacheService).cachePostTotal("popular_posts", 1L);
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
        
        // Mock cache miss
        when(postCacheService.getCachedPostContent("posts", page, size)).thenReturn(null);
        when(postCacheService.getCachedPostTotal("posts")).thenReturn(null);
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
    @DisplayName("Should handle invalid page sizes by validating them")
    void getAllPosts_WithInvalidPageSize_ShouldValidatePageSize() {
        // Given
        int page = 0, size = 0;
        
        // When & Then - Should throw IllegalArgumentException for invalid page size
        assertThrows(IllegalArgumentException.class, () -> {
            queryPostService.getAllPosts(page, size);
        });
    }
}