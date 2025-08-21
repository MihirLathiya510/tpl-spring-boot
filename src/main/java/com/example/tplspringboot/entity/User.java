package com.example.tplspringboot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.Set;

/**
 * User entity for authentication and authorization with multi-tenant support.
 * 
 * This entity uses the Pool Model approach where:
 * - All tenants share the same 'users' table
 * - tenant_id column (inherited from BaseEntity) provides isolation
 * - PostgreSQL Row-Level Security automatically filters data by tenant
 * 
 * Features:
 * - Automatic tenant isolation via BaseEntity
 * - Audit trail (created_at, updated_at) via BaseEntity
 * - BCrypt encrypted passwords
 * - Role-based authorization support
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email_tenant", columnList = "email, tenant_id", unique = true),
    @Index(name = "idx_users_tenant", columnList = "tenant_id"),
    @Index(name = "idx_users_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "email", nullable = false, length = 255)
    private String email;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "password", nullable = false, length = 255)
    private String password; // BCrypt encrypted
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_roles", 
        joinColumns = @JoinColumn(name = "user_id"),
        indexes = @Index(name = "idx_user_roles_tenant", columnList = "user_id")
    )
    @Column(name = "role", length = 50)
    @Builder.Default
    private Set<String> roles = Set.of("USER");
    
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * Factory method to create a new user with default settings.
     * The tenant_id will be automatically set from TenantContext in BaseEntity.
     */
    public static User createNew(String name, String email, String password) {
        return User.builder()
                .name(name)
                .email(email)
                .password(password)
                .roles(Set.of("USER"))
                .enabled(true)
                .build();
    }
    
    /**
     * Factory method to create a new user with age.
     */
    public static User createNew(String name, String email, Integer age, String password) {
        return User.builder()
                .name(name)
                .email(email)
                .age(age)
                .password(password)
                .roles(Set.of("USER"))
                .enabled(true)
                .build();
    }
    
    /**
     * Check if user has a specific role.
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
    
    /**
     * Add a role to the user.
     */
    public void addRole(String role) {
        if (roles != null) {
            roles.add(role);
        }
    }
    
    /**
     * Remove a role from the user.
     */
    public void removeRole(String role) {
        if (roles != null) {
            roles.remove(role);
        }
    }
}
