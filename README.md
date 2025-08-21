# tpl-spring-boot

A production-ready Spring Boot 3.4.1 template with JWT authentication and PostgreSQL multi-tenancy.

## Quick Start

### Prerequisites
- Java 21 LTS
- Maven 3.9+
- PostgreSQL 15+
- Docker (for local development)

### Local Development

#### Option 1: Using Makefile (Recommended)
```bash
make setup    # Start PostgreSQL and setup
make dev      # Start development server
make test     # Run tests
make help     # See all available commands
```

#### Option 2: Using Maven Profiles
```bash
# Development
mvn spring-boot:run -Pdev

# Production build
mvn clean package -Pprod

# Run tests
mvn test -Ptest

# Start PostgreSQL
mvn exec:exec@docker-up -Pdocker
```

#### Option 3: Manual Commands
```bash
# Start PostgreSQL
docker-compose up -d

# Run application
mvn spring-boot:run

# Access Swagger UI: http://localhost:8080/docs
```

### API Testing

1. **Register User**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/register \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: tenant1" \
     -d '{"name":"John Doe","email":"john@tenant1.com","password":"password123"}'
   ```

2. **Login**
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: tenant1" \
     -d '{"email":"john@tenant1.com","password":"password123"}'
   ```

3. **Access Protected Endpoint**
   ```bash
   curl -H "Authorization: Bearer <token>" \
     -H "X-Tenant-ID: tenant1" \
     http://localhost:8080/api/v1/users
   ```

## Technology Stack

- **Framework**: Spring Boot 3.4.1
- **Runtime**: Java 21 LTS
- **Security**: Spring Security 6.x + JWT
- **Database**: PostgreSQL 15+ with Row-Level Security
- **Persistence**: Spring Data JPA + Flyway
- **Documentation**: SpringDoc OpenAPI 2.7.0
- **Build**: Maven 3.9+

## Key Features

- **JWT Authentication**: Stateless authentication with access and refresh tokens
- **Multi-Tenancy**: PostgreSQL Row-Level Security for automatic data isolation
- **API Documentation**: Interactive Swagger UI with JWT authorization
- **Error Handling**: Global exception handling with structured responses
- **Validation**: Jakarta Bean Validation with custom error messages
- **Logging**: Structured JSON logging with correlation IDs
- **Health Checks**: Spring Boot Actuator endpoints
- **Security Headers**: CORS, HSTS, frame options protection
- **Comprehensive Testing**: Integration tests with Testcontainers for database isolation

## Documentation

- [Application Structure](docs/STRUCTURE.md) - Package organization and layer responsibilities
- [Multi-Tenancy](docs/MULTITENANCY.md) - PostgreSQL RLS implementation details

## Configuration

Key application properties:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/tpl_spring_boot
spring.jpa.hibernate.ddl-auto=none

# JWT
jwt.secret=myVerySecureSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm
jwt.expiration=86400000

# Multi-Tenancy
tenant.default=default
tenant.header-name=X-Tenant-ID

# Documentation
springdoc.swagger-ui.path=/docs
```

## Testing

The application includes a comprehensive test suite with 29 integration tests covering:

- **Authentication Flow**: User registration, login, JWT validation
- **Multi-Tenancy**: Data isolation between tenants, cross-tenant security
- **Authorization**: Protected endpoints, role-based access control
- **Validation**: Input validation, error handling, edge cases
- **Database Integration**: Testcontainers for isolated PostgreSQL testing

Run tests with:
```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=AuthControllerTest

# With debug logging
mvn test -Dlogging.level.com.example.tplspringboot=DEBUG
```

## Development

### Database Schema

Tables are created via Flyway migrations with automatic tenant isolation:

```sql
-- All tables include tenant_id for Row-Level Security
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    -- other fields...
);

-- RLS policies automatically filter by tenant
CREATE POLICY tenant_isolation_policy ON users
    USING (tenant_id = current_setting('app.current_tenant', true));
```

### Adding New Entities

1. Extend `BaseEntity` for automatic tenant handling
2. Add migration script in `src/main/resources/db/migration/`
3. Create repository interface extending `JpaRepository`
4. All queries automatically filtered by current tenant

### Security

All API endpoints (except `/auth/**`, `/docs/**`, `/actuator/health|info`) require:
- Valid JWT token in `Authorization: Bearer <token>` header
- Tenant context in `X-Tenant-ID` header
- Matching tenant between JWT claims and request header

## Production Deployment

1. **Environment Variables**
   ```bash
   export JWT_SECRET="production-secret-key"
   export DB_URL="jdbc:postgresql://prod-db:5432/app"
   export DB_USERNAME="app_user"
   export DB_PASSWORD="secure_password"
   ```

2. **Profile Activation**
   ```bash
   java -jar app.jar --spring.profiles.active=prod
   ```

3. **Health Monitoring**
   ```
   GET /actuator/health
   GET /actuator/info
   ```