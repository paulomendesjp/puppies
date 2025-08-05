package com.puppies.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event fired when a post is unliked.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostUnlikedEvent implements DomainEvent {
    
    private String eventId;
    private String eventType = "POST_UNLIKED";
    private LocalDateTime occurredAt;
    private String aggregateId; // Post ID
    private Long aggregateVersion;
    
    // Event-specific data
    private Long postId;
    private Long userId;
    private String userName;
    private LocalDateTime unlikedAt;
    
    public static PostUnlikedEvent from(Long postId, Long userId, String userName) {
        return PostUnlikedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("POST_UNLIKED")
                .occurredAt(LocalDateTime.now())
                .aggregateId(postId.toString())
                .aggregateVersion(1L)
                .postId(postId)
                .userId(userId)
                .userName(userName)
                .unlikedAt(LocalDateTime.now())
                .build();
    }
}