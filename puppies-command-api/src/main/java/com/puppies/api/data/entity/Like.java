package com.puppies.api.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Like entity representing a user's like action on a post.
 * 
 * This entity design is crucial for scalability. Instead of using a @ManyToMany
 * relationship between User and Post (which would be inefficient for posts with
 * thousands of likes), we model Like as its own entity.
 * 
 * Benefits:
 * - Efficient COUNT queries for like counts
 * - Efficient INSERT/DELETE for like/unlike operations
 * - Ability to add metadata (like timestamp) to the like action
 * - Prevents the N+1 query problem when loading posts with like information
 */
@Entity
@Table(name = "likes", 
       uniqueConstraints = @UniqueConstraint(
           name = "uk_user_post", 
           columnNames = {"user_id", "post_id"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"user", "post"})
@ToString(exclude = {"user", "post"})
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Post is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Constructor for creating a new like
     */
    public Like(User user, Post post) {
        this.user = user;
        this.post = post;
    }
}