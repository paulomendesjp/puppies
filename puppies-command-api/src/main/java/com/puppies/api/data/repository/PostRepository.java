package com.puppies.api.data.repository;

import com.puppies.api.data.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Post entity operations.
 * Provides standard CRUD operations plus optimized queries for the Query side of CQRS.
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Find posts by author ID, ordered by creation date (newest first).
     * Used for user profile post listings.
     */
    Page<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /**
     * Find all posts ordered by creation date (newest first).
     * Basic query for the main feed - will be enhanced with JOIN queries.
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Count posts by author ID.
     */
    long countByAuthorId(Long authorId);

    /**
     * OPTIMIZED QUERY: Get feed posts with all required data in a single query.
     * This eliminates the N+1 query problem by JOINing all necessary data.
     * 
     * Returns: Post ID, Author Name, Image URL, Text Content, Created At, Like Count, Is Liked By User
     */
    @Query("""
        SELECT p.id as postId,
               p.author.name as authorName,
               p.imageUrl as imageUrl,
               p.textContent as textContent,
               p.createdAt as createdAt,
               COUNT(l.id) as likeCount,
               CASE WHEN ul.id IS NOT NULL THEN true ELSE false END as isLikedByUser
        FROM Post p
        LEFT JOIN p.likes l
        LEFT JOIN p.likes ul ON ul.user.id = :currentUserId
        GROUP BY p.id, p.author.name, p.imageUrl, p.textContent, p.createdAt, ul.id
        ORDER BY p.createdAt DESC
        """)
    Page<Object[]> findFeedPostsWithDetails(@Param("currentUserId") Long currentUserId, Pageable pageable);

    /**
     * OPTIMIZED QUERY: Get a single post with all details.
     * Used for individual post view.
     */
    @Query("""
        SELECT p.id as postId,
               p.author.name as authorName,
               p.imageUrl as imageUrl,
               p.textContent as textContent,
               p.createdAt as createdAt,
               COUNT(l.id) as likeCount,
               CASE WHEN ul.id IS NOT NULL THEN true ELSE false END as isLikedByUser
        FROM Post p
        LEFT JOIN p.likes l
        LEFT JOIN p.likes ul ON ul.user.id = :currentUserId
        WHERE p.id = :postId
        GROUP BY p.id, p.author.name, p.imageUrl, p.textContent, p.createdAt, ul.id
        """)
    Optional<Object[]> findPostWithDetails(@Param("postId") Long postId, @Param("currentUserId") Long currentUserId);

    /**
     * OPTIMIZED QUERY: Get user's posts with like counts.
     */
    @Query("""
        SELECT p.id as postId,
               p.author.name as authorName,
               p.imageUrl as imageUrl,
               p.textContent as textContent,
               p.createdAt as createdAt,
               COUNT(l.id) as likeCount,
               CASE WHEN ul.id IS NOT NULL THEN true ELSE false END as isLikedByUser
        FROM Post p
        LEFT JOIN p.likes l
        LEFT JOIN p.likes ul ON ul.user.id = :currentUserId
        WHERE p.author.id = :authorId
        GROUP BY p.id, p.author.name, p.imageUrl, p.textContent, p.createdAt, ul.id
        ORDER BY p.createdAt DESC
        """)
    Page<Object[]> findUserPostsWithDetails(@Param("authorId") Long authorId, 
                                          @Param("currentUserId") Long currentUserId, 
                                          Pageable pageable);

    /**
     * OPTIMIZED QUERY: Get posts liked by a specific user.
     */
    @Query("""
        SELECT p.id as postId,
               p.author.name as authorName,
               p.imageUrl as imageUrl,
               p.textContent as textContent,
               p.createdAt as createdAt,
               COUNT(l.id) as likeCount,
               true as isLikedByUser
        FROM Post p
        INNER JOIN p.likes userLike ON userLike.user.id = :userId
        LEFT JOIN p.likes l
        GROUP BY p.id, p.author.name, p.imageUrl, p.textContent, p.createdAt
        ORDER BY userLike.createdAt DESC
        """)
    Page<Object[]> findLikedPostsWithDetails(@Param("userId") Long userId, Pageable pageable);
}