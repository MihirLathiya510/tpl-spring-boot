package com.example.tplspringboot.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Filter that resolves and sets tenant context for each HTTP request.
 * 
 * This filter runs early in the filter chain to ensure that tenant context
 * is available for all subsequent processing. It uses the TenantResolver
 * to determine the tenant from the request and sets it in the TenantContext.
 * 
 * The filter also ensures proper cleanup of tenant context after request
 * processing to prevent memory leaks and cross-request contamination.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run early in the filter chain
public class TenantFilter extends OncePerRequestFilter {
    
    private final TenantResolver tenantResolver;
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        String requestId = generateRequestId();
        String requestUri = request.getRequestURI();
        
        try {
            // Resolve tenant from request
            String tenantId = tenantResolver.resolveTenant(request);
            
            // Set tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            // Set PostgreSQL tenant context for RLS
            // We'll use a SQL comment approach to ensure RLS context is set per query
            try {
                jdbcTemplate.execute("SELECT set_current_tenant('" + tenantId + "')");
                log.debug("Set PostgreSQL tenant context to: {}", tenantId);
            } catch (Exception e) {
                log.warn("Failed to set PostgreSQL tenant context for '{}': {}", tenantId, e.getMessage());
                // Continue processing - RLS will use default behavior
            }
            
            // Add tenant info to response headers for debugging
            response.setHeader("X-Tenant-ID", tenantId);
            response.setHeader("X-Request-ID", requestId);
            
            log.debug("Processing request [{}] for tenant '{}': {}", 
                     requestId, tenantId, requestUri);
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error processing request [{}] for URI '{}': {}", 
                     requestId, requestUri, e.getMessage(), e);
            
            // Set error headers
            response.setHeader("X-Error", "Tenant resolution failed");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            throw e;
        } finally {
            // Always clear tenant context to prevent memory leaks
            String finalTenant = TenantContext.getCurrentTenant();
            TenantContext.clear();
            
            // Clear PostgreSQL tenant context
            try {
                jdbcTemplate.execute("SELECT set_current_tenant(NULL)");
                log.debug("Cleared PostgreSQL tenant context");
            } catch (Exception e) {
                log.warn("Failed to clear PostgreSQL tenant context: {}", e.getMessage());
            }
            
            log.debug("Completed request [{}] for tenant '{}': {}", 
                     requestId, finalTenant, requestUri);
        }
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip tenant resolution for certain paths
        return path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/docs") ||
               path.startsWith("/favicon.ico") ||
               path.startsWith("/error");
    }
    
    /**
     * Generates a unique request ID for tracking.
     */
    private String generateRequestId() {
        return Long.toHexString(System.nanoTime());
    }
}
