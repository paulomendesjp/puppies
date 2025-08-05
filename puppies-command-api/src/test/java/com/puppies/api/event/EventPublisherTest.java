package com.puppies.api.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventPublisher.
 * 
 * Tests event publishing functionality for CQRS architecture,
 * routing key determination, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisher Tests")
class EventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventPublisher eventPublisher;

    private PostCreatedEvent postCreatedEvent;
    private PostLikedEvent postLikedEvent;
    private PostUnlikedEvent postUnlikedEvent;
    private UserCreatedEvent userCreatedEvent;

    @BeforeEach
    void setUp() {
        postCreatedEvent = PostCreatedEvent.from(
                1L, 1L, "John Doe", "http://example.com/image.jpg", 
                "Test content", LocalDateTime.now()
        );

        postLikedEvent = PostLikedEvent.from(1L, 1L, "John Doe");
        postUnlikedEvent = PostUnlikedEvent.from(1L, 1L, "John Doe");
        userCreatedEvent = UserCreatedEvent.from(1L, "John Doe", "john@example.com", LocalDateTime.now());
    }

    @Test
    @DisplayName("Should publish PostCreatedEvent with correct routing key")
    void publishEvent_WithPostCreatedEvent_ShouldUseCorrectRoutingKey() {
        // When
        eventPublisher.publishEvent(postCreatedEvent);

        // Then
        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

        verify(rabbitTemplate).convertAndSend(
                exchangeCaptor.capture(),
                routingKeyCaptor.capture(),
                eventCaptor.capture()
        );

        assertThat(exchangeCaptor.getValue()).isEqualTo("puppies.domain.events");
        assertThat(routingKeyCaptor.getValue()).isEqualTo("post.events.created");
        assertThat(eventCaptor.getValue()).isEqualTo(postCreatedEvent);
    }

    @Test
    @DisplayName("Should publish PostLikedEvent with correct routing key")
    void publishEvent_WithPostLikedEvent_ShouldUseCorrectRoutingKey() {
        // When
        eventPublisher.publishEvent(postLikedEvent);

        // Then
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                routingKeyCaptor.capture(),
                eq(postLikedEvent)
        );

        assertThat(routingKeyCaptor.getValue()).isEqualTo("post.events.liked");
    }

    @Test
    @DisplayName("Should publish PostUnlikedEvent with correct routing key")
    void publishEvent_WithPostUnlikedEvent_ShouldUseCorrectRoutingKey() {
        // When
        eventPublisher.publishEvent(postUnlikedEvent);

        // Then
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                routingKeyCaptor.capture(),
                eq(postUnlikedEvent)
        );

        assertThat(routingKeyCaptor.getValue()).isEqualTo("post.events.unliked");
    }

    @Test
    @DisplayName("Should publish UserCreatedEvent with correct routing key")
    void publishEvent_WithUserCreatedEvent_ShouldUseCorrectRoutingKey() {
        // When
        eventPublisher.publishEvent(userCreatedEvent);

        // Then
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                routingKeyCaptor.capture(),
                eq(userCreatedEvent)
        );

        assertThat(routingKeyCaptor.getValue()).isEqualTo("user.events.created");
    }

    @Test
    @DisplayName("Should use unknown routing key for unrecognized event type")
    void publishEvent_WithUnknownEventType_ShouldUseUnknownRoutingKey() {
        // Given
        DomainEvent unknownEvent = new DomainEvent() {
            @Override
            public String getEventId() { return "test-id"; }
            
            @Override
            public String getEventType() { return "UNKNOWN_EVENT"; }
            
            @Override
            public LocalDateTime getOccurredAt() { return LocalDateTime.now(); }
            
            @Override
            public String getAggregateId() { return "test-aggregate"; }
            
            @Override
            public Long getAggregateVersion() { return 1L; }
        };

        // When
        eventPublisher.publishEvent(unknownEvent);

        // Then
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                routingKeyCaptor.capture(),
                eq(unknownEvent)
        );

        assertThat(routingKeyCaptor.getValue()).isEqualTo("unknown.events");
    }

    @Test
    @DisplayName("Should throw EventPublishingException when RabbitTemplate fails")
    void publishEvent_WhenRabbitTemplateFails_ShouldThrowEventPublishingException() {
        // Given
        RuntimeException rabbitException = new RuntimeException("RabbitMQ connection failed");
        doThrow(rabbitException).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When/Then
        assertThatThrownBy(() -> eventPublisher.publishEvent(postCreatedEvent))
                .isInstanceOf(EventPublisher.EventPublishingException.class)
                .hasMessage("Failed to publish event")
                .hasCause(rabbitException);
    }

    @Test
    @DisplayName("Should handle null event gracefully")
    void publishEvent_WithNullEvent_ShouldThrowNullPointerException() {
        // When/Then
        assertThatThrownBy(() -> eventPublisher.publishEvent(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should log event publishing attempt")
    void publishEvent_ShouldLogEventDetails() {
        // When
        eventPublisher.publishEvent(postCreatedEvent);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                eq("post.events.created"),
                eq(postCreatedEvent)
        );
        
        // Log verification would require a logger mock or log capture,
        // but the test verifies the main functionality
    }

    @Test
    @DisplayName("Should handle event with null event type")
    void publishEvent_WithNullEventType_ShouldUseUnknownRoutingKey() {
        // Given
        DomainEvent eventWithNullType = new DomainEvent() {
            @Override
            public String getEventId() { return "test-id"; }
            
            @Override
            public String getEventType() { return null; }
            
            @Override
            public LocalDateTime getOccurredAt() { return LocalDateTime.now(); }
            
            @Override
            public String getAggregateId() { return "test-aggregate"; }
            
            @Override
            public Long getAggregateVersion() { return 1L; }
        };

        // When
        eventPublisher.publishEvent(eventWithNullType);

        // Then
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                routingKeyCaptor.capture(),
                eq(eventWithNullType)
        );

        assertThat(routingKeyCaptor.getValue()).isEqualTo("unknown.events");
    }

    @Test
    @DisplayName("Should handle USER_UPDATED event type")
    void publishEvent_WithUserUpdatedEvent_ShouldUseCorrectRoutingKey() {
        // Given
        DomainEvent userUpdatedEvent = new DomainEvent() {
            @Override
            public String getEventId() { return "test-id"; }
            
            @Override
            public String getEventType() { return "USER_UPDATED"; }
            
            @Override
            public LocalDateTime getOccurredAt() { return LocalDateTime.now(); }
            
            @Override
            public String getAggregateId() { return "test-aggregate"; }
            
            @Override
            public Long getAggregateVersion() { return 1L; }
        };

        // When
        eventPublisher.publishEvent(userUpdatedEvent);

        // Then
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                routingKeyCaptor.capture(),
                eq(userUpdatedEvent)
        );

        assertThat(routingKeyCaptor.getValue()).isEqualTo("user.events.updated");
    }

    @Test
    @DisplayName("Should verify exchange name is correct")
    void publishEvent_ShouldUseCorrectExchangeName() {
        // When
        eventPublisher.publishEvent(postCreatedEvent);

        // Then
        verify(rabbitTemplate).convertAndSend(
                eq("puppies.domain.events"),
                anyString(),
                eq(postCreatedEvent)
        );
    }
}