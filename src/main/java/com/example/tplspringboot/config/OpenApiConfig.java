package com.example.tplspringboot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for API documentation.
 * Provides comprehensive API documentation via Swagger UI.
 * 
 * Access Swagger UI at: http://localhost:8080/docs
 * Access OpenAPI JSON at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.name:tpl-spring-boot}")
    private String appName;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from /api/v1/auth/login endpoint")))
                .info(new Info()
                        .title("Spring Boot API Template")
                        .version(appVersion)
                        .description("""
                                A comprehensive, production-ready Spring Boot template designed for enterprise applications. 
                                This template provides a robust foundation with industry best practices, comprehensive error handling, 
                                and enterprise-grade features.
                                
                                ## Authentication
                                
                                This API uses **JWT (JSON Web Token)** authentication. To access protected endpoints:
                                
                                1. **Register** a new user via `POST /api/v1/auth/register` 
                                2. **Login** with your credentials via `POST /api/v1/auth/login`
                                3. **Copy the accessToken** from the login response
                                4. **Click the "Authorize" button** above and paste the token (without "Bearer " prefix)
                                5. **Test protected endpoints** - they will automatically include the JWT token
                                
                                ### Multi-Tenancy
                                
                                All requests must include the `X-Tenant-ID` header to specify the tenant context.
                                Users and data are completely isolated between tenants.
                                
                                ## Key Features
                                
                                ### API Standards
                                - **OpenAPI 3.0 Specification** - Complete API documentation with interactive testing
                                - **RESTful Design** - Following REST architectural principles and HTTP standards
                                - **Consistent Response Format** - Standardized JSON responses across all endpoints
                                - **Comprehensive Error Handling** - Detailed error responses with trace IDs
                                
                                ### Validation & Security
                                - **JWT Authentication** - Stateless authentication with access and refresh tokens
                                - **Input Validation** - Jakarta Bean Validation with custom error messages
                                - **Multi-tenancy Support** - Complete data isolation between tenants
                                - **CORS Configuration** - Cross-origin resource sharing support
                                - **Security Headers** - HSTS, frame options, and content type protection
                                
                                ### Observability
                                - **Structured Logging** - JSON-formatted logs with correlation IDs
                                - **Health Checks** - Spring Boot Actuator endpoints for monitoring
                                - **Metrics Collection** - Application performance metrics
                                - **Request Tracing** - End-to-end request tracking with tenant context
                                
                                ### Development Experience
                                - **Database Migrations** - Flyway integration for schema management
                                - **Test Containers** - Integration testing with real databases
                                - **Hot Reload** - Development-friendly configuration
                                - **Code Quality** - Lombok integration for cleaner code
                                
                                ## Technology Stack
                                
                                - **Framework**: Spring Boot 3.4.1 (Latest LTS)
                                - **Runtime**: Java 21 LTS
                                - **Security**: Spring Security 6.x with JWT
                                - **Database**: PostgreSQL 15+ (Phase 4)
                                - **Build Tool**: Apache Maven 3.9+
                                - **Documentation**: SpringDoc OpenAPI 2.7.0
                                - **Testing**: JUnit 5, Testcontainers, MockMvc
                                
                                ## Getting Started
                                
                                1. Start by registering a new user in the **Authentication** section
                                2. Login to get your JWT token
                                3. Use the **Authorize** button to set your token
                                4. Test any protected endpoint with automatic authentication
                                
                                All endpoints follow REST conventions and return consistent JSON responses.
                                """)
                        .contact(new Contact()
                                .name("Enterprise Development Team")
                                .email("api-support@enterprise.com")
                                .url("https://docs.enterprise.com/api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Environment"),
                        new Server()
                                .url("https://api-staging.enterprise.com")
                                .description("Staging Environment"),
                        new Server()
                                .url("https://api.enterprise.com")
                                .description("Production Environment")
                ));
    }
}
