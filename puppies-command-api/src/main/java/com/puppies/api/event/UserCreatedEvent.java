package com.puppies.api.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain event fired when a new user is created.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent implements DomainEvent {
    
    private String eventId;
    private String eventType = "USER_CREATED";
    private LocalDateTime occurredAt;
    private String aggregateId; // User ID
    private Long aggregateVersion;
    
    // Event-specific data
    private Long userId;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    
    public static UserCreatedEvent from(Long userId, String name, String email, LocalDateTime createdAt) {
        return UserCreatedEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .eventType("USER_CREATED")
                .occurredAt(LocalDateTime.now())
                .aggregateId(userId.toString())
                .aggregateVersion(1L)
                .userId(userId)
                .name(name)
                .email(email)
                .createdAt(createdAt)
                .build();
    }
}