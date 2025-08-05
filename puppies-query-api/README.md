# Puppies Query API

The **Query API** handles all read operations in the CQRS architecture. This service is optimized for high-performance queries with intelligent caching and denormalized data structures.

## 🎯 Responsibilities

- User profile queries
- Post feed generation
- Search and filtering operations
- Real-time metrics (likes, comments, views)
- Intelligent caching with user behavior analysis

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Running infrastructure (see main README)
- Sync Worker running (for data synchronization)

### Running the Service

```bash
# From puppies-ecosystem directory
cd puppies-query-api

# Install dependencies and run
mvn clean install
mvn spring-boot:run
```

The service will start on **http://localhost:8082**

### API Documentation
Once running, visit: http://localhost:8082/swagger-ui.html

## 🔧 Configuration

### Database Connection
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/puppies_read
    username: admin
    password: admin123
```

### Key Features
- **Port**: 8082
- **Database**: PostgreSQL Read Store (port 5433)
- **Cache**: Redis with intelligent strategies
- **Security**: JWT-based authentication
- **Performance**: Optimized for read operations

## 📝 Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/{id}/profile` | Get user profile |
| GET | `/api/posts/feed` | Get personalized feed |
| GET | `/api/posts/{id}` | Get post details |
| GET | `/api/posts/search` | Search posts |
| GET | `/api/cache/metrics` | Cache performance metrics |
| POST | `/api/cache/invalidate` | Invalidate cache entries |

## 🚀 Performance Features

### Intelligent Caching
- **Hot Posts Cache**: Frequently accessed posts
- **User Behavior Cache**: Personalized recommendations
- **Feed Cache**: Pre-calculated user feeds

### Cache Strategies
```java
// Example cache configuration
@Cacheable(value = "posts", key = "#postId")
public ReadPost getPost(Long postId) { ... }

@Cacheable(value = "feed", key = "#userId + '_' + #page")
public List<ReadFeedItem> getUserFeed(Long userId, int page) { ... }
```

### Read Store Optimizations
- Denormalized data with pre-calculated counters
- Query-specific database indexes
- Optimized for complex feed queries

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn test -Dtest=**/*IntegrationTest

# Performance tests
mvn test -Dtest=**/*PerformanceTest
```

## 🔍 Monitoring

### Health Checks
- Application: http://localhost:8082/actuator/health
- Database: http://localhost:8082/actuator/health/db
- Redis: http://localhost:8082/actuator/health/redis

### Cache Metrics
```bash
# Get cache hit rates
curl http://localhost:8082/api/cache/metrics

# Example response
{
  "hitRate": 0.85,
  "missRate": 0.15,
  "evictionCount": 12,
  "averageLoadTime": "45ms"
}
```

## 📁 Project Structure

```
src/main/java/com/puppies/api/
├── read/
│   ├── controller/     # Query controllers
│   ├── service/       # Query business logic
│   ├── model/         # Read-optimized models
│   └── repository/    # Read repositories
├── cache/
│   ├── strategy/      # Cache strategies
│   ├── metrics/       # Cache monitoring
│   └── IntelligentCacheService.java
└── config/           # Configuration classes
```

## 📊 Cache Strategy Details

### User Behavior Analysis
The system analyzes user behavior to optimize caching:

```java
@Component
public class UserBehaviorCacheStrategy implements CacheStrategy {
    
    // Analyze user patterns
    public void analyzeUserBehavior(Long userId) {
        UserCacheProfile profile = buildUserProfile(userId);
        optimizeCacheForUser(profile);
    }
}
```

### Cache Invalidation
- **Event-driven**: Automatic invalidation on data changes
- **TTL-based**: Time-based expiration for different content types
- **Manual**: Administrative cache management endpoints

## 🐛 Troubleshooting

### Common Issues

1. **Empty Results from Queries**
   ```bash
   # Check if Sync Worker is running and processing events
   curl http://localhost:8083/actuator/health
   
   # Check RabbitMQ message processing
   curl http://localhost:15672/api/overview (guest/guest)
   ```

2. **Cache Miss Rate Too High**
   ```bash
   # Check Redis connection
   docker ps | grep puppies-redis
   
   # Analyze cache metrics
   curl http://localhost:8082/api/cache/metrics
   ```

3. **Slow Query Performance**
   ```sql
   -- Check database indexes
   SELECT schemaname, tablename, indexname 
   FROM pg_indexes 
   WHERE tablename LIKE 'read_%';
   ```

### Performance Tuning

1. **Database Optimization**
   ```sql
   -- Example indexes for common queries
   CREATE INDEX idx_read_posts_author_created 
   ON read_posts(author_id, created_at DESC);
   
   CREATE INDEX idx_read_posts_popularity 
   ON read_posts(popularity_score DESC, created_at DESC);
   ```

2. **Cache Configuration**
   ```yaml
   spring:
     cache:
       redis:
         time-to-live: 600000  # 10 minutes
   ```

## 🔄 Development Workflow

1. **Add Query**: Implement in appropriate service layer
2. **Optimize**: Add appropriate caching strategy
3. **Index**: Ensure database indexes support the query
4. **Test Performance**: Verify query performance under load
5. **Monitor**: Add metrics for query patterns

## 📈 Performance Benchmarks

| Operation | Target Response Time | Cache Hit Rate |
|-----------|---------------------|----------------|
| User Profile | < 50ms | > 90% |
| Post Feed | < 100ms | > 80% |
| Post Search | < 200ms | > 70% |
| Post Details | < 30ms | > 95% |