package com.puppies.api.read.repository;

import com.puppies.api.read.model.ReadUserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for read-only user profile queries from the read store
 */
@Repository
public interface ReadUserProfileRepository extends JpaRepository<ReadUserProfile, Long> {

    /**
     * Find user by email
     */
    Optional<ReadUserProfile> findByEmail(String email);

    /**
     * Find users with most posts
     */
    Page<ReadUserProfile> findAllByOrderByPostsCountDesc(Pageable pageable);

    /**
     * Find users with most likes received
     */
    Page<ReadUserProfile> findAllByOrderByTotalLikesReceivedDesc(Pageable pageable);

    /**
     * Find most active users (by total engagement)
     */
    @Query("SELECT u FROM ReadUserProfile u ORDER BY (u.postsCount + u.totalLikesGiven + u.totalLikesReceived) DESC")
    List<ReadUserProfile> findMostActiveUsers(Pageable pageable);

    /**
     * Search users by name
     */
    @Query("SELECT u FROM ReadUserProfile u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY u.totalLikesReceived DESC")
    Page<ReadUserProfile> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Find users with posts count greater than threshold
     */
    List<ReadUserProfile> findByPostsCountGreaterThanOrderByPostsCountDesc(Long threshold);

    /**
     * Get user statistics summary
     */
    @Query("SELECT u FROM ReadUserProfile u WHERE u.id = :userId")
    Optional<ReadUserProfile> findUserStats(@Param("userId") Long userId);
}