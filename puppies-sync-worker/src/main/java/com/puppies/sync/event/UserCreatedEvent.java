package com.puppies.sync.event;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event published when a new user is created
 * Must match the structure from com.puppies.api.event.UserCreatedEvent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    // Domain event base fields
    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private String aggregateId;
    private Long aggregateVersion;
    
    // Event-specific data
    private Long userId;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}
