# Puppies API Development Makefile

.PHONY: help setup start stop clean test build run logs

# Default target
help:
	@echo "Puppies API Development Commands"
	@echo "================================"
	@echo "setup     - Initial project setup (start infrastructure)"
	@echo "start     - Start infrastructure services (PostgreSQL, Redis, RabbitMQ)"
	@echo "stop      - Stop infrastructure services"
	@echo "clean     - Clean and stop all services"
	@echo "test      - Run all tests"
	@echo "test-unit - Run unit tests only"
	@echo "test-integration - Run integration tests only"
	@echo "build     - Build the application"
	@echo "run       - Run the application"
	@echo "dev       - Start infrastructure and run application"
	@echo "logs      - Show application logs"
	@echo "db-logs   - Show database logs"
	@echo "redis-logs - Show Redis logs"
	@echo ""
	@echo "Database Migrations (Flyway)"
	@echo "============================="
	@echo "migrate         - Run pending migrations"
	@echo "migrate-info    - Show migration status"
	@echo "migrate-validate - Validate migrations"
	@echo "migrate-clean   - Clean database (DEV ONLY)"
	@echo ""
	@echo "CQRS with Separated Stores"
	@echo "=========================="
	@echo "cqrs-start - Start CQRS infrastructure (separated read/write DBs)"
	@echo "cqrs-stop  - Stop CQRS infrastructure"
	@echo ""
	@echo "RabbitMQ Events Testing"
	@echo "======================="
	@echo "test-events - Test RabbitMQ event publishing and processing"
	@echo "rabbitmq-ui - Open RabbitMQ Management UI"

# Setup project for first time
setup: start
	@echo "🚀 Setting up Puppies API..."
	@echo "✅ Infrastructure services started"
	@echo "💡 Next steps:"
	@echo "   1. Run 'make run' to start the application"
	@echo "   2. Test with 'curl http://localhost:8080/actuator/health'"

# Start infrastructure services
start:
	@echo "🐕 Starting Puppies API infrastructure..."
	@docker-compose up -d
	@echo "⏳ Waiting for services to be ready..."
	@sleep 10
	@docker-compose ps

# Stop infrastructure services
stop:
	@echo "🛑 Stopping infrastructure services..."
	@docker-compose down

# Clean everything
clean:
	@echo "🧹 Cleaning up..."
	@docker-compose down -v
	@docker system prune -f
	@./mvnw clean

# Run all tests
test:
	@echo "🧪 Running all tests..."
	@./mvnw test

# Run unit tests only
test-unit:
	@echo "🔬 Running unit tests..."
	@./mvnw test -Dtest="**/*Test"

# Run integration tests only
test-integration:
	@echo "🌐 Running integration tests..."
	@./mvnw test -Dtest="**/*IntegrationTest"

# Build the application
build:
	@echo "🔨 Building application..."
	@./mvnw clean package -DskipTests

# Run the application
run:
	@echo "🚀 Starting Puppies API application..."
	@./mvnw spring-boot:run

# Complete development setup
dev: start
	@echo "🚀 Starting development environment..."
	@sleep 5
	@./mvnw spring-boot:run

# Show application logs
logs:
	@echo "📋 Application logs..."
	@tail -f logs/spring.log 2>/dev/null || echo "No log file found. Application may not be running."

# Show database logs
db-logs:
	@echo "📋 PostgreSQL logs..."
	@docker-compose logs -f postgres

# Show Redis logs
redis-logs:
	@echo "📋 Redis logs..."
	@docker-compose logs -f redis

# Database migrations
migrate:
	@echo "🗄️  Running database migrations..."
	@./mvnw flyway:migrate

migrate-info:
	@echo "📊 Migration information..."
	@./mvnw flyway:info

migrate-validate:
	@echo "✅ Validating migrations..."
	@./mvnw flyway:validate

migrate-clean:
	@echo "🧹 Cleaning database (DEVELOPMENT ONLY)..."
	@./mvnw flyway:clean

# CQRS separated stores
cqrs-start:
	@echo "🏗️  Starting CQRS infrastructure with separated stores..."
	@docker-compose -f docker-compose-cqrs-separated.yml up -d
	@echo "⏳ Waiting for services to be ready..."
	@sleep 15
	@docker-compose -f docker-compose-cqrs-separated.yml ps

cqrs-stop:
	@echo "🛑 Stopping CQRS infrastructure..."
	@docker-compose -f docker-compose-cqrs-separated.yml down

# Test RabbitMQ events
test-events:
	@echo "🐰 Testing RabbitMQ events..."
	@./test-rabbitmq-events.sh

# Monitor RabbitMQ
rabbitmq-ui:
	@echo "🌐 Opening RabbitMQ Management UI..."
	@open http://localhost:15672 || echo "Open http://localhost:15672 manually (puppies/puppies)"

# Create uploads directory
init-dirs:
	@mkdir -p uploads
	@mkdir -p logs