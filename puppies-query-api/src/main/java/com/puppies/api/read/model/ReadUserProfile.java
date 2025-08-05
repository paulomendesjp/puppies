package com.puppies.api.read.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Read-only User Profile model for Query API
 * Mirrors the denormalized structure from Sync Worker
 */
@Entity
@Table(name = "read_user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadUserProfile {

    @Id
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "posts_count", nullable = false)
    private Long postsCount = 0L;

    @Column(name = "followers_count", nullable = false)
    private Long followersCount = 0L;

    @Column(name = "following_count", nullable = false)
    private Long followingCount = 0L;

    @Column(name = "total_likes_received", nullable = false)
    private Long totalLikesReceived = 0L;

    @Column(name = "total_likes_given", nullable = false)
    private Long totalLikesGiven = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}