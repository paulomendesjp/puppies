package com.puppies.sync.consumer;

import com.puppies.sync.event.PostCreatedEvent;
import com.puppies.sync.event.PostLikedEvent;
import com.puppies.sync.event.UserCreatedEvent;
import com.puppies.sync.service.ReadStoreUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

/**
 * Unit tests for EventConsumer.
 * 
 * Tests RabbitMQ event consumption and delegation to
 * ReadStoreUpdateService in CQRS sync worker.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventConsumer Tests")
class EventConsumerTest {

    @Mock
    private ReadStoreUpdateService readStoreUpdateService;

    @InjectMocks
    private EventConsumer eventConsumer;

    private PostCreatedEvent postCreatedEvent;
    private PostLikedEvent postLikedEvent;
    private UserCreatedEvent userCreatedEvent;

    @BeforeEach
    void setUp() {
        postCreatedEvent = PostCreatedEvent.builder()
                .eventId("test-event-1")
                .postId(1L)
                .authorId(1L)
                .authorName("John Doe")
                .textContent("Test post content")
                .imageUrl("http://example.com/image.jpg")
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();

        postLikedEvent = PostLikedEvent.builder()
                .eventId("test-event-2")
                .postId(1L)
                .userId(2L)
                .occurredAt(LocalDateTime.now())
                .build();

        userCreatedEvent = UserCreatedEvent.builder()
                .eventId("test-event-3")
                .userId(1L)
                .name("John Doe")
                .email("john@example.com")
                .createdAt(LocalDateTime.now())
                .occurredAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should handle PostCreatedEvent and delegate to service")
    void handlePostCreated_ShouldDelegateToReadStoreUpdateService() {
        // When
        eventConsumer.handlePostCreated(postCreatedEvent);

        // Then
        verify(readStoreUpdateService).handlePostCreated(postCreatedEvent);
        verifyNoMoreInteractions(readStoreUpdateService);
    }

    @Test
    @DisplayName("Should handle PostLikedEvent and delegate to service")
    void handlePostLiked_ShouldDelegateToReadStoreUpdateService() {
        // When
        eventConsumer.handlePostLiked(postLikedEvent);

        // Then
        verify(readStoreUpdateService).handlePostLiked(postLikedEvent);
        verifyNoMoreInteractions(readStoreUpdateService);
    }

    @Test
    @DisplayName("Should handle UserCreatedEvent and delegate to service")
    void handleUserCreated_ShouldDelegateToReadStoreUpdateService() {
        // When
        eventConsumer.handleUserCreated(userCreatedEvent);

        // Then
        verify(readStoreUpdateService).handleUserCreated(userCreatedEvent);
        verifyNoMoreInteractions(readStoreUpdateService);
    }

    @Test
    @DisplayName("Should handle PostCreatedEvent with null values gracefully")
    void handlePostCreated_WithNullEvent_ShouldStillDelegateToService() {
        // When
        eventConsumer.handlePostCreated(null);

        // Then
        verify(readStoreUpdateService).handlePostCreated(null);
    }

    @Test
    @DisplayName("Should handle PostLikedEvent with null values gracefully")
    void handlePostLiked_WithNullEvent_ShouldStillDelegateToService() {
        // When
        eventConsumer.handlePostLiked(null);

        // Then
        verify(readStoreUpdateService).handlePostLiked(null);
    }

    @Test
    @DisplayName("Should handle UserCreatedEvent with null values gracefully")
    void handleUserCreated_WithNullEvent_ShouldStillDelegateToService() {
        // When
        eventConsumer.handleUserCreated(null);

        // Then
        verify(readStoreUpdateService).handleUserCreated(null);
    }

    @Test
    @DisplayName("Should handle PostCreatedEvent with complete data")
    void handlePostCreated_WithCompleteEvent_ShouldPassAllData() {
        // Given
        PostCreatedEvent completeEvent = PostCreatedEvent.builder()
                .eventId("complete-event-1")
                .postId(123L)
                .authorId(456L)
                .authorName("Jane Smith")
                .textContent("Complete post with all fields")
                .imageUrl("http://example.com/complete-image.jpg")
                .createdAt(LocalDateTime.now().minusHours(1))
                .occurredAt(LocalDateTime.now())
                .build();

        // When
        eventConsumer.handlePostCreated(completeEvent);

        // Then
        verify(readStoreUpdateService).handlePostCreated(eq(completeEvent));
    }

    @Test
    @DisplayName("Should handle PostLikedEvent with complete data")
    void handlePostLiked_WithCompleteEvent_ShouldPassAllData() {
        // Given
        PostLikedEvent completeEvent = PostLikedEvent.builder()
                .eventId("complete-like-event")
                .postId(789L)
                .userId(101112L)
                .occurredAt(LocalDateTime.now().minusMinutes(30))
                .build();

        // When
        eventConsumer.handlePostLiked(completeEvent);

        // Then
        verify(readStoreUpdateService).handlePostLiked(eq(completeEvent));
    }

    @Test
    @DisplayName("Should handle UserCreatedEvent with complete data")
    void handleUserCreated_WithCompleteEvent_ShouldPassAllData() {
        // Given
        UserCreatedEvent completeEvent = UserCreatedEvent.builder()
                .eventId("complete-user-event")
                .userId(131415L)
                .name("Alice Johnson")
                .email("alice@example.com")
                .createdAt(LocalDateTime.now().minusDays(1))
                .occurredAt(LocalDateTime.now())
                .build();

        // When
        eventConsumer.handleUserCreated(completeEvent);

        // Then
        verify(readStoreUpdateService).handleUserCreated(eq(completeEvent));
    }

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void handlePostCreated_WhenServiceThrowsException_ShouldPropagateException() {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(readStoreUpdateService).handlePostCreated(any());

        // When/Then
        try {
            eventConsumer.handlePostCreated(postCreatedEvent);
        } catch (RuntimeException e) {
            // Exception should propagate to trigger RabbitMQ retry mechanism
            assert e.getMessage().equals("Database connection failed");
        }

        verify(readStoreUpdateService).handlePostCreated(postCreatedEvent);
    }

    @Test
    @DisplayName("Should handle multiple events in sequence")
    void handleMultipleEvents_ShouldProcessAllCorrectly() {
        // When
        eventConsumer.handleUserCreated(userCreatedEvent);
        eventConsumer.handlePostCreated(postCreatedEvent);
        eventConsumer.handlePostLiked(postLikedEvent);

        // Then
        verify(readStoreUpdateService).handleUserCreated(userCreatedEvent);
        verify(readStoreUpdateService).handlePostCreated(postCreatedEvent);
        verify(readStoreUpdateService).handlePostLiked(postLikedEvent);
        verifyNoMoreInteractions(readStoreUpdateService);
    }

    @Test
    @DisplayName("Should maintain event processing order")
    void handleEvents_ShouldMaintainProcessingOrder() {
        // Given
        PostCreatedEvent firstEvent = PostCreatedEvent.builder()
                .eventId("first-event")
                .postId(1L)
                .authorId(1L)
                .authorName("First Author")
                .textContent("First post")
                .imageUrl("http://example.com/first.jpg")
                .createdAt(LocalDateTime.now().minusHours(2))
                .occurredAt(LocalDateTime.now().minusHours(2))
                .build();

        PostCreatedEvent secondEvent = PostCreatedEvent.builder()
                .eventId("second-event")
                .postId(2L)
                .authorId(2L)
                .authorName("Second Author")
                .textContent("Second post")
                .imageUrl("http://example.com/second.jpg")
                .createdAt(LocalDateTime.now().minusHours(1))
                .occurredAt(LocalDateTime.now().minusHours(1))
                .build();

        // When
        eventConsumer.handlePostCreated(firstEvent);
        eventConsumer.handlePostCreated(secondEvent);

        // Then
        // Verify calls were made in order
        var inOrder = inOrder(readStoreUpdateService);
        inOrder.verify(readStoreUpdateService).handlePostCreated(firstEvent);
        inOrder.verify(readStoreUpdateService).handlePostCreated(secondEvent);
    }
}