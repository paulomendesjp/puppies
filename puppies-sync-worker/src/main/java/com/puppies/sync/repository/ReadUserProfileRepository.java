package com.puppies.sync.repository;

import com.puppies.sync.model.ReadUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for ReadUserProfile entities in the Read Store
 */
@Repository
public interface ReadUserProfileRepository extends JpaRepository<ReadUserProfile, Long> {

    /**
     * Increment posts count for a user
     */
    @Modifying
    @Query("UPDATE ReadUserProfile u SET u.postsCount = u.postsCount + 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int incrementPostsCount(@Param("userId") Long userId);

    /**
     * Increment total likes received for a user
     */
    @Modifying
    @Query("UPDATE ReadUserProfile u SET u.totalLikesReceived = u.totalLikesReceived + 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int incrementLikesReceived(@Param("userId") Long userId);

    /**
     * Decrement total likes received for a user
     */
    @Modifying
    @Query("UPDATE ReadUserProfile u SET u.totalLikesReceived = u.totalLikesReceived - 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId AND u.totalLikesReceived > 0")
    int decrementLikesReceived(@Param("userId") Long userId);

    /**
     * Increment total likes given by a user
     */
    @Modifying
    @Query("UPDATE ReadUserProfile u SET u.totalLikesGiven = u.totalLikesGiven + 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int incrementLikesGiven(@Param("userId") Long userId);

    /**
     * Decrement total likes given by a user
     */
    @Modifying
    @Query("UPDATE ReadUserProfile u SET u.totalLikesGiven = u.totalLikesGiven - 1, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId AND u.totalLikesGiven > 0")
    int decrementLikesGiven(@Param("userId") Long userId);

    /**
     * Update last active timestamp
     */
    @Modifying
    @Query("UPDATE ReadUserProfile u SET u.lastActiveAt = :timestamp, u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    int updateLastActiveAt(@Param("userId") Long userId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Find user profile by email
     */
    Optional<ReadUserProfile> findByEmail(String email);

    /**
     * Check if user exists
     */
    boolean existsById(Long id);
}