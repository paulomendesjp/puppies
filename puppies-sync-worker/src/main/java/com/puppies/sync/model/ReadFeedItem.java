package com.puppies.sync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Denormalized Feed Item model for Read Store
 * Pre-calculated feed items for faster feed queries
 */
@Entity
@Table(name = "read_feed_items", 
       indexes = {
           @Index(name = "idx_user_created", columnList = "user_id, created_at DESC"),
           @Index(name = "idx_post_id", columnList = "post_id"),
           @Index(name = "idx_popularity", columnList = "popularity_score DESC")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadFeedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "post_author_id", nullable = false)
    private Long postAuthorId;

    @Column(name = "post_author_name", nullable = false)
    private String postAuthorName;

    @Column(name = "post_content", length = 2000)
    private String postContent;

    @Column(name = "post_image_url", nullable = false)
    private String postImageUrl;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "is_liked_by_user", nullable = false)
    private Boolean isLikedByUser = false;

    @Column(name = "popularity_score")
    private Double popularityScore = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}