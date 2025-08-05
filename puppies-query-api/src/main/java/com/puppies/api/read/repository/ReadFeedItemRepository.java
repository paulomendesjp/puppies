package com.puppies.api.read.repository;

import com.puppies.api.read.model.ReadFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for read-only feed queries from the read store
 */
@Repository
public interface ReadFeedItemRepository extends JpaRepository<ReadFeedItem, Long> {

    /**
     * Get user's feed ordered by creation time (latest first)
     */
    Page<ReadFeedItem> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Get user's feed ordered by popularity
     */
    Page<ReadFeedItem> findByUserIdOrderByPopularityScoreDesc(Long userId, Pageable pageable);

    /**
     * Get trending feed items across all users
     */
    Page<ReadFeedItem> findAllByOrderByPopularityScoreDesc(Pageable pageable);

    /**
     * Get recent highly engaging posts for a user
     */
    @Query("SELECT f FROM ReadFeedItem f WHERE f.userId = :userId AND f.likeCount > :minLikes ORDER BY f.createdAt DESC")
    List<ReadFeedItem> findRecentEngagingPosts(@Param("userId") Long userId, @Param("minLikes") Long minLikes, Pageable pageable);

    /**
     * Get posts from specific author in user's feed
     */
    Page<ReadFeedItem> findByUserIdAndPostAuthorIdOrderByCreatedAtDesc(Long userId, Long authorId, Pageable pageable);

    /**
     * Count feed items for a user
     */
    Long countByUserId(Long userId);

    /**
     * Find posts that user has liked in their feed
     */
    Page<ReadFeedItem> findByUserIdAndIsLikedByUserTrueOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Get discovery feed (posts from all users, high popularity)
     */
    @Query("SELECT f FROM ReadFeedItem f WHERE f.popularityScore > :minScore ORDER BY f.popularityScore DESC, f.createdAt DESC")
    List<ReadFeedItem> findDiscoveryFeed(@Param("minScore") Double minScore, Pageable pageable);
}