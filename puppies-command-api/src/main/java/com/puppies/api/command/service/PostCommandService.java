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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling post-related command operations.
 * Implements the Command side of CQRS for post management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostCommandService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final FileStorageService fileStorageService;
    private final EventPublisher eventPublisher;

    /**
     * Create a new post with image upload.
     * 
     * @param request The post creation request
     * @param image The image file to upload
     * @param authorEmail The email of the authenticated user
     * @return Response with created post information
     */
    @Transactional
    @CacheEvict(value = {"feed", "posts"}, allEntries = true)
    public CreatePostResponse createPost(CreatePostRequest request, MultipartFile image, String authorEmail) {
        log.info("Creating new post for user: {}", authorEmail);

        // Find the author
        User author = userRepository.findByEmail(authorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authorEmail));

        // Upload image
        String imageUrl = fileStorageService.uploadFile(image);

        // Create new post entity
        Post post = Post.builder()
                .author(author)
                .imageUrl(imageUrl)
                .textContent(request.getTextContent())
                .build();

        // Save post to database
        Post savedPost = postRepository.save(post);

        // 🚀 PUBLISH DOMAIN EVENT for read store synchronization
        PostCreatedEvent event = PostCreatedEvent.from(
                savedPost.getId(),
                savedPost.getAuthor().getId(),
                savedPost.getAuthor().getName(),
                savedPost.getImageUrl(),
                savedPost.getTextContent(),
                savedPost.getCreatedAt()
        );
        eventPublisher.publishEvent(event);

        log.info("Post created successfully with ID: {} and event published", savedPost.getId());

        // Return response DTO
        return CreatePostResponse.from(
                savedPost.getId(),
                savedPost.getAuthor().getName(),
                savedPost.getImageUrl(),
                savedPost.getTextContent(),
                savedPost.getCreatedAt()
        );
    }

    /**
     * Like a post.
     * 
     * @param postId The ID of the post to like
     * @param userEmail The email of the authenticated user
     * @throws ResourceNotFoundException if post not found
     * @throws IllegalStateException if post is already liked by user
     */
    @Transactional
    @CacheEvict(value = {"posts", "feed"}, allEntries = true)
    public void likePost(Long postId, String userEmail) {
        log.info("User {} attempting to like post {}", userEmail, postId);

        // Find user and post
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found: " + postId));

        // Check if already liked
        if (likeRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            throw new IllegalStateException("Post already liked by user");
        }

        // Create new like
        Like like = new Like(user, post);
        likeRepository.save(like);

        // 🚀 PUBLISH DOMAIN EVENT for read store synchronization
        PostLikedEvent event = PostLikedEvent.from(postId, user.getId(), user.getName());
        eventPublisher.publishEvent(event);

        log.info("Post {} liked successfully by user {} and event published", postId, userEmail);
    }

    /**
     * Unlike a post.
     * 
     * @param postId The ID of the post to unlike
     * @param userEmail The email of the authenticated user
     * @throws ResourceNotFoundException if post not found or not liked
     */
    @Transactional
    @CacheEvict(value = {"posts", "feed"}, allEntries = true)
    public void unlikePost(Long postId, String userEmail) {
        log.info("User {} attempting to unlike post {}", userEmail, postId);

        // Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Check if post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found: " + postId);
        }

        // Check if like exists
        if (!likeRepository.existsByUserIdAndPostId(user.getId(), postId)) {
            throw new ResourceNotFoundException("Like not found for post: " + postId);
        }

        // Remove like
        likeRepository.deleteByUserIdAndPostId(user.getId(), postId);

        // 🚀 PUBLISH DOMAIN EVENT for read store synchronization
        PostUnlikedEvent event = PostUnlikedEvent.from(postId, user.getId(), user.getName());
        eventPublisher.publishEvent(event);

        log.info("Post {} unliked successfully by user {} and event published", postId, userEmail);
    }

}