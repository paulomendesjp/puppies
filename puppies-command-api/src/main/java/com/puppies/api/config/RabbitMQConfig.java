package com.puppies.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for domain event publishing and consumption.
 * 
 * This configuration sets up the exchanges, queues, and bindings needed
 * for event-driven communication between write and read stores.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String DOMAIN_EVENTS_EXCHANGE = "puppies.domain.events";
    
    // Queue names
    public static final String POST_CREATED_QUEUE = "puppies.post.created.queue";
    public static final String POST_LIKED_QUEUE = "puppies.post.liked.queue";
    public static final String POST_UNLIKED_QUEUE = "puppies.post.unliked.queue";
    public static final String USER_CREATED_QUEUE = "puppies.user.created.queue";
    
    // Routing keys
    public static final String POST_EVENTS_ROUTING_KEY = "post.events";
    public static final String USER_EVENTS_ROUTING_KEY = "user.events";
    
    // Dead letter queue for failed events
    public static final String DEAD_LETTER_EXCHANGE = "puppies.dlx";
    public static final String DEAD_LETTER_QUEUE = "puppies.dead.letter.queue";

    /**
     * Main topic exchange for domain events.
     */
    @Bean
    public TopicExchange domainEventsExchange() {
        return ExchangeBuilder
                .topicExchange(DOMAIN_EVENTS_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Dead letter exchange for failed events.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * Queue for post created events.
     */
    @Bean
    public Queue postCreatedQueue() {
        return QueueBuilder
                .durable(POST_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "post.created.failed")
                .build();
    }

    /**
     * Queue for post liked events.
     */
    @Bean
    public Queue postLikedQueue() {
        return QueueBuilder
                .durable(POST_LIKED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "post.liked.failed")
                .build();
    }

    /**
     * Queue for post unliked events.
     */
    @Bean
    public Queue postUnlikedQueue() {
        return QueueBuilder
                .durable(POST_UNLIKED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "post.unliked.failed")
                .build();
    }

    /**
     * Queue for user created events.
     */
    @Bean
    public Queue userCreatedQueue() {
        return QueueBuilder
                .durable(USER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "user.created.failed")
                .build();
    }

    /**
     * Dead letter queue for failed events.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * Bind post events to appropriate queues.
     */
    @Bean
    public Binding postCreatedBinding() {
        return BindingBuilder
                .bind(postCreatedQueue())
                .to(domainEventsExchange())
                .with("post.events.created");
    }

    @Bean
    public Binding postLikedBinding() {
        return BindingBuilder
                .bind(postLikedQueue())
                .to(domainEventsExchange())
                .with("post.events.liked");
    }

    @Bean
    public Binding postUnlikedBinding() {
        return BindingBuilder
                .bind(postUnlikedQueue())
                .to(domainEventsExchange())
                .with("post.events.unliked");
    }

    /**
     * Bind user events to appropriate queues.
     */
    @Bean
    public Binding userCreatedBinding() {
        return BindingBuilder
                .bind(userCreatedQueue())
                .to(domainEventsExchange())
                .with("user.events.created");
    }

    /**
     * JSON message converter for event serialization with JSR310 support.
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        return converter;
    }

    /**
     * RabbitTemplate with JSON converter.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true); // Ensure messages are routed
        return template;
    }

    /**
     * Listener container factory with JSON converter.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setDefaultRequeueRejected(false); // Send to DLQ on failure
        factory.setConcurrentConsumers(2); // Parallel processing
        factory.setMaxConcurrentConsumers(5);
        return factory;
    }
}