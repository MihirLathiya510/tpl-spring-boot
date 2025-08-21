package com.example.tplspringboot.tenant;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves tenant information from HTTP requests.
 * 
 * This component implements multiple strategies for tenant identification:
 * 1. X-Tenant-ID header (primary method)
 * 2. Subdomain extraction from Host header
 * 3. Path parameter extraction
 * 4. JWT token claims (when available)
 * 
 * The resolver follows a priority order and falls back to default tenant
 * if no tenant information is found.
 */
@Component
@Slf4j
public class TenantResolver {
    
    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String HOST_HEADER = "Host";
    
    /**
     * Resolves tenant from the HTTP request using multiple strategies.
     * 
     * @param request the HTTP request
     * @return the resolved tenant identifier
     */
    public String resolveTenant(HttpServletRequest request) {
        // Strategy 1: Check X-Tenant-ID header (most explicit)
        String tenant = resolveFromHeader(request);
        if (StringUtils.hasText(tenant)) {
            log.debug("Resolved tenant '{}' from header", tenant);
            return tenant;
        }
        
        // Strategy 2: Extract from subdomain
        tenant = resolveFromSubdomain(request);
        if (StringUtils.hasText(tenant)) {
            log.debug("Resolved tenant '{}' from subdomain", tenant);
            return tenant;
        }
        
        // Strategy 3: Extract from path parameter
        tenant = resolveFromPath(request);
        if (StringUtils.hasText(tenant)) {
            log.debug("Resolved tenant '{}' from path", tenant);
            return tenant;
        }
        
        // Fallback to default tenant
        log.debug("No tenant information found in request, using default tenant");
        return TenantContext.getDefaultTenant();
    }
    
    /**
     * Resolves tenant from X-Tenant-ID header.
     */
    private String resolveFromHeader(HttpServletRequest request) {
        String tenantId = request.getHeader(TENANT_HEADER);
        return sanitizeTenantId(tenantId);
    }
    
    /**
     * Resolves tenant from subdomain in Host header.
     * Example: tenant1.api.example.com -> tenant1
     */
    private String resolveFromSubdomain(HttpServletRequest request) {
        String host = request.getHeader(HOST_HEADER);
        if (!StringUtils.hasText(host)) {
            return null;
        }
        
        // Remove port if present
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }
        
        // Extract subdomain (first part before first dot)
        String[] parts = host.split("\\.");
        if (parts.length > 2) {
            String subdomain = parts[0];
            // Exclude common subdomains
            if (!isCommonSubdomain(subdomain)) {
                return sanitizeTenantId(subdomain);
            }
        }
        
        return null;
    }
    
    /**
     * Resolves tenant from path parameter.
     * Example: /api/v1/tenant/tenant1/users -> tenant1
     */
    private String resolveFromPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!StringUtils.hasText(path)) {
            return null;
        }
        
        // Look for /tenant/{tenantId}/ pattern
        String tenantPattern = "/tenant/";
        int tenantIndex = path.indexOf(tenantPattern);
        if (tenantIndex >= 0) {
            int startIndex = tenantIndex + tenantPattern.length();
            int endIndex = path.indexOf("/", startIndex);
            if (endIndex == -1) {
                endIndex = path.length();
            }
            
            if (startIndex < endIndex) {
                String tenantId = path.substring(startIndex, endIndex);
                return sanitizeTenantId(tenantId);
            }
        }
        
        return null;
    }
    
    /**
     * Sanitizes and validates tenant identifier.
     */
    private String sanitizeTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        
        // Clean and validate tenant ID
        String sanitized = tenantId.trim().toLowerCase();
        
        // Basic validation: alphanumeric, hyphens, underscores only
        if (!sanitized.matches("^[a-z0-9_-]+$")) {
            log.warn("Invalid tenant ID format: '{}', using default tenant", tenantId);
            return null;
        }
        
        // Length validation
        if (sanitized.length() > 50) {
            log.warn("Tenant ID too long: '{}', using default tenant", tenantId);
            return null;
        }
        
        return sanitized;
    }
    
    /**
     * Checks if subdomain is a common one that shouldn't be treated as tenant.
     */
    private boolean isCommonSubdomain(String subdomain) {
        return subdomain.matches("^(www|api|admin|app|mail|ftp|blog|shop|dev|staging|prod|test)$");
    }
}
