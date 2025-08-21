# Spring Boot Project Makefile
# Professional script management for Java projects

.PHONY: help dev prod test clean build docker-up docker-down logs format check

# Default target
help:
	@echo "Available commands:"
	@echo "  make dev          - Start development server"
	@echo "  make prod         - Build and run production"
	@echo "  make test         - Run all tests"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make build        - Build application"
	@echo "  make docker-up    - Start PostgreSQL with Docker"
	@echo "  make docker-down  - Stop Docker containers"
	@echo "  make logs         - Show application logs"
	@echo "  make format       - Format code (when spotless added)"
	@echo "  make check        - Run code quality checks"

# Development
dev:
	@echo "Starting development server..."
	mvn spring-boot:run -Pdev

# Production
prod:
	@echo "Building for production..."
	mvn clean package -Pprod
	@echo "Starting production server..."
	java -jar target/tpl-spring-boot-*.jar --spring.profiles.active=prod

# Testing
test:
	@echo "Running tests..."
	mvn test -Ptest

# Clean
clean:
	@echo "Cleaning build artifacts..."
	mvn clean

# Build
build:
	@echo "Building application..."
	mvn package

# Docker operations
docker-up:
	@echo "Starting PostgreSQL..."
	docker-compose up -d

docker-down:
	@echo "Stopping Docker containers..."
	docker-compose down

# Logs
logs:
	@echo "Showing application logs..."
	docker-compose logs -f

# Code quality
format:
	@echo "Formatting code..."
	mvn spotless:apply

check:
	@echo "Running code quality checks..."
	mvn verify

# Quick development setup
setup: docker-up
	@echo "Waiting for PostgreSQL to be ready..."
	sleep 5
	@echo "Setup complete! Run 'make dev' to start development."

# Full CI pipeline
ci: clean test build
	@echo "CI pipeline completed successfully!"
