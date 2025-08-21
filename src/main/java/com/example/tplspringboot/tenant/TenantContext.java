package com.example.tplspringboot.tenant;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local context holder for tenant information.
 * Provides tenant isolation across the application by storing
 * the current tenant identifier in a thread-local variable.
 * 
 * This ensures that each HTTP request processes within the
 * correct tenant context throughout the entire request lifecycle.
 */
@Slf4j
public class TenantContext {
    
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "default";
    
    /**
     * Sets the current tenant for the current thread.
     * 
     * @param tenantId the tenant identifier
     */
    public static void setCurrentTenant(String tenantId) {
        if (tenantId == null || tenantId.trim().isEmpty()) {
            log.warn("Attempting to set null or empty tenant ID, using default tenant");
            CURRENT_TENANT.set(DEFAULT_TENANT);
        } else {
            log.debug("Setting current tenant to: {}", tenantId);
            CURRENT_TENANT.set(tenantId.trim().toLowerCase());
        }
    }
    
    /**
     * Gets the current tenant for the current thread.
     * 
     * @return the current tenant identifier, or default tenant if none set
     */
    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        if (tenant == null) {
            log.debug("No tenant set for current thread, using default tenant");
            return DEFAULT_TENANT;
        }
        return tenant;
    }
    
    /**
     * Clears the current tenant from the current thread.
     * Should be called at the end of request processing to prevent memory leaks.
     */
    public static void clear() {
        String tenant = CURRENT_TENANT.get();
        if (tenant != null) {
            log.debug("Clearing tenant context for: {}", tenant);
            CURRENT_TENANT.remove();
        }
    }
    
    /**
     * Gets the default tenant identifier.
     * 
     * @return the default tenant identifier
     */
    public static String getDefaultTenant() {
        return DEFAULT_TENANT;
    }
    
    /**
     * Checks if a tenant is currently set.
     * 
     * @return true if a tenant is set, false otherwise
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }
}
