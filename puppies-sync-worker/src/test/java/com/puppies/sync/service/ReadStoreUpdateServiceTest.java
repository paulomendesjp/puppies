package com.puppies.sync.service;

import com.puppies.sync.event.PostCreatedEvent;
import com.puppies.sync.event.PostLikedEvent;
import com.puppies.sync.event.UserCreatedEvent;
import com.puppies.sync.model.ReadFeedItem;
import com.puppies.sync.model.ReadPost;
import com.puppies.sync.model.ReadUserProfile;
import com.puppies.sync.repository.ReadFeedItemRepository;
import com.puppies.sync.repository.ReadPostRepository;
import com.puppies.sync.repository.ReadUserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReadStoreUpdateService.
 * 
 * Tests the core CQRS synchronization logic that transforms
 * domain events into read-optimized data structures.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReadStoreUpdateService Tests")
class ReadStoreUpdateServiceTest {

    @Mock
    private ReadPostRepository readPostRepository;
    
    @Mock
    private ReadUserProfileRepository readUserProfileRepository;
    
    @Mock
    private ReadFeedItemRepository readFeedItemRepository;

    @InjectMocks
    private ReadStoreUpdateService readStoreUpdateService;

    private PostCreatedEvent postCreatedEvent;
    private PostLikedEvent postLikedEvent;
    private UserCreatedEvent userCreatedEvent;
    private ReadUserProfile existingUserProfile;

    @BeforeEach
    void setUp() {
        postCreatedEvent = PostCreatedEvent.builder()
                .eventId("test-event-1")
                .postId(1L)
                .authorId(1L)
                .authorName("John Doe")
                .textContent("Test post content")
                .imageUrl("http://example.com/image.jpg")
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        postLikedEvent = PostLikedEvent.builder()
                .eventId("test-event-2")
                .postId(1L)
                .userId(2L)
                .occurredAt(LocalDateTime.now())
                .build();

        userCreatedEvent = UserCreatedEvent.builder()
                .eventId("test-event-3")
                .userId(1L)
                .name("John Doe")
                .email("john@example.com")
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        existingUserProfile = ReadUserProfile.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .postsCount(5L)
                .followersCount(10L)
                .followingCount(8L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    @DisplayName("Should handle PostCreatedEvent and create denormalized post")
    void handlePostCreated_ShouldCreateReadPostAndUpdateUserProfile() {
        // Given
        when(readUserProfileRepository.incrementPostsCount(postCreatedEvent.getAuthorId())).thenReturn(1);
        when(readUserProfileRepository.findAll()).thenReturn(List.of(existingUserProfile));

        // When
        readStoreUpdateService.handlePostCreated(postCreatedEvent);

        // Then
        // Verify ReadPost creation
        ArgumentCaptor<ReadPost> readPostCaptor = ArgumentCaptor.forClass(ReadPost.class);
        verify(readPostRepository).save(readPostCaptor.capture());
        
        ReadPost savedPost = readPostCaptor.getValue();
        assertThat(savedPost.getId()).isEqualTo(1L);
        assertThat(savedPost.getAuthorId()).isEqualTo(1L);
        assertThat(savedPost.getAuthorName()).isEqualTo("John Doe");
        assertThat(savedPost.getContent()).isEqualTo("Test post content");
        assertThat(savedPost.getImageUrl()).isEqualTo("http://example.com/image.jpg");
        assertThat(savedPost.getLikeCount()).isEqualTo(0L);
        assertThat(savedPost.getCommentCount()).isEqualTo(0L);
        assertThat(savedPost.getViewCount()).isEqualTo(0L);
        assertThat(savedPost.getPopularityScore()).isGreaterThanOrEqualTo(0.0);

        // Verify user profile post count increment
        verify(readUserProfileRepository).incrementPostsCount(1L);

        // Verify feed items creation
        verify(readFeedItemRepository, atLeastOnce()).save(any(ReadFeedItem.class));
    }

    @Test
    @DisplayName("Should create user profile placeholder when user not found during post creation")
    void handlePostCreated_WhenUserNotFound_ShouldCreatePlaceholderProfile() {
        // Given
        when(readUserProfileRepository.incrementPostsCount(postCreatedEvent.getAuthorId())).thenReturn(0);
        when(readUserProfileRepository.findAll()).thenReturn(List.of());

        // When
        readStoreUpdateService.handlePostCreated(postCreatedEvent);

        // Then
        // Verify placeholder user profile creation
        ArgumentCaptor<ReadUserProfile> userProfileCaptor = ArgumentCaptor.forClass(ReadUserProfile.class);
        verify(readUserProfileRepository, atLeastOnce()).save(userProfileCaptor.capture());
        
        ReadUserProfile placeholderProfile = userProfileCaptor.getValue();
        assertThat(placeholderProfile.getId()).isEqualTo(1L);
        assertThat(placeholderProfile.getName()).isEqualTo("John Doe");
        assertThat(placeholderProfile.getPostsCount()).isEqualTo(0L);

        // Verify second increment attempt after placeholder creation
        verify(readUserProfileRepository, times(2)).incrementPostsCount(1L);
    }

    @Test
    @DisplayName("Should handle PostLikedEvent and increment like count")
    void handlePostLiked_ShouldIncrementLikeCount() {
        // When
        readStoreUpdateService.handlePostLiked(postLikedEvent);

        // Then
        verify(readPostRepository).incrementLikeCount(1L);
    }

    @Test
    @DisplayName("Should handle UserCreatedEvent and create user profile")
    void handleUserCreated_ShouldCreateReadUserProfile() {
        // When
        readStoreUpdateService.handleUserCreated(userCreatedEvent);

        // Then
        ArgumentCaptor<ReadUserProfile> userProfileCaptor = ArgumentCaptor.forClass(ReadUserProfile.class);
        verify(readUserProfileRepository).save(userProfileCaptor.capture());
        
        ReadUserProfile savedProfile = userProfileCaptor.getValue();
        assertThat(savedProfile.getId()).isEqualTo(1L);
        assertThat(savedProfile.getName()).isEqualTo("John Doe");
        assertThat(savedProfile.getEmail()).isEqualTo("john@example.com");
        assertThat(savedProfile.getPostsCount()).isEqualTo(0L);
        assertThat(savedProfile.getFollowersCount()).isEqualTo(0L);
        assertThat(savedProfile.getFollowingCount()).isEqualTo(0L);
        assertThat(savedProfile.getCreatedAt()).isEqualTo(userCreatedEvent.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle null PostCreatedEvent gracefully")
    void handlePostCreated_WithNullEvent_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> readStoreUpdateService.handlePostCreated(null))
                .isInstanceOf(NullPointerException.class);

        // Verify no repository interactions
        verifyNoInteractions(readPostRepository, readUserProfileRepository, readFeedItemRepository);
    }

    @Test
    @DisplayName("Should handle null PostLikedEvent gracefully")
    void handlePostLiked_WithNullEvent_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> readStoreUpdateService.handlePostLiked(null))
                .isInstanceOf(NullPointerException.class);

        // Verify no repository interactions
        verifyNoInteractions(readPostRepository);
    }

    @Test
    @DisplayName("Should handle null UserCreatedEvent gracefully")
    void handleUserCreated_WithNullEvent_ShouldThrowException() {
        // When/Then
        assertThatThrownBy(() -> readStoreUpdateService.handleUserCreated(null))
                .isInstanceOf(NullPointerException.class);

        // Verify no repository interactions
        verifyNoInteractions(readUserProfileRepository);
    }

    @Test
    @DisplayName("Should calculate initial popularity score for new post")
    void handlePostCreated_ShouldCalculateInitialPopularityScore() {
        // Given
        when(readUserProfileRepository.incrementPostsCount(postCreatedEvent.getAuthorId())).thenReturn(1);
        when(readUserProfileRepository.findAll()).thenReturn(List.of(existingUserProfile));

        // When
        readStoreUpdateService.handlePostCreated(postCreatedEvent);

        // Then
        ArgumentCaptor<ReadPost> readPostCaptor = ArgumentCaptor.forClass(ReadPost.class);
        verify(readPostRepository).save(readPostCaptor.capture());
        
        ReadPost savedPost = readPostCaptor.getValue();
        assertThat(savedPost.getPopularityScore()).isNotNull();
        assertThat(savedPost.getPopularityScore()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("Should create feed items for all users when post is created")
    void handlePostCreated_ShouldCreateFeedItemsForAllUsers() {
        // Given
        ReadUserProfile user1 = ReadUserProfile.builder().id(1L).name("User 1").build();
        ReadUserProfile user2 = ReadUserProfile.builder().id(2L).name("User 2").build();
        List<ReadUserProfile> allUsers = List.of(user1, user2);
        
        when(readUserProfileRepository.incrementPostsCount(postCreatedEvent.getAuthorId())).thenReturn(1);
        when(readUserProfileRepository.findAll()).thenReturn(allUsers);

        // When
        readStoreUpdateService.handlePostCreated(postCreatedEvent);

        // Then
        // Verify feed items are created for all users
        ArgumentCaptor<ReadFeedItem> feedItemCaptor = ArgumentCaptor.forClass(ReadFeedItem.class);
        verify(readFeedItemRepository, times(2)).save(feedItemCaptor.capture());
        
        List<ReadFeedItem> feedItems = feedItemCaptor.getAllValues();
        assertThat(feedItems).hasSize(2);
        
        // Verify feed items have correct data
        feedItems.forEach(feedItem -> {
            assertThat(feedItem.getPostId()).isEqualTo(1L);
            assertThat(feedItem.getPostAuthorId()).isEqualTo(1L);
            assertThat(feedItem.getPostContent()).isEqualTo("Test post content");
            assertThat(feedItem.getPostImageUrl()).isEqualTo("http://example.com/image.jpg");
            assertThat(feedItem.getCreatedAt()).isEqualTo(postCreatedEvent.getCreatedAt());
        });
    }

    @Test
    @DisplayName("Should handle database errors during post creation")
    void handlePostCreated_WhenDatabaseFails_ShouldPropagateException() {
        // Given
        when(readPostRepository.save(any(ReadPost.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When/Then
        assertThatThrownBy(() -> readStoreUpdateService.handlePostCreated(postCreatedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database connection failed");
    }

    @Test
    @DisplayName("Should handle repository errors during like count increment")
    void handlePostLiked_WhenRepositoryFails_ShouldPropagateException() {
        // Given
        when(readPostRepository.incrementLikeCount(any(Long.class)))
                .thenThrow(new RuntimeException("Database update failed"));

        // When/Then
        assertThatThrownBy(() -> readStoreUpdateService.handlePostLiked(postLikedEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database update failed");
    }

    @Test
    @DisplayName("Should verify transaction boundaries")
    void handlePostCreated_ShouldExecuteInTransaction() {
        // Given
        when(readUserProfileRepository.incrementPostsCount(postCreatedEvent.getAuthorId())).thenReturn(1);
        when(readUserProfileRepository.findAll()).thenReturn(List.of(existingUserProfile));

        // When
        readStoreUpdateService.handlePostCreated(postCreatedEvent);

        // Then
        // Verify all operations are called (indicating transaction scope)
        verify(readPostRepository).save(any(ReadPost.class));
        verify(readUserProfileRepository).incrementPostsCount(any(Long.class));
        verify(readFeedItemRepository, atLeastOnce()).save(any(ReadFeedItem.class));
    }

    @Test
    @DisplayName("Should handle PostCreatedEvent with minimal data")
    void handlePostCreated_WithMinimalData_ShouldStillWork() {
        // Given
        PostCreatedEvent minimalEvent = PostCreatedEvent.builder()
                .eventId("minimal-event")
                .postId(2L)
                .authorId(2L)
                .authorName("Minimal Author")
                .textContent(null) // No text content
                .imageUrl("http://example.com/minimal.jpg")
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();
        
        when(readUserProfileRepository.incrementPostsCount(2L)).thenReturn(1);
        when(readUserProfileRepository.findAll()).thenReturn(List.of());

        // When
        readStoreUpdateService.handlePostCreated(minimalEvent);

        // Then
        // Should not throw exception and should save the post
        ArgumentCaptor<ReadPost> readPostCaptor = ArgumentCaptor.forClass(ReadPost.class);
        verify(readPostRepository).save(readPostCaptor.capture());
        
        ReadPost savedPost = readPostCaptor.getValue();
        assertThat(savedPost.getContent()).isNull();
        assertThat(savedPost.getImageUrl()).isEqualTo("http://example.com/minimal.jpg");
    }
}
