package com.puppies.sync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the Sync Worker
 * Configures the queues and message converters for consuming events
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    /**
     * Note: Queue definitions are managed by the Command API.
     * This service only consumes from existing queues using @RabbitListener.
     * 
     * The Command API creates all queues with proper Dead Letter Exchange configuration.
     */

    /**
     * JSON message converter for deserializing events with JSR310 support
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        return converter;
    }

    /**
     * Rabbit listener container factory with JSON converter
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}