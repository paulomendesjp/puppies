package com.puppies.sync.event;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a post is liked
 * Must match the structure from com.puppies.api.event.PostLikedEvent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLikedEvent {
    // Domain event base fields
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private String aggregateId;
    private Long aggregateVersion;
    
    // Event-specific data
    private Long postId;
    private Long userId;
    private String userName;
    private LocalDateTime likedAt;
}
