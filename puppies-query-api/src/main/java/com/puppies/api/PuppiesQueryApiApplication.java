package com.puppies.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Puppies Query API.
 * 
 * This application implements the Query side of CQRS (Command Query Responsibility Segregation)
 * architecture, handling all read operations and data serving.
 * 
 * Key architectural features:
 * - Query side of CQRS (read operations only)
 * - Optimized database queries from read store
 * - Redis caching for improved performance
 * - Denormalized data for fast queries
 * - Feed generation and content serving
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.puppies.api.read.repository")
@EnableTransactionManagement
@EnableCaching
public class PuppiesQueryApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuppiesQueryApiApplication.class, args);
    }
}