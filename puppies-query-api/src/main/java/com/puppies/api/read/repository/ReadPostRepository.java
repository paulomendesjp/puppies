package com.puppies.api.read.repository;

import com.puppies.api.read.model.ReadPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for read-only post queries from the read store
 */
@Repository
public interface ReadPostRepository extends JpaRepository<ReadPost, Long> {

    /**
     * Find posts ordered by creation date (latest first)
     */
    Page<ReadPost> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find trending posts (by popularity score)
     */
    Page<ReadPost> findAllByOrderByPopularityScoreDesc(Pageable pageable);

    /**
     * Find most liked posts
     */
    Page<ReadPost> findAllByOrderByLikeCountDesc(Pageable pageable);

    /**
     * Find posts by author
     */
    Page<ReadPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /**
     * Find recent posts (last 24 hours) ordered by popularity
     */
    @Query("SELECT p FROM ReadPost p WHERE p.createdAt > :since ORDER BY p.popularityScore DESC")
    List<ReadPost> findRecentTrendingPosts(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * Search posts by content
     */
    @Query("SELECT p FROM ReadPost p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY p.createdAt DESC")
    Page<ReadPost> searchByContent(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count posts by author
     */
    Long countByAuthorId(Long authorId);

    /**
     * Find posts with high engagement (likes > threshold)
     */
    @Query("SELECT p FROM ReadPost p WHERE p.likeCount > :threshold ORDER BY p.likeCount DESC")
    List<ReadPost> findHighEngagementPosts(@Param("threshold") Long threshold, Pageable pageable);
}