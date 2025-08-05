package com.puppies.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event fired when a post is liked.
 * This event triggers updates to post like counts in the read store.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLikedEvent implements DomainEvent {
    
    private String eventId;
    private String eventType = "POST_LIKED";
    private LocalDateTime occurredAt;
    private String aggregateId; // Post ID
    private Long aggregateVersion;
    
    // Event-specific data
    private Long postId;
    private Long userId;
    private String userName;
    private LocalDateTime likedAt;
    
    public static PostLikedEvent from(Long postId, Long userId, String userName) {
        return PostLikedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("POST_LIKED")
                .occurredAt(LocalDateTime.now())
                .aggregateId(postId.toString())
                .aggregateVersion(1L)
                .postId(postId)
                .userId(userId)
                .userName(userName)
                .likedAt(LocalDateTime.now())
                .build();
    }
}