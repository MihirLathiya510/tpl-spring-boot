# Multi-Tenancy Implementation

## Architecture Overview

This application implements the **Pool Model** approach using PostgreSQL Row-Level Security (RLS) for tenant isolation. All tenants share the same database schema but data is automatically isolated at the database level.

## Strategy Comparison

| Approach | Isolation Level | Scalability | Cost | Maintenance |
|----------|----------------|-------------|------|-------------|
| **Pool Model (Our Choice)** | Database-level RLS | Thousands of tenants | Lowest | Simplest |
| Schema-per-Tenant | Schema-level | Hundreds of tenants | Medium | Complex |
| Database-per-Tenant | Complete isolation | Tens of tenants | Highest | Very Complex |

## Implementation Components

### Tenant Context Management

#### TenantContext
Thread-local storage for tenant information throughout request lifecycle.

```java
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
```

#### TenantResolver
Extracts tenant identifier from HTTP requests, primarily from X-Tenant-ID header.

```java
@Component
public class TenantResolver {
    @Value("${tenant.header-name:X-Tenant-ID}")
    private String tenantHeaderName;
    
    @Value("${tenant.default:default}")
    private String defaultTenant;
    
    public String resolveTenant(HttpServletRequest request) {
        String tenantId = request.getHeader(tenantHeaderName);
        return StringUtils.hasText(tenantId) ? tenantId : defaultTenant;
    }
}
```

### Request Processing Flow

#### TenantFilter
Runs first in filter chain to establish tenant context for entire request.

```java
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {
    
    protected void doFilterInternal(HttpServletRequest request, 
            HttpServletResponse response, FilterChain filterChain) {
        try {
            // 1. Resolve tenant from request
            String tenantId = tenantResolver.resolveTenant(request);
            
            // 2. Set application-level tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            // 3. Set PostgreSQL session variable for RLS
            jdbcTemplate.execute("SELECT set_current_tenant('" + tenantId + "')");
            
            // 4. Add tenant to response headers
            response.setHeader("X-Tenant-ID", tenantId);
            
            // 5. Continue processing
            filterChain.doFilter(request, response);
            
        } finally {
            // 6. Clean up tenant context
            TenantContext.clear();
        }
    }
}
```

## Database Design

### Schema Structure

All tables include a `tenant_id` column for data segregation:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_users_email_tenant UNIQUE (email, tenant_id)
);
```

### Row-Level Security Implementation

#### RLS Policies
Automatic filtering based on PostgreSQL session variables:

```sql
-- Enable RLS on table (forced for superusers)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- Create tenant isolation policy
CREATE POLICY tenant_isolation_policy ON users
    FOR ALL TO PUBLIC
    USING (tenant_id = current_setting('app.current_tenant', true));
```

#### Session Functions
Helper functions for tenant context management:

```sql
-- Set current tenant in PostgreSQL session
CREATE OR REPLACE FUNCTION set_current_tenant(tenant_id TEXT)
RETURNS void AS $$
BEGIN
    PERFORM set_config('app.current_tenant', tenant_id, true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Get current tenant from PostgreSQL session
CREATE OR REPLACE FUNCTION get_current_tenant()
RETURNS TEXT AS $$
BEGIN
    RETURN current_setting('app.current_tenant', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### Index Optimization

Strategic indexing for tenant-aware queries:

```sql
-- Primary tenant filtering
CREATE INDEX idx_users_tenant_id ON users(tenant_id);

-- Composite index for unique constraints
CREATE INDEX idx_users_email_tenant ON users(email, tenant_id);

-- Performance optimization for common queries
CREATE INDEX idx_users_created_at ON users(created_at);
```

## Entity Design

### BaseEntity
Abstract base class providing tenant context and audit fields:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    @PreUpdate
    private void setTenantId() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getCurrentTenant();
        }
    }
}
```

### User Entity
Extends BaseEntity for automatic tenant handling:

```java
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email_tenant", columnList = "email, tenant_id", unique = true),
    @Index(name = "idx_users_tenant", columnList = "tenant_id")
})
public class User extends BaseEntity {
    // Entity fields - tenant_id inherited from BaseEntity
    // PostgreSQL RLS automatically filters by tenant
}
```

## Repository Layer

### Automatic Tenant Filtering
Spring Data JPA repositories work transparently with RLS:

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // All methods automatically filtered by PostgreSQL RLS
    Optional<User> findByEmail(String email);
    Page<User> findByEnabled(boolean enabled, Pageable pageable);
    boolean existsByEmail(String email);
    
    // No need for explicit tenant_id in queries
    // RLS policies handle filtering automatically
}
```

## JWT Integration

### Tenant-Aware Tokens
JWT tokens include tenant context for validation:

```json
{
  "sub": "user@tenant1.com",
  "tenant": "tenant1",
  "roles": "ROLE_USER",
  "type": "access",
  "iat": 1735688840,
  "exp": 1735775240
}
```

### Token Validation
JWT filter validates tenant consistency:

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private void authenticateUser(HttpServletRequest request, String jwt) {
        String tokenTenant = jwtUtil.getTenantFromToken(jwt);
        String currentTenant = TenantContext.getCurrentTenant();
        
        // Validate tenant consistency
        if (!currentTenant.equals(tokenTenant)) {
            log.warn("Tenant mismatch: JWT '{}' vs current '{}'", 
                    tokenTenant, currentTenant);
            return; // Reject authentication
        }
        
        // Proceed with authentication
    }
}
```

## Security Guarantees

### Database-Level Isolation
- PostgreSQL RLS enforces tenant separation at the database level
- No application bugs can leak data between tenants
- Policies apply to all database operations (SELECT, INSERT, UPDATE, DELETE)
- Forced RLS prevents superuser bypass

### Application-Level Validation
- JWT tokens contain tenant claims for additional validation
- Filter chain ensures tenant context is set before authentication
- Response headers include tenant information for debugging
- Request correlation with tenant context in logs

### Performance Characteristics
- Single database connection pool shared across tenants
- Optimized query plans with proper indexing
- Minimal overhead from RLS policy evaluation
- Automatic query optimization by PostgreSQL

## Tenant Lifecycle

### Request Processing
1. HTTP request arrives with X-Tenant-ID header
2. TenantFilter extracts tenant identifier
3. TenantContext stores tenant in ThreadLocal
4. PostgreSQL session variable set for RLS
5. All subsequent database queries filtered by tenant
6. Response includes tenant confirmation headers
7. Tenant context cleared after request completion

### Data Access Pattern
1. Repository method called (no tenant parameter needed)
2. Spring Data JPA generates standard SQL
3. PostgreSQL applies RLS policy automatically
4. Results filtered to current tenant only
5. Entity instances populated with tenant context
6. Business logic receives tenant-isolated data

## Configuration

### Application Properties
```properties
# Multi-Tenancy Configuration
tenant.default=default
tenant.header-name=X-Tenant-ID

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/tpl_spring_boot
spring.jpa.hibernate.ddl-auto=none
```

### Security Configuration
```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

## Migration and Deployment

### Database Migrations
Flyway scripts handle RLS setup and table creation:

```
src/main/resources/db/migration/
└── V1__Create_users_table_with_RLS.sql
    ├── CREATE TABLE statements
    ├── ALTER TABLE ENABLE ROW LEVEL SECURITY
    ├── CREATE POLICY statements
    ├── CREATE FUNCTION statements
    └── GRANT permissions
```

### Production Considerations
- Monitor RLS policy performance with query analysis
- Index tenant_id columns for optimal filtering
- Consider partitioning for very large tenant datasets
- Implement tenant onboarding automation
- Set up monitoring for cross-tenant data access attempts
