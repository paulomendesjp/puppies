package com.puppies.api.common.constants;

/**
 * API constants for the Puppies Query API.
 * Centralized location for all magic numbers and strings.
 */
public final class QueryApiConstants {

    private QueryApiConstants() {
        // Utility class - prevent instantiation
    }

    // Cache Names
    public static final class CacheNames {
        public static final String HOT_POSTS = "hot_posts";
        public static final String WARM_POSTS = "warm_posts";
        public static final String COLD_POSTS = "cold_posts";
        public static final String HOT_FEED = "hot_feed";
        public static final String WARM_FEED = "warm_feed";
        public static final String COLD_FEED = "cold_feed";
        public static final String USER_BEHAVIOR = "user_behavior";
        public static final String POST_CONTENT = "post_content";
        public static final String POST_TOTAL = "post_total";
        public static final String POST = "post";
        public static final String RECENT_TRENDING = "recent_trending";
        public static final String HIGH_ENGAGEMENT = "high_engagement";
        public static final String AUTHOR_POST_COUNT = "author_post_count";
        public static final String POSTS = "posts";
        public static final String USERS = "users";
        public static final String FEED = "feed";
    }

    // Cache Keys and Prefixes
    public static final class CacheKeys {
        public static final String POSTS_PREFIX = "posts";
        public static final String TRENDING_POSTS_PREFIX = "trending_posts";
        public static final String POPULAR_POSTS_PREFIX = "popular_posts";
        public static final String AUTHOR_POSTS_PREFIX = "author_posts_";
        public static final String CONTENT_SUFFIX = "_content_";
        public static final String TOTAL_SUFFIX = "_total";
        public static final String POST_FORMAT = "post:%d:user:%s";
        public static final String FEED_FORMAT = "feed:%s:user:%d:engagement:%s";
        public static final String ANONYMOUS_USER = "anonymous";
    }

    // Business Rules
    public static final class BusinessRules {
        public static final int DEFAULT_PAGE_SIZE = 10;
        public static final int DEFAULT_PAGE_NUMBER = 0;
        public static final int DEFAULT_TRENDING_LIMIT = 10;
        public static final int DEFAULT_ACTIVE_USERS_LIMIT = 10;
        public static final long DEFAULT_PROLIFIC_THRESHOLD = 5L;
        public static final int TRENDING_POSTS_LIMIT = 50;
        public static final int MAX_SIMULATION_REQUESTS = 1000;
        public static final long CACHE_WARMING_INTERVAL_MS = 300000L; // 5 minutes
        public static final int CACHE_CLEANUP_HOURS = 24;
        public static final int HOT_CACHE_VIEWS_THRESHOLD = 100;
        public static final int HOT_CACHE_VIEWERS_THRESHOLD = 50;
        public static final double HOT_CACHE_ENGAGEMENT_THRESHOLD = 0.1;
        public static final int WARM_CACHE_VIEWS_THRESHOLD = 20;
        public static final int WARM_CACHE_VIEWERS_THRESHOLD = 10;
        public static final double WARM_CACHE_ENGAGEMENT_THRESHOLD = 0.05;
    }

    // HTTP Status Messages
    public static final class ErrorMessages {
        public static final String AUTHENTICATION_REQUIRED = "Authentication required";
        public static final String PROVIDE_VALID_JWT = "Please provide a valid JWT token";
        public static final String USER_NOT_FOUND = "User not found";
        public static final String USER_PROFILE_NOT_FOUND = "User profile not found for email: ";
        public static final String INTERNAL_SERVER_ERROR = "Internal server error";
        public static final String FAILED_TO_RETRIEVE_USER_POSTS = "Failed to retrieve user posts";
        public static final String CACHE_LAYER_NOT_FOUND = "Cache layer not found: ";
        public static final String FAILED_TO_CLEAR_CACHE = "Failed to clear cache layer";
        public static final String FAILED_TO_GET_CACHE_STATS = "Failed to get cache statistics";
        public static final String FAILED_TO_ANALYZE_USER_CACHE = "Failed to analyze user cache behavior";
        public static final String FAILED_TO_GET_POST_METRICS = "Failed to get post metrics";
        public static final String FAILED_TO_WARM_CACHE = "Failed to warm cache";
        public static final String FAILED_TO_GET_CACHE_INSIGHTS = "Failed to get cache insights";
        public static final String FAILED_TO_SIMULATE_CACHE_LOAD = "Failed to simulate cache load";
        public static final String MAX_SIMULATION_REQUESTS_EXCEEDED = "Maximum 1000 requests allowed for simulation";
    }

    // Success Messages
    public static final class SuccessMessages {
        public static final String CACHE_WARMING_TRIGGERED = "Cache warming triggered successfully";
        public static final String CACHE_LAYER_CLEARED = "Cache layer cleared: ";
        public static final String CACHE_WARMING_COMPLETED = "Cache warming completed. Warmed %d trending posts";
        public static final String SIMULATION_COMPLETED = "Simulated %d requests for %d users and %d posts";
    }

    // Cache Layer Descriptions
    public static final class CacheDescriptions {
        public static final String HOT_POSTS_DESC = "Trending/viral content (30min TTL)";
        public static final String WARM_POSTS_DESC = "Popular content (15min TTL)";
        public static final String COLD_POSTS_DESC = "Less popular content (5min TTL)";
        public static final String HOT_FEED_DESC = "High engagement user feeds (10min TTL)";
        public static final String WARM_FEED_DESC = "Standard user feeds (5min TTL)";
        public static final String COLD_FEED_DESC = "Low engagement user feeds (2min TTL)";
        public static final String USER_BEHAVIOR_DESC = "User behavior profiles (1hour TTL)";
        public static final String POSTS_DESC = "Legacy post cache (5min TTL)";
        public static final String USERS_DESC = "Legacy user cache (15min TTL)";
        public static final String FEED_DESC = "Legacy feed cache (2min TTL)";
    }

    // Cache TTLs in minutes
    public static final class CacheTtl {
        public static final int HOT_CACHE_MINUTES = 30;
        public static final int WARM_CACHE_MINUTES = 15;
        public static final int COLD_CACHE_MINUTES = 5;
        public static final int USER_BEHAVIOR_MINUTES = 60;
        public static final int HIGH_ENGAGEMENT_FEED_MINUTES = 10;
        public static final int STANDARD_FEED_MINUTES = 5;
        public static final int LOW_ENGAGEMENT_FEED_MINUTES = 2;
    }

    // API URLs and Patterns
    public static final class ApiUrls {
        // Public endpoints
        public static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/posts", "/api/posts/{id}", "/api/posts/trending", 
            "/api/posts/popular", "/api/posts/search", "/api/posts/author/{authorId}"
        };
        
        public static final String[] PUBLIC_FEED_ENDPOINTS = {
            "/api/feed/trending", "/api/feed/discover"
        };
        
        public static final String[] PUBLIC_USER_ENDPOINTS = {
            "/api/users/top-creators", "/api/users/top-liked", "/api/users/most-active",
            "/api/users/search", "/api/users/prolific"
        };
        
        public static final String[] PUBLIC_CACHE_ENDPOINTS = {
            "/api/cache/**"
        };
        
        public static final String[] PUBLIC_SYSTEM_ENDPOINTS = {
            "/actuator/**", "/error"
        };
        
        public static final String[] PUBLIC_SWAGGER_ENDPOINTS = {
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", 
            "/v3/api-docs.yaml", "/swagger-resources/**", "/webjars/**"
        };
        
        // Authenticated endpoints
        public static final String[] AUTHENTICATED_ENDPOINTS = {
            "/api/posts/my-posts", "/api/feed/user/{userId}", 
            "/api/feed/user/{userId}/**", "/api/users/{userId}", 
            "/api/users/email/{email}"
        };
        
        // CORS Origins
        public static final String[] CORS_ORIGIN_PATTERNS = {
            "http://localhost:*", "http://127.0.0.1:*", "https://localhost:*"
        };
    }

    // Log Messages
    public static final class LogMessages {
        public static final String CACHE_HIT_POSTS = "📦 CACHE HIT - Using cached posts: page={}, size={}, total={}";
        public static final String CACHE_MISS_POSTS = "📦 CACHE MISS - Loading posts from DB: page={}, size={}";
        public static final String CACHE_STORE_POSTS = "📦 CACHE STORE - Loaded {} posts for page {}, total={}";
        public static final String CACHE_HIT_TRENDING = "🔥 CACHE HIT - Using cached trending posts: page={}, size={}, total={}";
        public static final String CACHE_MISS_TRENDING = "🔥 CACHE MISS - Loading trending posts from DB: page={}, size={}";
        public static final String CACHE_STORE_TRENDING = "🔥 CACHE STORE - Loaded {} trending posts for page {}, total={}";
        public static final String CACHE_HIT_POPULAR = "❤️ CACHE HIT - Using cached popular posts: page={}, size={}, total={}";
        public static final String CACHE_MISS_POPULAR = "❤️ CACHE MISS - Loading popular posts from DB: page={}, size={}";
        public static final String CACHE_STORE_POPULAR = "❤️ CACHE STORE - Loaded {} popular posts for page {}, total={}";
        public static final String CACHE_HIT_AUTHOR = "👤 CACHE HIT - Using cached author posts: authorId={}, page={}, size={}, total={}";
        public static final String CACHE_MISS_AUTHOR = "👤 CACHE MISS - Loading posts by author from DB: authorId={}, page={}, size={}";
        public static final String CACHE_STORE_AUTHOR = "👤 CACHE STORE - Loaded {} posts for author {}, total={}";
        public static final String CACHE_MISS_POST = "📝 CACHE MISS - Loading post from DB: id={}";
        public static final String CACHE_STORE_POST = "📝 CACHE STORE - Loaded post: id={}, title={}";
        public static final String CACHE_STORE_POST_NOT_FOUND = "📝 CACHE STORE - Post not found: id={}";
        public static final String CACHE_WARMING_START = "🔥 Starting cache warming process...";
        public static final String CACHE_WARMING_SUCCESS = "✅ Cache warming completed. Warmed {} trending posts";
        public static final String CACHE_WARMING_ERROR = "❌ Error during cache warming";
        public static final String CACHE_CLEARED = "🧹 Cleared cache layer: {}";
        public static final String CACHE_CLEANUP = "🧹 Cleaned up old cache metrics";
    }

    // Health Status
    public static final class HealthStatus {
        public static final String EXCELLENT = "EXCELLENT";
        public static final String GOOD = "GOOD";
        public static final String NEEDS_ATTENTION = "NEEDS_ATTENTION";
        public static final String NEEDS_OPTIMIZATION = "NEEDS_OPTIMIZATION";
    }
}