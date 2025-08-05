package com.puppies.api.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Post entity representing a user's post in the Puppies API.
 * This entity is optimized for write operations (Command side of CQRS).
 */
@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"likes"})
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Author is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @NotBlank(message = "Image URL is required")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Size(max = 2000, message = "Text content must not exceed 2000 characters")
    @Column(name = "text_content", length = 2000)
    private String textContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // One-to-Many relationship with Like
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Like> likes = new ArrayList<>();

    /**
     * Constructor for creating a new post
     */
    public Post(User author, String imageUrl, String textContent) {
        this.author = author;
        this.imageUrl = imageUrl;
        this.textContent = textContent;
        this.likes = new ArrayList<>();
    }

    /**
     * Gets the count of likes for this post.
     * Note: This is not optimized and should not be used in query operations.
     * Use the query-side services with proper aggregations instead.
     */
    public int getLikeCount() {
        return likes.size();
    }
}