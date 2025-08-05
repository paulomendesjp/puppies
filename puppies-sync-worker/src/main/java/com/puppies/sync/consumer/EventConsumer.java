package com.puppies.sync.consumer;

import com.puppies.sync.event.PostCreatedEvent;
import com.puppies.sync.event.PostLikedEvent;
import com.puppies.sync.event.UserCreatedEvent;
import com.puppies.sync.service.ReadStoreUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ event consumer that listens to events from Command API
 * and updates the Read database accordingly
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final ReadStoreUpdateService readStoreUpdateService;

    @RabbitListener(queues = "puppies.post.created.queue")
    public void handlePostCreated(PostCreatedEvent event) {
        log.info("🔄 Processing PostCreatedEvent: {}", event.getPostId());
        readStoreUpdateService.handlePostCreated(event);
    }

    @RabbitListener(queues = "puppies.post.liked.queue")
    public void handlePostLiked(PostLikedEvent event) {
        log.info("❤️ Processing PostLikedEvent: {}", event.getPostId());
        readStoreUpdateService.handlePostLiked(event);
    }

    @RabbitListener(queues = "puppies.user.created.queue")
    public void handleUserCreated(UserCreatedEvent event) {
        log.info("👤 Processing UserCreatedEvent: {}", event.getUserId());
        readStoreUpdateService.handleUserCreated(event);
    }
}