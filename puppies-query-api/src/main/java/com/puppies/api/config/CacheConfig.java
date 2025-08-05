package com.puppies.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Cache configuration for Redis.
 * 
 * This configuration sets up Redis as the cache provider with different
 * TTL (Time To Live) values for different types of cached data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure ObjectMapper for Redis serialization with LocalDateTime and complex type support.
     */
    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Support for LocalDateTime
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Enable type information to preserve complex types like Page
        objectMapper.activateDefaultTyping(
            LaissezFaireSubTypeValidator.instance,
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        // Configure for better compatibility
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        return objectMapper;
    }

    /**
     * Configure Redis cache manager with custom TTL for different cache regions.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper redisObjectMapper) {
        
        // Default cache configuration with properly configured ObjectMapper
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        // Custom configurations for specific cache regions
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // ===== PAGE CACHE CONFIGURATIONS =====
        // Separate caches for Page components to avoid Redis serialization issues
        cacheConfigurations.put("post_content", defaultConfig
                .entryTtl(Duration.ofMinutes(10))); // Cache for List<ReadPost>
        cacheConfigurations.put("post_total", defaultConfig
                .entryTtl(Duration.ofMinutes(15))); // Cache for Long totals
        cacheConfigurations.put("feed_content", defaultConfig
                .entryTtl(Duration.ofMinutes(8))); // Cache for List<ReadFeedItem>
        cacheConfigurations.put("feed_total", defaultConfig
                .entryTtl(Duration.ofMinutes(12))); // Cache for feed totals
        
        // Legacy cache configurations (kept for backward compatibility)
        cacheConfigurations.put("posts", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("users", defaultConfig
                .entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("feed", defaultConfig
                .entryTtl(Duration.ofMinutes(2)));
        
        // 🚀 INTELLIGENT CACHE LAYERS for different content hotness levels
        
        // Hot cache - trending/viral content (highest priority)
        cacheConfigurations.put("hot_posts", defaultConfig
                .entryTtl(Duration.ofMinutes(30))  // Longer TTL for hot content
                .disableCachingNullValues());       // Don't cache nulls for hot content
        
        // Warm cache - moderately popular content
        cacheConfigurations.put("warm_posts", defaultConfig
                .entryTtl(Duration.ofMinutes(15))  // Standard TTL
                .disableCachingNullValues());
        
        // Cold cache - less popular content (shorter TTL to save memory)
        cacheConfigurations.put("cold_posts", defaultConfig
                .entryTtl(Duration.ofMinutes(5))   // Shorter TTL for cold content
                .disableCachingNullValues());
        
        // User behavior cache - personalized caching profiles
        cacheConfigurations.put("user_behavior", defaultConfig
                .entryTtl(Duration.ofHours(1))     // Longer TTL for user patterns
                .disableCachingNullValues());
        
        // Feed caches with different engagement levels
        cacheConfigurations.put("hot_feed", defaultConfig
                .entryTtl(Duration.ofMinutes(10))); // Active users get longer feed cache
        cacheConfigurations.put("warm_feed", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));  // Standard users
        cacheConfigurations.put("cold_feed", defaultConfig
                .entryTtl(Duration.ofMinutes(2)));  // Inactive users get minimal cache

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Configure RedisTemplate for direct Redis operations.
     * Used by IntelligentCacheService for cache size calculations and custom operations.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Set serializers for keys and values with properly configured ObjectMapper
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));
        
        // Enable transaction support
        template.setEnableTransactionSupport(true);
        
        return template;
    }
}