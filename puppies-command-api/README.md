# Puppies Command API

The **Command API** handles all write operations in the CQRS architecture. This service is responsible for data mutations, business logic validation, and publishing domain events.

## 🎯 Responsibilities

- User registration and authentication
- Post creation with image uploads
- Like/Unlike operations
- File storage management
- Domain event publishing via RabbitMQ

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Running infrastructure (see main README)

### Running the Service

```bash
# From puppies-ecosystem directory
cd puppies-command-api

# Install dependencies and run
mvn clean install
mvn spring-boot:run
```

The service will start on **http://localhost:8081**

### API Documentation
Once running, visit: http://localhost:8081/swagger-ui.html

## 🔧 Configuration

### Database Connection
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/puppies_write
    username: admin
    password: admin123
```

### Key Features
- **Port**: 8081
- **Database**: PostgreSQL Write Store (port 5432)
- **Message Broker**: RabbitMQ (publishes events)
- **File Storage**: Local uploads directory
- **Security**: JWT-based authentication

## 📝 Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create new user |
| POST | `/api/auth/login` | User authentication |
| POST | `/api/posts` | Create new post |
| POST | `/api/posts/{id}/like` | Like a post |
| DELETE | `/api/posts/{id}/like` | Unlike a post |
| POST | `/api/files/upload` | Upload image file |

## 🧪 Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn test -Dtest=**/*IntegrationTest
```

## 🔍 Monitoring

- Health check: http://localhost:8081/actuator/health
- Metrics: http://localhost:8081/actuator/metrics

## 📁 Project Structure

```
src/main/java/com/puppies/api/command/
├── controller/          # REST controllers
├── service/            # Business logic
├── dto/               # Data transfer objects
└── config/            # Configuration classes

src/main/java/com/puppies/api/
├── data/              # JPA entities and repositories
├── event/             # Domain events and publisher
├── security/          # JWT and security config
└── exception/         # Exception handling
```

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   ```bash
   # Check if write database is running
   docker ps | grep puppies-postgres-write
   ```

2. **RabbitMQ Connection Issues**
   ```bash
   # Check RabbitMQ status
   docker ps | grep puppies-rabbitmq
   ```

3. **File Upload Issues**
   - Ensure `./uploads` directory exists and is writable
   - Check `file.upload-dir` configuration in `application.yml`

### Logs
```bash
# View application logs with debug level
mvn spring-boot:run -Dspring.profiles.active=dev
```

## 🔄 Development Workflow

1. **Create Feature**: Implement in appropriate service layer
2. **Add Tests**: Include unit and integration tests
3. **Event Publishing**: Add domain events for state changes
4. **API Documentation**: Update OpenAPI annotations
5. **Test Integration**: Verify with Query API and Sync Worker