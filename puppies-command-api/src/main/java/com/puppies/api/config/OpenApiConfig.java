package com.puppies.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Puppies Command API.
 * 
 * This configuration provides comprehensive API documentation for all command operations
 * including user registration, authentication, post creation, and demo endpoints.
 * 
 * Access the Swagger UI at: http://localhost:8081/swagger-ui.html
 * OpenAPI JSON at: http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI puppiesCommandApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("🐶 Puppies Command API")
                        .description("""
                                **Puppies Social Media Platform - Command API (Write Side)**
                                
                                This is the **Command API** of our CQRS-based social media platform for dog lovers! 🐕
                                
                                ## 🚀 Quick Start Guide
                                
                                **1. Register a new user:**
                                ```
                                POST /api/users
                                {
                                  "name": "Dog Lover",
                                  "email": "doglover@example.com",
                                  "password": "SecurePass123!"
                                }
                                ```
                                
                                **2. Login to get your JWT token:**
                                ```
                                POST /api/auth/token
                                {
                                  "email": "doglover@example.com",
                                  "password": "SecurePass123!"
                                }
                                ```
                                
                                **3. Authorize in Swagger:**
                                - Click the 🔒 **Authorize** button at the top
                                - Enter: `Bearer YOUR_JWT_TOKEN`
                                - Now you can use all protected endpoints!
                                
                                **4. Try demo endpoints (public - no auth needed):**
                                ```
                                GET /api/demo/random-dog-image
                                GET /api/demo/dog-image/breed/golden-retriever
                                GET /api/demo/dog-api/health
                                GET /api/demo/trending/posts
                                GET /api/demo/stats
                                ```
                                
                                **5. Try protected demo endpoints (auth required):**
                                ```
                                POST /api/demo/create-random-dog-post
                                POST /api/demo/create-multiple-posts
                                ```
                                
                                ## 🐕 Demo Features
                                
                                **🟢 Public Demo Endpoints (No Auth Required):**
                                - 🎯 Get random/breed-specific dog images
                                - 🏥 Check external APIs health status
                                - 🔥 Get trending posts/feed from Query API
                                - 📊 Get demo system stats
                                
                                **🔒 Protected Demo Endpoints (Auth Required):**
                                - 🎲 Create posts with random dog images (downloads & saves locally)
                                - 🎪 Create multiple demo posts at once (all saved to uploads folder)
                                
                                ## 🏗️ Architecture
                                
                                This Command API handles:
                                - ✅ User registration and authentication
                                - ✅ Post creation and management  
                                - ✅ Like/Unlike operations
                                - ✅ Event publishing to CQRS read side
                                - ✅ Demo endpoints with external dog APIs
                                
                                **Related Services:**
                                - **Query API**: Port 8082 - Read operations, feeds, search
                                - **Sync Worker**: Port 8083 - Event processing, read store updates
                                
                                ## 🔐 Authentication
                                
                                Most endpoints require JWT authentication. After login:
                                1. Copy the JWT token from login response
                                2. Click the 🔒 **Authorize** button above
                                3. Enter: `Bearer <your-jwt-token>`
                                4. Click **Authorize**
                                
                                **🐕 Image Handling:**
                                - Downloads images from external APIs
                                - Saves locally in uploads/ folder  
                                - Same flow as manual file upload
                                
                                **External APIs Used:**
                                - 🐕 https://dog.ceo/api/breeds/image/random
                                - 🐕 https://random.dog/woof.json
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
                                .url("http://localhost:8081")
                                .description("Local Development Server - Command API"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("Local Development Server - Query API (Read-only)")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from /api/auth/token endpoint. Format: Bearer <token>")));
    }
}