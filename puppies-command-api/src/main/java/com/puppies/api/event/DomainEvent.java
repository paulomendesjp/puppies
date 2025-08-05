package com.puppies.api.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

/**
 * Base interface for all domain events in the Puppies API.
 * 
 * This is the foundation for implementing true CQRS with event-driven
 * synchronization between write and read stores.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserCreatedEvent.class, name = "USER_CREATED"),
    @JsonSubTypes.Type(value = PostCreatedEvent.class, name = "POST_CREATED"),
    @JsonSubTypes.Type(value = PostLikedEvent.class, name = "POST_LIKED"),
    @JsonSubTypes.Type(value = PostUnlikedEvent.class, name = "POST_UNLIKED")
})
public interface DomainEvent {
    
    /**
     * Unique identifier for this event instance.
     */
    String getEventId();
    
    /**
     * The type of event (USER_CREATED, POST_CREATED, etc.).
     */
    String getEventType();
    
    /**
     * When this event occurred.
     */
    LocalDateTime getOccurredAt();
    
    /**
     * The aggregate ID that this event relates to.
     */
    String getAggregateId();
    
    /**
     * Version of the aggregate after this event.
     */
    Long getAggregateVersion();
}