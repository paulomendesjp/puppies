package com.puppies.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Puppies Query API.
 * 
 * This configuration provides comprehensive API documentation for all query operations
 * including feed retrieval, post search, and user profiles.
 * 
 * Access the Swagger UI at: http://localhost:8082/swagger-ui.html
 * OpenAPI JSON at: http://localhost:8082/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI puppiesQueryApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🔍 Puppies Query API")
                        .description("""
                                **Puppies Social Media Platform - Query API (Read Side)**
                                
                                This is the **Query API** of our CQRS-based social media platform for dog lovers! 🐕
                                
                                ## 🚀 Available Endpoints
                                
                                **📝 Posts:**
                                - `GET /api/posts` - All posts (paginated)
                                - `GET /api/posts/{id}` - Single post details
                                - `GET /api/posts/trending` - Trending posts 🔥 *(uses cache)*
                                - `GET /api/posts/popular` - Most liked posts ❤️
                                - `GET /api/posts/author/{authorId}` - Posts by specific author (using ID)
                                - `GET /api/posts/my-posts` - **🆕 My Posts** (using JWT token) 🔐
                                - `GET /api/posts/search?q=keyword` - Search posts 🔍
                                
                                **📱 Feeds:**
                                - `GET /api/feed/user/{userId}` - User's personalized feed *(cached)*
                                - `GET /api/feed/user/{userId}/popular` - User's popular feed
                                - `GET /api/feed/user/{userId}/liked` - Posts user liked ❤️ *(cached)*
                                - `GET /api/feed/trending` - Global trending feed 🌍 *(cached)*
                                - `GET /api/feed/discover` - Discovery feed for new content
                                
                                **👤 User Profiles:**
                                - `GET /api/users/{userId}` - User profile with stats *(cached)*
                                - `GET /api/users/email/{email}` - Find user by email *(cached)*
                                - `GET /api/users/top-creators` - Top content creators 🏆 *(cached)*
                                - `GET /api/users/top-liked` - Most liked users ❤️ *(cached)*
                                - `GET /api/users/most-active` - Most active users 🔥 *(cached)*
                                - `GET /api/users/search?q=name` - Search users by name 🔍
                                - `GET /api/users/prolific?threshold=5` - Users with many posts 📝 *(cached)*
                                
                                **📊 Cache & Monitoring:**
                                - `GET /api/cache/stats` - Cache performance metrics
                                - `GET /api/cache/clear` - Clear specific cache
                                
                                ## ⚡ Performance Features
                                
                                This Query API includes:
                                - 🚀 **Intelligent Multi-layer Caching** (Hot/Warm/Cold)
                                - 📊 **Real-time Analytics & Metrics**
                                - 🎯 **Personalized Content Delivery**
                                - 🔍 **Advanced Search & Filtering**
                                - 📈 **Trending Content Detection**
                                
                                ## 🏗️ Architecture
                                
                                This Query API handles:
                                - ✅ Feed generation and retrieval
                                - ✅ Post search and filtering
                                - ✅ User profile views
                                - ✅ Trending content analysis
                                - ✅ Cache optimization
                                
                                **Related Services:**
                                - **Command API**: Port 8081 - Write operations (register, create posts)
                                - **Sync Worker**: Port 8083 - Event processing, read store updates
                                
                                ## 🔐 Authentication
                                
                                Most endpoints require JWT authentication. Get your token from the Command API:
                                ```
                                POST http://localhost:8081/api/v1/auth/login
                                ```
                                
                                Then include it in requests:
                                ```
                                Authorization: Bearer <your-jwt-token>
                                ```
                                
                                ## 📊 Cache Performance Logging
                                
                                **Watch the logs to see cache in action!** 🎯
                                
                                When you call endpoints marked with *(cached)*, you'll see logs like:
                                ```
                                🔥 CACHE MISS - Loading trending posts from DB: page=0, size=10
                                🔥 CACHE STORE - Loaded 10 trending posts for page 0
                                ```
                                
                                **To test cache hits:**
                                1. Call any cached endpoint twice
                                2. First call = CACHE MISS (loads from DB)
                                3. Second call = CACHE HIT (no logs = cached!)
                                
                                **Best endpoints to test cache:**
                                - `GET /api/posts/trending` - Most frequently cached
                                - `GET /api/users/{userId}` - User profile cache
                                - `GET /api/feed/user/{userId}` - Feed cache
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Puppies Development Team")
                                .email("dev@puppies.com")
                                .url("https://github.com/puppies/social-platform"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local Development Server - Query API (Read-only)"),
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local Development Server - Command API (Write operations)")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from Command API: POST http://localhost:8081/api/v1/auth/login")));
    }

    /**
     * Ensure all our controllers are properly scanned
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("puppies-query-api")
                .packagesToScan(
                    "com.puppies.api.read.controller",  // Query controllers
                    "com.puppies.api.cache"             // Cache controllers
                )
                .build();
    }
}