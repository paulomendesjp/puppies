package com.puppies.api.data.repository;

import com.puppies.api.data.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Repository interface for Like entity operations.
 * Optimized for the like/unlike command operations and efficient count queries.
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * Count the number of likes for a specific post.
     * This is much more efficient than loading all Like entities.
     */
    long countByPostId(Long postId);

    /**
     * Check if a specific user has liked a specific post.
     * Returns true if the like exists, false otherwise.
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);

    /**
     * Find a specific like by user and post.
     * Used for the unlike operation.
     */
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    /**
     * Delete a like by user and post IDs.
     * This is more efficient than finding the Like entity first.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.user.id = :userId AND l.post.id = :postId")
    void deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * Count total likes received by a user across all their posts.
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.author.id = :userId")
    long countLikesReceivedByUser(@Param("userId") Long userId);

    /**
     * Count total likes given by a user.
     */
    long countByUserId(Long userId);
}