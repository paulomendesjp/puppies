package com.puppies.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Puppies Command API.
 * 
 * This application implements the Command side of CQRS (Command Query Responsibility Segregation)
 * architecture, handling all write operations and business logic.
 * 
 * Key architectural features:
 * - Command side of CQRS (write operations only)
 * - JWT-based stateless authentication  
 * - Event publishing to RabbitMQ
 * - Business logic and validation
 * - Write database operations
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableTransactionManagement
public class PuppiesCommandApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuppiesCommandApiApplication.class, args);
    }
}