package com.puppies.api.command.service;

import com.puppies.api.command.dto.CreatePostRequest;
import com.puppies.api.command.dto.CreatePostResponse;
import com.puppies.api.data.entity.Like;
import com.puppies.api.data.entity.Post;
import com.puppies.api.data.entity.User;
import com.puppies.api.data.repository.LikeRepository;
import com.puppies.api.data.repository.PostRepository;
import com.puppies.api.data.repository.UserRepository;
import com.puppies.api.event.EventPublisher;
import com.puppies.api.event.PostCreatedEvent;
import com.puppies.api.event.PostLikedEvent;
import com.puppies.api.event.PostUnlikedEvent;
import com.puppies.api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostCommandService.
 * 
 * Tests the business logic for post creation, likes/unlikes,
 * and event publishing in CQRS architecture.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostCommandService Tests")
class PostCommandServiceTest {

    @Mock
    private PostRepository postRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private LikeRepository likeRepository;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private MultipartFile imageFile;

    @InjectMocks
    private PostCommandService postCommandService;

    private User testUser;
    private Post testPost;
    private CreatePostRequest createPostRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .password("hashedPassword")
                .createdAt(LocalDateTime.now())
                .build();

        testPost = Post.builder()
                .id(1L)
                .author(testUser)
                .imageUrl("http://example.com/image.jpg")
                .textContent("Test post content")
                .createdAt(LocalDateTime.now())
                .build();

        createPostRequest = CreatePostRequest.builder()
                .textContent("Test post content")
                .build();
    }

    @Test
    @DisplayName("Should create post successfully with valid data and publish event")
    void createPost_WithValidData_ShouldReturnCreatedPostAndPublishEvent() {
        // Given
        String authorEmail = "john@example.com";
        String uploadedImageUrl = "http://example.com/uploaded-image.jpg";
        
        when(userRepository.findByEmail(authorEmail)).thenReturn(Optional.of(testUser));
        when(fileStorageService.uploadFile(imageFile)).thenReturn(uploadedImageUrl);
        when(postRepository.save(any(Post.class))).thenReturn(testPost);

        // When
        CreatePostResponse response = postCommandService.createPost(createPostRequest, imageFile, authorEmail);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getImageUrl()).isEqualTo("http://example.com/image.jpg");
        assertThat(response.getTextContent()).isEqualTo("Test post content");
        assertThat(response.getMessage()).isEqualTo("Post created successfully");

        // Verify repository interactions
        verify(userRepository).findByEmail(authorEmail);
        verify(fileStorageService).uploadFile(imageFile);
        verify(postRepository).save(any(Post.class));

        // Verify event publishing
        ArgumentCaptor<PostCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PostCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        PostCreatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getPostId()).isEqualTo(1L);
        assertThat(publishedEvent.getAuthorId()).isEqualTo(1L);
        assertThat(publishedEvent.getAuthorName()).isEqualTo("John Doe");
        assertThat(publishedEvent.getTextContent()).isEqualTo("Test post content");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void createPost_WithNonExistentUser_ShouldThrowResourceNotFoundException() {
        // Given
        String authorEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(authorEmail)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> postCommandService.createPost(createPostRequest, imageFile, authorEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: " + authorEmail);

        // Verify no file upload or post save occurred
        verify(fileStorageService, never()).uploadFile(any());
        verify(postRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should like post successfully and publish event")
    void likePost_WithValidData_ShouldCreateLikeAndPublishEvent() {
        // Given
        Long postId = 1L;
        String userEmail = "john@example.com";
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(likeRepository.existsByUserIdAndPostId(testUser.getId(), postId)).thenReturn(false);
        
        Like savedLike = Like.builder()
                .id(1L)
                .user(testUser)
                .post(testPost)
                .createdAt(LocalDateTime.now())
                .build();
        when(likeRepository.save(any(Like.class))).thenReturn(savedLike);

        // When
        postCommandService.likePost(postId, userEmail);

        // Then
        verify(postRepository).findById(postId);
        verify(userRepository).findByEmail(userEmail);
        verify(likeRepository).existsByUserIdAndPostId(testUser.getId(), postId);
        verify(likeRepository).save(any(Like.class));

        // Verify event publishing
        ArgumentCaptor<PostLikedEvent> eventCaptor = ArgumentCaptor.forClass(PostLikedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        PostLikedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getPostId()).isEqualTo(postId);
        assertThat(publishedEvent.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when liking non-existent post")
    void likePost_WithNonExistentPost_ShouldThrowResourceNotFoundException() {
        // Given
        Long postId = 999L;
        String userEmail = "john@example.com";
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> postCommandService.likePost(postId, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post not found: " + postId);

        verify(likeRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when user already liked the post")
    void likePost_WhenAlreadyLiked_ShouldThrowIllegalStateException() {
        // Given
        Long postId = 1L;
        String userEmail = "john@example.com";
        
        when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(likeRepository.existsByUserIdAndPostId(testUser.getId(), postId)).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> postCommandService.likePost(postId, userEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Post already liked by user");

        verify(likeRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should unlike post successfully and publish event")
    void unlikePost_WithValidData_ShouldDeleteLikeAndPublishEvent() {
        // Given
        Long postId = 1L;
        String userEmail = "john@example.com";
        
        when(postRepository.existsById(postId)).thenReturn(true);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(likeRepository.existsByUserIdAndPostId(testUser.getId(), postId)).thenReturn(true);

        // When
        postCommandService.unlikePost(postId, userEmail);

        // Then
        verify(postRepository).existsById(postId);
        verify(userRepository).findByEmail(userEmail);
        verify(likeRepository).existsByUserIdAndPostId(testUser.getId(), postId);
        verify(likeRepository).deleteByUserIdAndPostId(testUser.getId(), postId);

        // Verify event publishing
        ArgumentCaptor<PostUnlikedEvent> eventCaptor = ArgumentCaptor.forClass(PostUnlikedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        
        PostUnlikedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getPostId()).isEqualTo(postId);
        assertThat(publishedEvent.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user hasn't liked the post")
    void unlikePost_WhenNotLiked_ShouldThrowResourceNotFoundException() {
        // Given
        Long postId = 1L;
        String userEmail = "john@example.com";
        
        when(postRepository.existsById(postId)).thenReturn(true);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(likeRepository.existsByUserIdAndPostId(testUser.getId(), postId)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> postCommandService.unlikePost(postId, userEmail))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Like not found for post: " + postId);

        verify(likeRepository, never()).deleteByUserIdAndPostId(anyLong(), anyLong());
        verify(eventPublisher, never()).publishEvent(any());
    }
}