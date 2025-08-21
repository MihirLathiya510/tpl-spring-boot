# Application Structure

## Package Organization

```
src/main/java/com/example/tplspringboot/
├── config/           # Configuration classes
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── controller/       # REST endpoints
│   ├── AuthController.java
│   └── UserController.java
├── dto/             # Data Transfer Objects
│   ├── AuthResponse.java
│   ├── ErrorResponse.java
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   └── UserDto.java
├── entity/          # JPA entities
│   ├── BaseEntity.java
│   └── User.java
├── exception/       # Custom exceptions
│   ├── AuthenticationException.java
│   ├── BusinessException.java
│   ├── ConflictException.java
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── ValidationException.java
├── repository/      # Data access layer
│   └── UserRepository.java
├── security/        # Security components
│   ├── JwtAuthenticationFilter.java
│   └── JwtUtil.java
├── service/         # Business logic
│   └── UserService.java
├── tenant/          # Multi-tenancy support
│   ├── TenantContext.java
│   ├── TenantFilter.java
│   └── TenantResolver.java
└── TplSpringBootApplication.java
```

## Layer Responsibilities

### Controller Layer
- HTTP request/response handling
- Input validation via Jakarta Bean Validation
- OpenAPI documentation annotations
- Delegates to service layer

### Service Layer
- Business logic implementation
- Transaction management (@Transactional)
- Coordinates between controllers and repositories
- Exception handling and conversion

### Repository Layer
- Data access abstraction
- Spring Data JPA implementations
- Automatic tenant filtering via PostgreSQL RLS
- Query optimization and pagination

### Entity Layer
- JPA entity definitions
- Database mapping annotations
- Audit fields via BaseEntity
- Automatic tenant context injection

## Configuration Classes

### SecurityConfig
- JWT authentication setup
- Filter chain configuration
- CORS policy definition
- Authorization rules

### OpenApiConfig
- Swagger UI customization
- API documentation metadata
- Security scheme definitions
- Server environment configuration

## Filter Chain Architecture

```
HTTP Request
    ↓
TenantFilter (Order: 1)
├── Extract X-Tenant-ID header
├── Set TenantContext
├── Configure PostgreSQL session
└── Add response headers
    ↓
JwtAuthenticationFilter
├── Extract JWT token
├── Validate token and tenant
├── Set SecurityContext
└── Handle authentication errors
    ↓
Spring Security Filters
├── Authorization checks
├── Method security
└── Access control
    ↓
Controller Methods
```

## Error Handling Strategy

### Global Exception Handler
- Centralized error processing
- Consistent error response format
- HTTP status code mapping
- Request correlation tracking

### Custom Exceptions
- BusinessException: Base for business logic errors
- AuthenticationException: Authentication failures (401 Unauthorized)
- ValidationException: Input validation failures
- ResourceNotFoundException: Entity not found
- ConflictException: Resource conflict situations

### Error Response Structure
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": "Additional context",
  "traceId": "correlation-id",
  "timestamp": "2025-01-20T18:07:03.113Z",
  "path": "/api/v1/users",
  "method": "POST",
  "fieldErrors": []
}
```

## Data Flow Architecture

### Request Processing
1. HTTP request arrives with headers
2. TenantFilter extracts tenant context
3. JwtAuthenticationFilter validates authentication
4. Controller receives validated request
5. Service processes business logic
6. Repository accesses filtered data
7. Response travels back through chain

### Database Interaction
1. Service calls repository method
2. Spring Data JPA generates query
3. PostgreSQL RLS applies tenant filter
4. Results automatically filtered by tenant
5. Entities populated with audit fields
6. Service receives tenant-isolated data

## Technology Stack Integration

### Spring Boot 3.4.1
- Auto-configuration for rapid development
- Production-ready features via Actuator
- Embedded server configuration
- Profile-based environment management

### Spring Security 6.x
- JWT-based stateless authentication
- Method-level security annotations
- Filter chain customization
- CORS and security headers

### Spring Data JPA
- Repository pattern implementation
- Automatic query generation
- Transaction management
- Audit field population

### PostgreSQL Integration
- Row-Level Security for tenant isolation
- Flyway for schema migrations
- HikariCP connection pooling
- Optimized indexing strategy

## Build and Deployment

### Maven Configuration
- Dependency management
- Plugin configuration
- Profile-specific builds
- Packaging and distribution

### Database Migrations
- Version-controlled schema changes
- Flyway migration scripts
- Baseline and validation
- Production deployment safety

### Configuration Management
- Environment-specific properties
- Externalized configuration
- Secret management ready
- Profile activation
