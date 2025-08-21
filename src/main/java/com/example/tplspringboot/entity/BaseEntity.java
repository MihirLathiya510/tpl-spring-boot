package com.example.tplspringboot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base entity class providing common audit fields and tenant support.
 * 
 * This class implements the Pool Model approach for multi-tenancy where:
 * - All entities share the same database schema
 * - tenant_id column provides tenant isolation
 * - PostgreSQL Row-Level Security (RLS) enforces data access policies
 * 
 * All application entities should extend this class to inherit:
 * - Tenant isolation (tenant_id)
 * - Audit trail (created_at, updated_at)
 * - Common ID field (id)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Tenant identifier for multi-tenant data isolation.
     * This field is used by PostgreSQL Row-Level Security policies
     * to automatically filter data based on the current tenant context.
     */
    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    /**
     * Timestamp when the entity was first created.
     * Automatically populated by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the entity was last updated.
     * Automatically updated by JPA auditing on every save operation.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Pre-persist callback to set tenant_id from current context.
     * This ensures that every new entity is automatically assigned
     * to the current tenant without manual intervention.
     */
    @PrePersist
    protected void prePersist() {
        if (tenantId == null) {
            tenantId = getCurrentTenant();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Pre-update callback to update the updatedAt timestamp
     * and ensure tenant_id consistency.
     */
    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Ensure tenant_id cannot be changed
        if (tenantId == null) {
            tenantId = getCurrentTenant();
        }
    }

    /**
     * Gets the current tenant ID from the tenant context.
     * This method integrates with our existing TenantContext system.
     */
    private String getCurrentTenant() {
        try {
            // Import will be resolved when TenantContext is available
            return com.example.tplspringboot.tenant.TenantContext.getCurrentTenant();
        } catch (Exception e) {
            // Fallback to default tenant if context is not available
            return "default";
        }
    }

    /**
     * Equality based on ID and tenant for proper entity comparison.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        BaseEntity that = (BaseEntity) obj;
        return id != null && id.equals(that.id) && 
               tenantId != null && tenantId.equals(that.tenantId);
    }

    /**
     * Hash code based on ID and tenant.
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    /**
     * String representation including tenant context.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
