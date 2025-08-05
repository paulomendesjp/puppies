package com.puppies.api.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for publishing domain events to RabbitMQ.
 * 
 * This enables asynchronous communication between the write side (commands)
 * and read side (queries) in our CQRS architecture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    // Exchange and routing key constants
    private static final String DOMAIN_EVENTS_EXCHANGE = "puppies.domain.events";
    private static final String POST_EVENTS_ROUTING_KEY = "post.events";
    private static final String USER_EVENTS_ROUTING_KEY = "user.events";

    /**
     * Publish a domain event to the appropriate queue.
     */
    public void publishEvent(DomainEvent event) {
        try {
            String routingKey = getRoutingKey(event);
            
            log.info("Publishing event: {} with ID: {} to routing key: {}", 
                    event.getEventType(), event.getEventId(), routingKey);
            
            rabbitTemplate.convertAndSend(
                DOMAIN_EVENTS_EXCHANGE, 
                routingKey, 
                event
            );
            
            log.debug("Event published successfully: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Failed to publish event: {} with ID: {}", 
                    event.getEventType(), event.getEventId(), e);
            
            // In production, you might want to:
            // 1. Store failed events in a dead letter queue
            // 2. Implement retry mechanisms
            // 3. Alert operations team
            throw new EventPublishingException("Failed to publish event", e);
        }
    }

    /**
     * Determine the routing key based on event type.
     */
    private String getRoutingKey(DomainEvent event) {
        String eventType = event.getEventType();
        if (eventType == null) {
            return "unknown.events";
        }
        
        return switch (eventType) {
            case "POST_CREATED" -> "post.events.created";
            case "POST_LIKED" -> "post.events.liked";
            case "POST_UNLIKED" -> "post.events.unliked";
            case "USER_CREATED" -> "user.events.created";
            case "USER_UPDATED" -> "user.events.updated";
            default -> "unknown.events";
        };
    }

    /**
     * Exception thrown when event publishing fails.
     */
    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}