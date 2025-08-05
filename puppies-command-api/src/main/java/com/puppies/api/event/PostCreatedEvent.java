package com.puppies.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event fired when a new post is created.
 * This event triggers updates to the read store for feed generation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCreatedEvent implements DomainEvent {
    
    private String eventId;
    private String eventType = "POST_CREATED";
    private LocalDateTime occurredAt;
    private String aggregateId; // Post ID
    private Long aggregateVersion;
    
    // Event-specific data
    private Long postId;
    private Long authorId;
    private String authorName;
    private String imageUrl;
    private String textContent;
    private LocalDateTime createdAt;
    
    public static PostCreatedEvent from(Long postId, Long authorId, String authorName, 
                                      String imageUrl, String textContent, LocalDateTime createdAt) {
        return PostCreatedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("POST_CREATED")
                .occurredAt(LocalDateTime.now())
                .aggregateId(postId.toString())
                .aggregateVersion(1L)
                .postId(postId)
                .authorId(authorId)
                .authorName(authorName)
                .imageUrl(imageUrl)
                .textContent(textContent)
                .createdAt(createdAt)
                .build();
    }
}