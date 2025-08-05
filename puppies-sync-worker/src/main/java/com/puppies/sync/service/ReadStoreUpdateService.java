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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for updating the Read database
 * when events are received from the Command side.
 * 
 * This is the CORRECT place for read store updates in CQRS architecture.
 * The Command API publishes events, and this service consumes them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReadStoreUpdateService {

    private final ReadPostRepository readPostRepository;
    private final ReadUserProfileRepository readUserProfileRepository;
    private final ReadFeedItemRepository readFeedItemRepository;

    @Transactional
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("📝 Updating read store for new post: {}", event.getPostId());
        
        try {
            // 1. Create denormalized post record in read database
            ReadPost readPost = ReadPost.builder()
                .id(event.getPostId())
                .authorId(event.getAuthorId())
                .authorName(event.getAuthorName()) // Now available from the event
                .content(event.getTextContent()) // Updated field name
                .imageUrl(event.getImageUrl())
                .createdAt(event.getCreatedAt())
                .likeCount(0L)
                .commentCount(0L)
                .viewCount(0L)
                .popularityScore(calculateInitialPopularityScore(event))
                .updatedAt(LocalDateTime.now())
                .build();
            
            readPostRepository.save(readPost);
            log.debug("✅ Created denormalized post record for post {}", event.getPostId());
            
            // 2. Update user post count in denormalized user profile
            int updatedProfiles = readUserProfileRepository.incrementPostsCount(event.getAuthorId());
            if (updatedProfiles > 0) {
                log.debug("✅ Incremented posts count for user {}", event.getAuthorId());
            } else {
                log.warn("⚠️ User profile not found for user {}, creating placeholder", event.getAuthorId());
                createUserProfilePlaceholder(event.getAuthorId(), event.getAuthorName());
                readUserProfileRepository.incrementPostsCount(event.getAuthorId());
            }
            
            // 3. Create feed items for all users (in real system, this would be for followers only)
            // For demo purposes, we'll create feed items for a few mock users
            createFeedItemsForNewPost(readPost);
            
            // 4. Update daily/hourly aggregations (for analytics)
            updatePostCreationMetrics(event);
            
            log.info("✅ Post {} successfully added to read store with {} feed items", 
                    event.getPostId(), countFeedItemsForPost(event.getPostId()));
            
        } catch (Exception e) {
            log.error("❌ Failed to update read store for post creation: {}", event.getPostId(), e);
            throw new ReadStoreUpdateException("Failed to process post creation event", e);
        }
    }

    @Transactional 
    public void handlePostLiked(PostLikedEvent event) {
        log.info("❤️ Updating read store for post like: {}", event.getPostId());
        
        try {
            // 1. Update like count in denormalized post record
            int updatedPosts = readPostRepository.incrementLikeCount(event.getPostId());
            if (updatedPosts == 0) {
                log.warn("⚠️ Post {} not found in read store, skipping like update", event.getPostId());
                return;
            }
            
            // 2. Update like count in all feed items for this post
            Optional<ReadPost> readPost = readPostRepository.findById(event.getPostId());
            if (readPost.isPresent()) {
                Long newLikeCount = readPost.get().getLikeCount();
                readFeedItemRepository.updateLikeCountForPost(event.getPostId(), newLikeCount);
                
                // Update like status for the specific user
                readFeedItemRepository.updateLikeStatusForUser(event.getPostId(), event.getUserId(), true);
                
                log.debug("✅ Updated like count to {} for post {}", newLikeCount, event.getPostId());
            }
            
            // 3. Update user statistics
            // Increment likes given by the user who liked
            readUserProfileRepository.incrementLikesGiven(event.getUserId());
            
            // Increment likes received by the post author
            Optional<ReadPost> post = readPostRepository.findById(event.getPostId());
            if (post.isPresent()) {
                readUserProfileRepository.incrementLikesReceived(post.get().getAuthorId());
                log.debug("✅ Incremented likes received for post author {}", post.get().getAuthorId());
            }
            
            // 4. Update popularity score based on engagement
            updatePopularityScore(event.getPostId());
            
            // 5. Update user activity timestamp
            readUserProfileRepository.updateLastActiveAt(event.getUserId(), event.getLikedAt());
            
            log.info("✅ Like for post {} successfully processed in read store", event.getPostId());
            
        } catch (Exception e) {
            log.error("❌ Failed to update read store for post like: {}", event.getPostId(), e);
            throw new ReadStoreUpdateException("Failed to process post like event", e);
        }
    }

    @Transactional
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("👤 Updating read store for new user: {}", event.getUserId());
        
        try {
            // 1. Create denormalized user profile record
            ReadUserProfile profile = ReadUserProfile.builder()
                .id(event.getUserId())
                .name(event.getName())
                .email(event.getEmail())
                .createdAt(event.getCreatedAt())
                .lastActiveAt(event.getCreatedAt())
                .postsCount(0L)
                .followersCount(0L)
                .followingCount(0L)
                .totalLikesReceived(0L)
                .totalLikesGiven(0L)
                .updatedAt(LocalDateTime.now())
                .build();
            
            readUserProfileRepository.save(profile);
            log.debug("✅ Created user profile for user {}", event.getUserId());
            
            // 2. Initialize feed items for existing posts (limited for demo)
            initializeFeedForNewUser(event.getUserId());
            
            // 3. Update user registration metrics
            updateUserRegistrationMetrics(event);
            
            log.info("✅ User {} successfully added to read store with initialized feed", event.getUserId());
            
        } catch (Exception e) {
            log.error("❌ Failed to update read store for user creation: {}", event.getUserId(), e);
            throw new ReadStoreUpdateException("Failed to process user creation event", e);
        }
    }

    /**
     * Calculate initial popularity score for a new post
     */
    private Double calculateInitialPopularityScore(PostCreatedEvent event) {
        // Base score based on author popularity (simplified)
        return 1.0; // In real system, this would consider author's follower count, etc.
    }

    /**
     * Create feed items for a new post (for all users in demo)
     */
    private void createFeedItemsForNewPost(ReadPost readPost) {
        try {
            // In a real system, this would only create feed items for followers
            // For demo, we'll create feed items for existing users (limited to avoid too many records)
            List<ReadUserProfile> users = readUserProfileRepository.findAll();
            
            int feedItemsCreated = 0;
            for (ReadUserProfile user : users) {
                if (feedItemsCreated >= 10) break; // Limit for demo
                
                ReadFeedItem feedItem = ReadFeedItem.builder()
                    .userId(user.getId())
                    .postId(readPost.getId())
                    .postAuthorId(readPost.getAuthorId())
                    .postAuthorName(readPost.getAuthorName())
                    .postContent(readPost.getContent())
                    .postImageUrl(readPost.getImageUrl())
                    .likeCount(readPost.getLikeCount())
                    .isLikedByUser(false)
                    .popularityScore(readPost.getPopularityScore())
                    .createdAt(readPost.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                readFeedItemRepository.save(feedItem);
                feedItemsCreated++;
            }
            
            log.debug("✅ Created {} feed items for post {}", feedItemsCreated, readPost.getId());
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to create feed items for post {}", readPost.getId(), e);
        }
    }

    /**
     * Initialize feed for a new user with recent posts
     */
    private void initializeFeedForNewUser(Long userId) {
        try {
            // Get recent posts (limited for demo)
            List<ReadPost> recentPosts = readPostRepository.findAll()
                    .stream()
                    .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                    .limit(5)
                    .toList();
            
            for (ReadPost post : recentPosts) {
                ReadFeedItem feedItem = ReadFeedItem.builder()
                    .userId(userId)
                    .postId(post.getId())
                    .postAuthorId(post.getAuthorId())
                    .postAuthorName(post.getAuthorName())
                    .postContent(post.getContent())
                    .postImageUrl(post.getImageUrl())
                    .likeCount(post.getLikeCount())
                    .isLikedByUser(false)
                    .popularityScore(post.getPopularityScore())
                    .createdAt(post.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                readFeedItemRepository.save(feedItem);
            }
            
            log.debug("✅ Initialized feed with {} posts for user {}", recentPosts.size(), userId);
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to initialize feed for user {}", userId, e);
        }
    }

    /**
     * Update popularity score based on engagement
     */
    private void updatePopularityScore(Long postId) {
        try {
            Optional<ReadPost> postOpt = readPostRepository.findById(postId);
            if (postOpt.isPresent()) {
                ReadPost post = postOpt.get();
                
                // Simple popularity calculation based on likes and age
                long hoursOld = java.time.Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours();
                double ageDecay = Math.max(0.1, 1.0 / (1.0 + hoursOld * 0.1));
                double likeScore = post.getLikeCount() * 2.0;
                double viewScore = post.getViewCount() * 0.1;
                
                double newPopularityScore = (likeScore + viewScore) * ageDecay;
                
                readPostRepository.updatePopularityScore(postId, newPopularityScore);
                readFeedItemRepository.updatePopularityScoreForPost(postId, newPopularityScore);
                
                log.debug("✅ Updated popularity score to {} for post {}", newPopularityScore, postId);
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to update popularity score for post {}", postId, e);
        }
    }

    /**
     * Create a placeholder user profile if not exists
     */
    private void createUserProfilePlaceholder(Long userId, String userName) {
        try {
            if (!readUserProfileRepository.existsById(userId)) {
                ReadUserProfile placeholder = ReadUserProfile.builder()
                    .id(userId)
                    .name(userName)
                    .email("user" + userId + "@placeholder.com")
                    .createdAt(LocalDateTime.now())
                    .lastActiveAt(LocalDateTime.now())
                    .postsCount(0L)
                    .followersCount(0L)
                    .followingCount(0L)
                    .totalLikesReceived(0L)
                    .totalLikesGiven(0L)
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                readUserProfileRepository.save(placeholder);
                log.debug("✅ Created placeholder profile for user {}", userId);
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to create placeholder profile for user {}", userId, e);
        }
    }

    /**
     * Count feed items for a post
     */
    private long countFeedItemsForPost(Long postId) {
        try {
            return readFeedItemRepository.findByPostId(postId).size();
        } catch (Exception e) {
            log.warn("⚠️ Failed to count feed items for post {}", postId, e);
            return 0;
        }
    }

    /**
     * Update post creation metrics (for analytics)
     */
    private void updatePostCreationMetrics(PostCreatedEvent event) {
        // In a real system, this would update time-series data for analytics
        log.debug("📊 Updated post creation metrics for {}", event.getCreatedAt().toLocalDate());
    }

    /**
     * Update user registration metrics (for analytics)
     */
    private void updateUserRegistrationMetrics(UserCreatedEvent event) {
        // In a real system, this would update time-series data for analytics
        log.debug("📊 Updated user registration metrics for {}", event.getCreatedAt().toLocalDate());
    }

    /**
     * Exception thrown when read store updates fail.
     */
    public static class ReadStoreUpdateException extends RuntimeException {
        public ReadStoreUpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}