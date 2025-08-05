package com.puppies.api.data.repository;

import com.puppies.api.data.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations.
 * Provides standard CRUD operations plus custom queries for the Command side of CQRS.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address (used for authentication).
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given email.
     */
    boolean existsByEmail(String email);

    /**
     * Find users by name containing the search term (case-insensitive).
     * Used for user search functionality.
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Custom query to get user statistics.
     * This demonstrates a read-optimized query that could be used in the Query side.
     */
    @Query("""
        SELECT u.id as userId,
               u.name as userName,
               u.email as userEmail,
               u.createdAt as userCreatedAt,
               COUNT(DISTINCT p.id) as postCount,
               COALESCE(SUM(l.likeCount), 0) as totalLikesReceived
        FROM User u
        LEFT JOIN u.posts p
        LEFT JOIN (
            SELECT post.id as postId, COUNT(like.id) as likeCount
            FROM Like like
            GROUP BY like.post.id
        ) l ON p.id = l.postId
        WHERE u.id = :userId
        GROUP BY u.id, u.name, u.email, u.createdAt
        """)
    Optional<Object[]> findUserStatistics(@Param("userId") Long userId);
}