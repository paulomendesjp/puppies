package com.puppies.api.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuration for multiple datasources in CQRS architecture.
 * 
 * This configuration is activated when the "cqrs.separated-stores" property is true.
 * It sets up separate datasources for read and write operations with their own
 * Flyway migrations.
 * 
 * Usage:
 * - Set "cqrs.separated-stores=true" in application.yml
 * - Configure write and read datasource properties
 * - Deploy with docker-compose-cqrs-separated.yml
 */
@Configuration
@ConditionalOnProperty(name = "cqrs.separated-stores", havingValue = "true")
public class MultiDatasourceConfig {

    /**
     * Primary datasource for write operations.
     * This is the normalized database where commands modify data.
     */
    @Primary
    @Bean(name = "writeDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.write")
    public DataSource writeDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Secondary datasource for read operations.
     * This is the denormalized database optimized for queries.
     */
    @Bean(name = "readDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.read")
    public DataSource readDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Flyway configuration for write store migrations.
     */
    @Bean(name = "writeStoreFlyway")
    public Flyway writeStoreFlyway(@Qualifier("writeDataSource") DataSource writeDataSource) {
        return Flyway.configure()
                .dataSource(writeDataSource)
                .locations("classpath:db/migration/write")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();
    }

    /**
     * Flyway configuration for read store migrations.
     */
    @Bean(name = "readStoreFlyway")
    public Flyway readStoreFlyway(@Qualifier("readDataSource") DataSource readDataSource) {
        return Flyway.configure()
                .dataSource(readDataSource)
                .locations("classpath:db/migration/read")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();
    }

    /**
     * Migrate both databases on startup.
     */
    @Bean
    public FlywayMigrationRunner flywayMigrationRunner(
            @Qualifier("writeStoreFlyway") Flyway writeStoreFlyway,
            @Qualifier("readStoreFlyway") Flyway readStoreFlyway) {
        
        return new FlywayMigrationRunner(writeStoreFlyway, readStoreFlyway);
    }

    /**
     * Helper class to run migrations for both stores.
     */
    public static class FlywayMigrationRunner {
        
        public FlywayMigrationRunner(Flyway writeStoreFlyway, Flyway readStoreFlyway) {
            // Run write store migrations first
            writeStoreFlyway.migrate();
            
            // Then run read store migrations
            readStoreFlyway.migrate();
        }
    }
}