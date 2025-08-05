package com.puppies.sync.repository;

import com.puppies.sync.model.ReadFeedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ReadFeedItem entities in the Read Store
 */
@Repository
public interface ReadFeedItemRepository extends JpaRepository<ReadFeedItem, Long> {

    /**
     * Update like count for all feed items of a specific post
     */
    @Modifying
    @Query("UPDATE ReadFeedItem f SET f.likeCount = :likeCount, f.updatedAt = CURRENT_TIMESTAMP WHERE f.postId = :postId")
    int updateLikeCountForPost(@Param("postId") Long postId, @Param("likeCount") Long likeCount);

    /**
     * Update like status for a specific user and post
     */
    @Modifying
    @Query("UPDATE ReadFeedItem f SET f.isLikedByUser = :isLiked, f.updatedAt = CURRENT_TIMESTAMP WHERE f.postId = :postId AND f.userId = :userId")
    int updateLikeStatusForUser(@Param("postId") Long postId, @Param("userId") Long userId, @Param("isLiked") Boolean isLiked);

    /**
     * Update popularity score for all feed items of a specific post
     */
    @Modifying
    @Query("UPDATE ReadFeedItem f SET f.popularityScore = :score, f.updatedAt = CURRENT_TIMESTAMP WHERE f.postId = :postId")
    int updatePopularityScoreForPost(@Param("postId") Long postId, @Param("score") Double score);

    /**
     * Find all feed items for a specific post
     */
    @Query("SELECT f FROM ReadFeedItem f WHERE f.postId = :postId")
    List<ReadFeedItem> findByPostId(@Param("postId") Long postId);

    /**
     * Find feed items for a specific user (for feed queries)
     */
    @Query("SELECT f FROM ReadFeedItem f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<ReadFeedItem> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Delete all feed items for a specific post (if post is deleted)
     */
    @Modifying
    @Query("DELETE FROM ReadFeedItem f WHERE f.postId = :postId")
    int deleteByPostId(@Param("postId") Long postId);

    /**
     * Count feed items for a user
     */
    @Query("SELECT COUNT(f) FROM ReadFeedItem f WHERE f.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);
}