package com.puppies.sync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Denormalized Post model for Read Store
 * Contains all data needed for queries without joins
 */
@Entity
@Table(name = "read_posts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadPost {

    @Id
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "content", length = 2000)
    private String content;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

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