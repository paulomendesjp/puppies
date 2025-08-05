package com.puppies.sync.event;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a new post is created
 * Must match the structure from com.puppies.api.event.PostCreatedEvent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreatedEvent {
    // Domain event base fields
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private String aggregateId;
    private Long aggregateVersion;
    
    // Event-specific data
    private Long postId;
    private Long authorId;
    private String authorName;
    private String imageUrl;
    private String textContent;
    private LocalDateTime createdAt;
}
