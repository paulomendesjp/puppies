package com.puppies.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Puppies CQRS Sync Worker
 * 
 * This service is responsible for:
 * - Consuming events from RabbitMQ
 * - Updating the read database with denormalized data
 * - Maintaining eventual consistency between command and query stores
 */
@SpringBootApplication
@EnableRabbit
@EnableJpaRepositories
@EnableTransactionManagement
public class PuppiesSyncWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PuppiesSyncWorkerApplication.class, args);
    }
}