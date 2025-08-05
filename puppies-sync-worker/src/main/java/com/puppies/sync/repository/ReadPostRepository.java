package com.puppies.sync.repository;

import com.puppies.sync.model.ReadPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for ReadPost entities in the Read Store
 */
@Repository
public interface ReadPostRepository extends JpaRepository<ReadPost, Long> {

    /**
     * Increment like count for a post
     */
    @Modifying
    @Query("UPDATE ReadPost p SET p.likeCount = p.likeCount + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :postId")
    int incrementLikeCount(@Param("postId") Long postId);

    /**
     * Decrement like count for a post
     */
    @Modifying
    @Query("UPDATE ReadPost p SET p.likeCount = p.likeCount - 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :postId AND p.likeCount > 0")
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * Update popularity score for a post
     */
    @Modifying
    @Query("UPDATE ReadPost p SET p.popularityScore = :score, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :postId")
    int updatePopularityScore(@Param("postId") Long postId, @Param("score") Double score);

    /**
     * Increment view count for a post
     */
    @Modifying
    @Query("UPDATE ReadPost p SET p.viewCount = p.viewCount + 1, p.updatedAt = CURRENT_TIMESTAMP WHERE p.id = :postId")
    int incrementViewCount(@Param("postId") Long postId);

    /**
     * Find post by ID for read operations
     */
    Optional<ReadPost> findById(Long id);

    /**
     * Count posts by author
     */
    @Query("SELECT COUNT(p) FROM ReadPost p WHERE p.authorId = :authorId")
    Long countByAuthorId(@Param("authorId") Long authorId);
}